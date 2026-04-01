/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.*
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.CLASSPATH
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.MODULE_NAME
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_REFLECT
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.NO_STDLIB
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JvmModule
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.TestKotlinLogger
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
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
import kotlin.io.path.*

class CancellationCompatibilitySmokeTest : BaseCompilationTest() {

    @DisplayName("Non-incremental in-process compilation test with cancellation")
    @Test
    fun nonIncrementalInProcessWithCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val hasCancellationSupport = hasCancellationSupport(kotlinToolchains.getCompilerVersion())
        project(kotlinToolchains, kotlinToolchains.createInProcessExecutionPolicy()) {
            val module1 = module("jvm-module-1")
            assumeTrue(hasCancellationSupport)
            module1.compileAndThrow(compilationAction = { operation ->
                operation.cancel()
            }) { ex: Throwable ->
                assertLogContainsSubstringExactlyTimes(
                    LogLevel.ERROR, "org.jetbrains.kotlin.progress.CompilationCanceledException", 1
                )
                assertTrue(ex is OperationCancelledException)
            }
        }
    }


    @OptIn(ExperimentalAtomicApi::class)
    @DisplayName("Non-incremental daemon compilation test with cancellation")
    @Test
    fun nonIncrementalDaemonWithCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val hasCancellationSupport = hasCancellationSupport(kotlinToolchains.getCompilerVersion())
        assumeTrue(hasCancellationSupport)
        runSingleShotDaemonTest(kotlinToolchains, additionalDaemonConfiguration = {
            this[ExecutionPolicy.WithDaemon.JVM_ARGUMENTS] = listOf(
                "Dkotlin.daemon.wait.before.compilation.for.tests=true"
            )
        }, additionalCleanupActions = { daemonRunPath ->
            daemonRunPath.resolve("daemon-test-start").deleteIfExists()
        }) { daemonPolicy, daemonRunPath ->
            project(kotlinToolchains, daemonPolicy) {
                val module1 = module("jvm-module-1") as JvmModule
                val operationWasCancelled = AtomicBoolean(false)
                with(module1) {
                    val allowedExtensions = setOf("kt", "kts", "java")
                    val compilationOperation = kotlinToolchain.jvm.jvmCompilationOperationBuilder(
                        sourcesDirectory.walk().filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }
                            .toList(),
                        outputDirectory
                    )
                    moduleCompilationConfigAction(compilationOperation)
                    compilationOperation.compilerArguments[NO_REFLECT] = true
                    compilationOperation.compilerArguments[NO_STDLIB] = true
                    compilationOperation.compilerArguments[CLASSPATH] = compileClasspath
                    compilationOperation.compilerArguments[MODULE_NAME] = moduleName
                    val logger = TestKotlinLogger()
                    val operation = compilationOperation.build()
                    val thread = thread {
                        try {
                            buildSession.executeOperation(operation, daemonPolicy, logger)
                        } catch (_: OperationCancelledException) {
                            operationWasCancelled.store(true)
                        }
                    }
                    operation.cancel()
                    daemonRunPath.resolve("daemon-test-start").createFile().toFile().deleteOnExit()
                    thread.join()
                    assertTrue { operationWasCancelled.load() }
                }
            }
        }
    }

    @DisplayName("Incremental in-process compilation test with cancellation")
    @Test
    fun incrementalInProcessWithCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val hasCancellationSupport = hasCancellationSupport(kotlinToolchains.getCompilerVersion())
        project(kotlinToolchains, kotlinToolchains.createInProcessExecutionPolicy()) {
            val module1 = module("jvm-module-1")
            assumeTrue(hasCancellationSupport)
            assertThrows<OperationCancelledException> {
                module1.compileIncrementally(SourcesChanges.Unknown, compilationAction = { operation ->
                    operation.cancel()
                })
            }
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    @DisplayName("Incremental daemon compilation test with cancellation")
    @Test
    fun incrementalDaemonWithCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val hasCancellationSupport = hasCancellationSupport(kotlinToolchains.getCompilerVersion())
        assumeTrue(hasCancellationSupport)
        runSingleShotDaemonTest(kotlinToolchains, additionalDaemonConfiguration = {
            this[ExecutionPolicy.WithDaemon.JVM_ARGUMENTS] = listOf(
                "-Dkotlin.daemon.wait.before.compilation.for.tests=true"
            )
        }, additionalCleanupActions = { daemonRunPath ->
            daemonRunPath.resolve("daemon-test-start").deleteIfExists()
        }) { daemonPolicy, daemonRunPath ->
            project(kotlinToolchains, daemonPolicy) {
                val module1 = module("jvm-module-1") as JvmModule
                val operationWasCancelled = AtomicBoolean(false)
                with(module1) {
                    val allowedExtensions = setOf("kt", "kts", "java")
                    val compilationOperation = kotlinToolchain.jvm.jvmCompilationOperationBuilder(
                        sourcesDirectory.walk().filter { path -> path.pathString.run { allowedExtensions.any { endsWith(".$it") } } }
                            .toList(),
                        outputDirectory
                    )
                    moduleCompilationConfigAction(compilationOperation)
                    compilationOperation.compilerArguments[NO_REFLECT] = true
                    compilationOperation.compilerArguments[NO_STDLIB] = true
                    compilationOperation.compilerArguments[CLASSPATH] = compileClasspath
                    compilationOperation.compilerArguments[MODULE_NAME] = moduleName

                    val snapshotIcConfig = compilationOperation.snapshotBasedIcConfigurationBuilder(
                        icCachesDir,
                        SourcesChanges.Unknown,
                        emptyList(),
                    )
                    compilationOperation[JvmCompilationOperation.INCREMENTAL_COMPILATION] = snapshotIcConfig.build()

                    val logger = TestKotlinLogger()
                    val operation = compilationOperation.build()
                    val thread = thread {
                        try {
                            buildSession.executeOperation(operation, daemonPolicy, logger)
                        } catch (_: OperationCancelledException) {
                            operationWasCancelled.store(true)
                        }
                    }
                    operation.cancel()
                    daemonRunPath.resolve("daemon-test-start").createFile().toFile().deleteOnExit()
                    thread.join()
                    assertTrue { operationWasCancelled.load() }
                }
            }
        }
    }

    @DisplayName("Sample non-incremental compilation test without cancellation support")
    @Test
    fun nonIncrementalWithoutCancellation() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        val hasCancellationSupport = hasCancellationSupport(kotlinToolchains.getCompilerVersion())
        project(kotlinToolchains, kotlinToolchains.createInProcessExecutionPolicy()) {
            val module1 = module("jvm-module-1")
            assumeFalse(hasCancellationSupport)
            val exception = assertThrows<IllegalStateException> {
                module1.compile(compilationAction = { operation ->
                    operation.cancel()
                })
            }
            assertEquals("Cancellation is supported from compiler version 2.3.20.", exception.message)
        }
    }

    private fun hasCancellationSupport(compilerVersion: String) =
        KotlinToolingVersion(compilerVersion) >= KotlinToolingVersion(2, 3, 20, "RC")
}
