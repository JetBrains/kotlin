/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import org.jetbrains.kotlin.cli.jvm.index.JvmClasspathIndexes
import org.jetbrains.kotlin.cli.jvm.index.VfsClassFileHandle
import org.jetbrains.kotlin.cli.jvm.index.nio.PathClassFileHandle
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.useJavaDirect
import org.jetbrains.kotlin.config.useNioJvmDependenciesIndex
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import javax.tools.ToolProvider
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * The hidden `-Xuse-nio-jvm-dependencies-index` switch: which compilations get NIO root entries (java-direct with FastJar, by default),
 * compilations with them, and `=false` as the escape hatch to VFS entries.
 */
class NioRootEntriesSwitchTest : JvmDependenciesIndexContractTestBase() {
    private fun configuration(nio: Boolean, javaDirect: Boolean = true, fastJar: Boolean? = null) =
        KotlinTestUtils.newConfiguration().apply {
            useNioJvmDependenciesIndex = nio
            useJavaDirect = javaDirect
            if (fastJar != null) put(JVMConfigurationKeys.USE_FAST_JAR_FILE_SYSTEM, fastJar)
        }

    @Test
    fun nioEntriesAreOnByDefault() {
        val indexes = JvmClasspathIndexes(KotlinTestUtils.newConfiguration())
        assertNotSame(indexes.javaFileManagerIndex, indexes.classFinderIndex)
    }

    @Test
    fun switchTurnsNioEntriesOff() {
        JvmClasspathIndexes(configuration(nio = false)).let {
            assertSame(it.javaFileManagerIndex, it.classFinderIndex)
        }
    }

    @Test
    fun nioEntriesAreNotSelectedWithoutJavaDirectOrFastJar() {
        JvmClasspathIndexes(configuration(nio = true, javaDirect = false)).let {
            assertSame(it.javaFileManagerIndex, it.classFinderIndex)
        }
        JvmClasspathIndexes(configuration(nio = true, fastJar = false)).let {
            assertSame(it.javaFileManagerIndex, it.classFinderIndex)
        }
    }

    @Test
    fun switchGivesTheClassFinderNioEntriesAndKeepsVfsEntriesForTheJavaFileManager() {
        val out = dir("out", "p/A.class")
        val indexes = JvmClasspathIndexes(configuration(nio = true, fastJar = true))
        assertNotSame(indexes.javaFileManagerIndex, indexes.classFinderIndex)

        val vfsIndex = indexes.addRoots(listOf(out.dirRoot()))
        assertInstanceOf(VfsClassFileHandle::class.java, vfsIndex.findClassFiles(classId("p/A"), CLASS).single())
        assertInstanceOf(
            VfsClassFileHandle::class.java, indexes.javaFileManagerIndex.findClassFiles(classId("p/A"), CLASS).single()
        )
        assertInstanceOf(PathClassFileHandle::class.java, indexes.classFinderIndex.findClassFiles(classId("p/A"), CLASS).single())
        assertEquals(indexes.javaFileManagerIndex.indexedRoots.toList(), indexes.classFinderIndex.indexedRoots.toList())
    }

    @Test
    fun compilesAgainstDirectoryClasspathWithNioEntries() = compileAgainstDirectoryClasspath()

    @Test
    fun compilesAgainstDirectoryClasspathWithVfsEntries() = compileAgainstDirectoryClasspath("-Xuse-nio-jvm-dependencies-index=false")

    private fun compileAgainstDirectoryClasspath(vararg switch: String) {
        val lib = tempDir.resolve("lib").createDirectories()
        val javaSource = tempDir.resolve("javaSrc/p/J.java").apply { parent.createDirectories() }
        javaSource.writeText("package p; public class J { public static int answer() { return 42; } }")
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", lib.toString(), javaSource.toString()))

        val libSource = tempDir.resolve("L.kt")
        libSource.writeText("package p\n\nclass L { fun f() = J.answer() }\n\ninline fun twice(x: Int) = x * 2\n")
        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2JVMCompiler(), listOf(libSource.toString(), "-cp", lib.toString(), "-d", lib.toString())
        )

        val useSource = tempDir.resolve("use.kt")
        useSource.writeText("import p.*\n\nfun use(): Int = twice(L().f()) + J.answer()\n")
        val out: Path = tempDir.resolve("out")
        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2JVMCompiler(),
            listOf(useSource.toString(), "-cp", lib.toString(), "-d", out.toString(), *switch)
        )
        assertTrue(out.resolve("UseKt.class").exists())
    }
}
