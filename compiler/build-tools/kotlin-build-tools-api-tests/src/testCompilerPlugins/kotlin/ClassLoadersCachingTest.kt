/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.*
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.walk

class ClassLoadersCachingTest : BaseCompilationTest() {
    @DefaultStrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Test that plugins loader is used to cache compiler plugins")
    fun testPluginsLoaderCache(project: ProjectCreator) {
        project {
            val module = module("sandbox-plugin", moduleCompilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
            })
            runSingleShotDaemonTest(
                kotlinToolchain, {
                    val logsPath = module.buildDirectory.resolve("daemon-logs")
                    set(ExecutionPolicy.WithDaemon.LOGS_PATH, logsPath)
                }) { daemonPolicy, _ ->

                val finalPolicy = module.defaultStrategyConfig as? ExecutionPolicy.InProcess ?: daemonPolicy
                val classLoaderCacheRegex = "Creating new classloader for classpath.*".toRegex()

                module.compile(strategyConfig = finalPolicy) {
                    if (finalPolicy is ExecutionPolicy.WithDaemon) {
                        assertEquals(1, getDaemonLogs(daemonPolicy).lines().count { it.contains(classLoaderCacheRegex) })
                    } else {
                        assertLogContainsPatterns(LogLevel.INFO, classLoaderCacheRegex)
                    }
                }
                module.compile(strategyConfig = finalPolicy) {
                    if (finalPolicy is ExecutionPolicy.WithDaemon) {
                        // it's the same log, so take into account the previously existing line
                        assertEquals(1, getDaemonLogs(daemonPolicy).lines().count { it.contains(classLoaderCacheRegex) })
                    } else {
                        assertLogDoesNotContainPatterns(LogLevel.INFO, classLoaderCacheRegex)
                    }
                }
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("Test that plugins loader is used to cache compiler plugins with IC runner")
    fun testPluginsLoaderCacheIc(scenario: ScenarioCreator) {
        scenario {
            assumeTrue(this.strategyConfig is ExecutionPolicy.InProcess)

            val module = module("sandbox-plugin", compilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
            })

            // cache was already filled when module was being created for the scenario, so we need to clear it
            kotlinToolchains.clearClassloadersCache()

            module.replaceFileWithVersion("main.kt", "step1")
            module.compile(forceOutput = LogLevel.INFO) {
                assertLogContainsPatterns(LogLevel.INFO, "Creating new classloader for classpath.*".toRegex())
            }

            module.replaceFileWithVersion("main.kt", "step2")
            module.compile {
                assertLogDoesNotContainPatterns(LogLevel.INFO, "Creating new classloader for classpath.*".toRegex())
            }
        }
    }

    private fun KotlinToolchains.clearClassloadersCache() {
        val cache = javaClass.declaredFields.single { it.name == "classloadersCache" }.apply { isAccessible = true }.get(this)
        cache?.javaClass?.methods?.find { it.name == "close" }?.invoke(cache)
    }

    @DefaultStrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Test that plugins loader is not used to cache compiler plugins when disabled")
    fun testPluginsLoaderCacheDisabled(project: ProjectCreator) {
        project {
            assumeTrue(this.defaultStrategyConfig is ExecutionPolicy.InProcess)

            val module = module("sandbox-plugin", moduleCompilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
                operation[BuildOperation.ENABLE_CLASSLOADER_CACHE] = false
            })

            module.compile(forceOutput = LogLevel.INFO) {
                assertLogDoesNotContainPatterns(LogLevel.INFO, "Creating new classloader for classpath.*".toRegex())
            }
        }
    }
}

private fun getDaemonLogs(daemonPolicy: ExecutionPolicy.WithDaemon): String =
    daemonPolicy[ExecutionPolicy.WithDaemon.LOGS_PATH].walk().toList().first { it.name.endsWith("log") }.readText()
