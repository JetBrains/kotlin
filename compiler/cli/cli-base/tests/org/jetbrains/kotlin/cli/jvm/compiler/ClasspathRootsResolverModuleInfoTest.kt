/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.common.config.ContentRoot
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRoot
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntryFactory
import org.jetbrains.kotlin.cli.jvm.index.nio.NioRootEntryFactory
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.putJavaModuleInfoSourceReader
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassIdToJavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

/**
 * `cli-base` cannot parse `module-info.java` itself, so [ClasspathRootsResolver] reads it through the
 * [org.jetbrains.kotlin.cli.jvm.modules.javaModuleInfoSourceReader] of the configuration.
 *
 * The JDK is left out (`jdkHome = null`, `-Xjdk-release 8`), so that only the Java source root is resolved; the module finder reports
 * that as a `JAVA_MODULE_RESOLUTION_ERROR`, which the tests ignore.
 */
class ClasspathRootsResolverModuleInfoTest {

    @Test
    fun testMissingReaderIsAnError(@TempDir tempDir: Path) {
        val sourceRoot = tempDir.writeModuleInfo()
        val configuration = CompilerConfiguration.create()

        val result = configuration.resolver(findClassOnClasspath = { null }).convertClasspathRoots(sourceRoot.asSourceRoots())

        assertEquals(
            listOf(CliDiagnostics.CLASSPATH_RESOLUTION_ERROR),
            configuration.diagnosticsCollector.diagnostics.map { it.factory }
                .filter { it == CliDiagnostics.CLASSPATH_RESOLUTION_ERROR || it == CliDiagnostics.CLASSPATH_RESOLUTION_WARNING },
            "a module-info.java without a configured reader is reported as an error",
        )
        assertTrue(result.modules.isEmpty(), "the source root does not become a module")
    }

    @Test
    fun testReaderMustNotSearchClasspathWhileRootsAreComputed(@TempDir tempDir: Path) {
        val sourceRoot = tempDir.writeModuleInfo()
        val configuration = CompilerConfiguration.create()
        configuration.putJavaModuleInfoSourceReader { _, findClassOnClasspath ->
            findClassOnClasspath(ClassId.topLevel(FqName("org.jspecify.annotations.NullMarked")))
            null
        }

        val exception = assertThrows<IllegalStateException> {
            configuration.resolver(findClassOnClasspath = { null }).convertClasspathRoots(sourceRoot.asSourceRoots())
        }
        assertTrue("while its roots are computed" in exception.message.orEmpty(), exception.message)
    }

    @Test
    fun testModuleInfoIsReadThroughRootEntries(@TempDir tempDir: Path) {
        val sourceRoot = tempDir.writeModuleInfo()
        val configuration = CompilerConfiguration.create()
        val texts = mutableListOf<String>()
        configuration.putJavaModuleInfoSourceReader { text, _ ->
            texts += text.toString()
            JavaModuleInfo("m", emptyList(), emptyList(), emptyList())
        }
        val nioEntries = NioRootEntryFactory(jarPool = null)
        val entryRoots = mutableListOf<JavaRoot>()
        val rootEntries = JvmRootEntryFactory { root -> entryRoots += root; nioEntries.createRootEntry(root) }

        val result = configuration.resolver(findClassOnClasspath = { null }, rootEntries = rootEntries)
            .convertClasspathRoots(sourceRoot.asSourceRoots())

        assertEquals(listOf("module m { }\n"), texts)
        assertEquals(listOf(JvmClasspathRootId.of(sourceRoot)), entryRoots.map { it.id }, "the source root is read through its entry")
        assertEquals(listOf("m"), result.modules.map { it.name })
        assertEquals(
            sourceRoot.resolve("module-info.java").toString(),
            (result.modules.single() as JavaModule.Explicit).moduleInfoFile.path,
            "the module keeps the virtual file of its descriptor",
        )
    }

    @Test
    fun testOutputDirectoryOnClasspathIsARootOfTheSourceModule(@TempDir tempDir: Path) {
        val sourceRoot = tempDir.writeModuleInfo()
        val outputDirectory = tempDir.resolve("out").createDirectories()
        val configuration = CompilerConfiguration.create()
        configuration.putJavaModuleInfoSourceReader { _, _ -> JavaModuleInfo("m", emptyList(), emptyList(), emptyList()) }

        val result = configuration.resolver(findClassOnClasspath = { null }, outputDirectory = JvmClasspathRootId.of(outputDirectory))
            .convertClasspathRoots(sourceRoot.asSourceRoots() + JvmClasspathRoot(outputDirectory.toFile()))

        assertEquals(
            listOf(JvmClasspathRootId.of(sourceRoot) to false, JvmClasspathRootId.of(outputDirectory) to true),
            result.modules.single().moduleRoots.map { it.id to it.isBinary },
        )
    }

    @Test
    fun testSingleJavaFileRootsAreReturnedSeparately(@TempDir tempDir: Path) {
        val sourceDirectory = tempDir.resolve("src").createDirectories()
        val javaFile = tempDir.resolve("A.java").also { it.writeText("class A {}\n") }
        val configuration = CompilerConfiguration.create()

        val result = configuration.resolver(findClassOnClasspath = { null })
            .convertClasspathRoots(listOf(JavaSourceRoot(sourceDirectory.toFile(), null), JavaSourceRoot(javaFile.toFile(), null)))

        assertEquals(listOf(JvmClasspathRootId.of(sourceDirectory)), result.roots.map { it.id })
        assertEquals(listOf(JvmClasspathRootId.of(javaFile)), result.singleJavaFileRoots.map { it.id })
    }

    private fun CompilerConfiguration.resolver(
        findClassOnClasspath: ClassIdToJavaClass,
        outputDirectory: JvmClasspathRootId? = null,
        rootEntries: JvmRootEntryFactory = JvmRootEntryFactory.VFS,
    ): ClasspathRootsResolver {
        val localFileSystem = CoreLocalFileSystem()
        return ClasspathRootsResolver(
            this,
            additionalModules = emptyList(),
            contentRootToVirtualFile = { localFileSystem.findFileByIoFile((it as JvmContentRoot).file) },
            javaModuleFinder = CliJavaModuleFinder(jdkHome = null, this, javaFileManager = null, jdkRelease = 8),
            requireStdlibModule = false,
            outputDirectory = outputDirectory,
            findClassOnClasspath = findClassOnClasspath,
            jdkRelease = 8,
            hasKotlinSources = true,
            rootEntries = rootEntries,
        )
    }

    private fun Path.writeModuleInfo(): Path {
        val sourceRoot = resolve("src").createDirectories()
        sourceRoot.resolve("module-info.java").writeText("module m { }\n")
        return sourceRoot
    }

    private fun Path.asSourceRoots(): List<ContentRoot> = listOf(JavaSourceRoot(toFile(), packagePrefix = null))
}
