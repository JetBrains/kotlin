/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.OperationCancelledException
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.JvmModule
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.TestKotlinLogger
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.thread
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.pathString
import kotlin.io.path.walk

class CancellationCompatibilitySmokeTest : BaseCompilationTest() {

    @DisplayName("Non-incremental compilation test with cancellation")
    @Test
    fun nonIncrementalWithCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(BaseCompilationTest::class.java.classLoader)
        val hasCancellationSupport = KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) > KotlinToolingVersion(2, 3, 0, "Beta1")
        project(kotlinToolchains, kotlinToolchains.createInProcessExecutionPolicy()) {
            val module1 = module("jvm-module-1")
            assumeTrue(hasCancellationSupport)
            module1.compileAndThrow(compilationConfigAction = { operation ->
                operation.cancel()
            }) { _, ex: Throwable ->
                assertLogContainsSubstringExactlyTimes(
                    LogLevel.ERROR, "org.jetbrains.kotlin.progress.CompilationCanceledException", 1
                )
                assertTrue(ex is OperationCancelledException)
            }
        }
    }


    @OptIn(ExperimentalAtomicApi::class)
    @DisplayName("Daemon compilation test with cancellation")
    @Test
    fun daemonWithCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(BaseCompilationTest::class.java.classLoader)
        val hasCancellationSupport = KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) > KotlinToolingVersion(2, 3, 0, "Beta1")
        val daemonPolicy = kotlinToolchains.createDaemonExecutionPolicy()
        assumeTrue(hasCancellationSupport)
        project(kotlinToolchains, daemonPolicy) {
            val daemonRunPath = projectDirectory.resolve("daemon-files").createDirectories()
            daemonPolicy[ExecutionPolicy.WithDaemon.JVM_ARGUMENTS] = listOf(
                "Dkotlin.daemon.wait.before.compilation.for.tests=true"
            )
            daemonPolicy[ExecutionPolicy.WithDaemon.DAEMON_RUN_DIR_PATH] = daemonRunPath
            daemonPolicy[ExecutionPolicy.WithDaemon.SHUTDOWN_DELAY_MILLIS] = 100
            val module1 = module("jvm-module-1") as JvmModule
            val operationWasCancelled = AtomicBoolean(false)
            with(module1) {
                val allowedExtensions = setOf("kt", "kts", "java")
                val compilationOperation = kotlinToolchain.jvm.createJvmCompilationOperation(
                    sourcesDirectory.walk().filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }.toList(),
                    outputDirectory
                )
                moduleCompilationConfigAction(compilationOperation)
                compilationOperation.compilerArguments[NO_REFLECT] = true
                compilationOperation.compilerArguments[NO_STDLIB] = true
                compilationOperation.compilerArguments[CLASSPATH] = compileClasspath
                compilationOperation.compilerArguments[MODULE_NAME] = moduleName
                val logger = TestKotlinLogger()
                val thread = thread {
                    try {
                        buildSession.executeOperation(compilationOperation, daemonPolicy, logger)
                    } catch (_: OperationCancelledException) {
                        operationWasCancelled.store(true)
                    }
                }
                compilationOperation.cancel()
                daemonRunPath.resolve("daemon-test-start").createFile()
                thread.join()
                assertTrue { operationWasCancelled.load() }
            }
        }
    }

    @DisplayName("Incremental compilation test with cancellation")
    @Test
    fun incrementalWithCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(BaseCompilationTest::class.java.classLoader)
        val hasCancellationSupport = KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) > KotlinToolingVersion(2, 3, 0, "Beta1")
        project(kotlinToolchains, kotlinToolchains.createInProcessExecutionPolicy()) {
            val module1 = module("jvm-module-1")
            assumeTrue(hasCancellationSupport)
            assertThrows<OperationCancelledException> {
                module1.compileIncrementally(SourcesChanges.Unknown, compilationConfigAction = { operation ->
                    operation.cancel()
                })
            }
        }
    }


    @DisplayName("Sample non-incremental compilation test without cancellation support")
    @Test
    fun nonIncrementalWithoutCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(BaseCompilationTest::class.java.classLoader)
        val hasCancellationSupport = KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) > KotlinToolingVersion(2, 3, 0, "Beta1")
        project(kotlinToolchains, kotlinToolchains.createInProcessExecutionPolicy()) {
            val module1 = module("jvm-module-1")
            assumeFalse(hasCancellationSupport)
            val exception = assertThrows<IllegalStateException> {
                module1.compile(compilationConfigAction = { operation ->
                    operation.cancel()
                })
            }
            assertEquals("Cancellation is supported from compiler version 2.3.20.", exception.message)
        }
    }
}
