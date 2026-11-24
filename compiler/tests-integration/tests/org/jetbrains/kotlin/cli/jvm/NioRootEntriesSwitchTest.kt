/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import com.intellij.mock.MockProject
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinderFactory
import org.jetbrains.kotlin.cli.jvm.index.JvmClasspathIndexes
import org.jetbrains.kotlin.cli.jvm.index.VfsClassFileHandle
import org.jetbrains.kotlin.cli.jvm.index.nio.JarClassFileHandle
import org.jetbrains.kotlin.cli.jvm.index.nio.PathClassFileHandle
import org.jetbrains.kotlin.cli.jvm.index.virtualFile
import org.jetbrains.kotlin.config.useJavaDirect
import org.jetbrains.kotlin.config.useNioJvmDependenciesIndex
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.VirtualFileBasedApi
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import javax.tools.ToolProvider
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeText

/** The `-Xuse-nio-jvm-dependencies-index` switch: which compilations get NIO root entries, and a compilation with them. */
class NioRootEntriesSwitchTest : JvmDependenciesIndexContractTestBase() {
    private fun configuration(nio: Boolean, javaDirect: Boolean = true) =
        KotlinTestUtils.newConfiguration().apply {
            useNioJvmDependenciesIndex = nio
            useJavaDirect = javaDirect
        }

    @Test
    fun nioEntriesAreOffByDefault() {
        val indexes = JvmClasspathIndexes(KotlinTestUtils.newConfiguration(), jarFileSystem = null)
        assertSame(indexes.javaFileManagerIndex, indexes.classFinderIndex)
    }

    @Test
    fun nioEntriesAreNotSelectedWithoutJavaDirect() {
        val indexes = JvmClasspathIndexes(configuration(nio = true, javaDirect = false), jarFileSystem = null)
        assertSame(indexes.javaFileManagerIndex, indexes.classFinderIndex)
    }

    @Test
    fun switchGivesTheClassFinderNioEntriesAndKeepsVfsEntriesForTheJavaFileManager() {
        val out = dir("out", "p/A.class")
        val indexes = JvmClasspathIndexes(configuration(nio = true), jarFileSystem = null)
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
    fun jarRootsGetNioEntriesOnlyIfJarsAreReadWithFastJar() {
        val lib = jar("lib.jar", "q/B.class")
        val fastJarFileSystem = newFastJarFileSystem()
        try {
            val withFastJar = JvmClasspathIndexes(configuration(nio = true), fastJarFileSystem)
            withFastJar.addRoots(listOf(lib.jarRoot()))
            assertInstanceOf(JarClassFileHandle::class.java, withFastJar.classFinderIndex.findClassFiles(classId("q/B"), CLASS).single())
        } finally {
            fastJarFileSystem.clearHandlersCache()
        }

        // E.g. with `-Xuse-fast-jar-file-system=false`: only jars lose their NIO entries.
        val out = dir("out", "p/A.class")
        val withCoreJarFileSystem = JvmClasspathIndexes(configuration(nio = true), CoreJarFileSystem())
        withCoreJarFileSystem.addRoots(listOf(lib.jarRoot(), out.dirRoot()))
        assertInstanceOf(
            VfsClassFileHandle::class.java, withCoreJarFileSystem.classFinderIndex.findClassFiles(classId("q/B"), CLASS).single()
        )
        assertInstanceOf(
            PathClassFileHandle::class.java, withCoreJarFileSystem.classFinderIndex.findClassFiles(classId("p/A"), CLASS).single()
        )
    }

    @OptIn(VirtualFileBasedApi::class)
    @Test
    fun virtualFileLookupsReadVfsEntriesWithTheSwitchOn() {
        val out = dir("out", "p/A.class")
        val indexes = JvmClasspathIndexes(configuration(nio = true), jarFileSystem = null)
        indexes.addRoots(listOf(out.dirRoot()))
        val factory = CliVirtualFileFinderFactory(
            indexes.classFinderIndex, enableSearchInCtSym = false, perfManager = null, vfsIndex = indexes.javaFileManagerIndex,
        )
        val classFile = out.resolve("p/A.class").asLocalVirtualFile()

        val disposable = Disposer.newDisposable()
        try {
            val project = MockProject(null, disposable)
            val projectFinder = factory.create(project)
            assertEquals(classFile, projectFinder.findVirtualFileWithHeader(classId("p/A")))
            assertEquals(classFile, projectFinder.findSourceOrBinaryVirtualFile(classId("p/A")))
            assertArrayEquals(out.resolve("p/A.class").readBytes(), projectFinder.findBinaryClassBytes(classId("p/A")))

            val scopeFinder = factory.create(GlobalSearchScope.filesScope(project, listOf(classFile)))
            assertEquals(classFile, scopeFinder.findVirtualFileWithHeader(classId("p/A")))
        } finally {
            Disposer.dispose(disposable)
        }

        val nioHandle = indexes.classFinderIndex.findClassFiles(classId("p/A"), CLASS).single()
        val error = assertThrows(IllegalStateException::class.java) { nioHandle.virtualFile }
        assertTrue("no VirtualFile" in error.message.orEmpty(), error.message)
    }

    @Test
    fun compilesAgainstDirectoryClasspathWithNioEntries() {
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
            listOf(useSource.toString(), "-cp", lib.toString(), "-d", out.toString(), "-Xuse-nio-jvm-dependencies-index")
        )
        assertTrue(out.resolve("UseKt.class").exists())
    }
}
