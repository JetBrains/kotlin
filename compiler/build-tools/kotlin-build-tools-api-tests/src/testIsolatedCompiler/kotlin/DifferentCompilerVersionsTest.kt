/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.OperationCancelledException
import org.jetbrains.kotlin.buildtools.api.SharedApiClassesClassLoader
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Paths

class DifferentCompilerVersionsTest : BaseCompilationTest() {
    @Test
    @DisplayName("Test that different compiler versions can be used with isolated classloader")
    @TestMetadata("jvm-module-1")
    fun olderCompilerImpl() {
        val toolchain = createToolchain()
        project(toolchain, toolchain.createInProcessExecutionPolicy()) {
            val module1 = module("jvm-module-1", stdlibClasspath = stdlibClasspath)
            module1.compile {
                assertOutputs("FooKt.class", "Bar.class", "BazKt.class")
            }
        }
    }

    @Test
    @DisplayName("Test that cancellation exception can be thrown")
    @TestMetadata("jvm-module-1")
    fun cancellationExceptionTest() {
        val kotlinToolchains = createToolchain()
        println(kotlinToolchains.getCompilerVersion())
        val hasCancellationSupport = KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) > KotlinToolingVersion(2, 3, 0, null)
        assumeTrue(hasCancellationSupport)
        project(kotlinToolchains, kotlinToolchains.createInProcessExecutionPolicy()) {
            val module1 = module("jvm-module-1")
            module1.compileAndThrow(compilationAction = { operation ->
                operation.cancel()
            }) { ex: Throwable ->
                assertLogContainsSubstringExactlyTimes(
                    LogLevel.ERROR, "org.jetbrains.kotlin.progress.CompilationCanceledException", 1
                )
                assertTrue(ex is OperationCancelledException) { "Wrong exception type: ${ex}, expected OperationCancelledException" }
            }
        }
    }

    private fun createToolchain(): KotlinToolchains {
        val compilerClasspath = System.getProperty("kotlin.build-tools-api.test.compilerClasspath").split(File.pathSeparator)
            .map { Paths.get(it).toUri().toURL() }
        val compilerClassloader = URLClassLoader(compilerClasspath.toTypedArray(), SharedApiClassesClassLoader())
        return KotlinToolchains.loadImplementation(compilerClassloader)
    }

    private val stdlibClasspath =
        System.getProperty("kotlin.build-tools-api.test.stdlibClasspath").split(File.pathSeparator).map { Paths.get(it) }
}