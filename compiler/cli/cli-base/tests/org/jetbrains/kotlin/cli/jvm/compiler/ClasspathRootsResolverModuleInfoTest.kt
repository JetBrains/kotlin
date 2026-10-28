/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.config.JavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.putJavaModuleInfoSourceReader
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassIdToJavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
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

    private fun CompilerConfiguration.resolver(findClassOnClasspath: ClassIdToJavaClass): ClasspathRootsResolver {
        val localFileSystem = CoreLocalFileSystem()
        return ClasspathRootsResolver(
            this,
            additionalModules = emptyList(),
            contentRootToVirtualFile = { localFileSystem.findFileByIoFile((it as JavaSourceRoot).file) },
            javaModuleFinder = CliJavaModuleFinder(jdkHome = null, this, findClassOnClasspath, jdkRelease = 8),
            requireStdlibModule = false,
            outputDirectory = null,
            findClassOnClasspath = findClassOnClasspath,
            jdkRelease = 8,
            hasKotlinSources = true,
        )
    }

    private fun Path.writeModuleInfo(): Path {
        val sourceRoot = resolve("src").createDirectories()
        sourceRoot.resolve("module-info.java").writeText("module m { }\n")
        return sourceRoot
    }

    private fun Path.asSourceRoots() = listOf(JavaSourceRoot(toFile(), packagePrefix = null))
}
