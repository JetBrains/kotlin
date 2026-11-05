/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.BuildOperation.Companion.ENABLE_CLASSLOADER_CACHE
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.AbstractProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ProjectCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.io.path.copyTo
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory

class ClassLoadersCachingTest : BaseCompilationTest() {
    @DefaultStrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Test that plugins loader is used to cache compiler plugins")
    fun testPluginsLoaderCache(project: ProjectCreator) {
        project {
            assumeTrue(this.defaultStrategyConfig is ExecutionPolicy.InProcess)

            val module = module("sandbox-plugin", moduleCompilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
                operation[ENABLE_CLASSLOADER_CACHE] = true
            })

            module.compile {
                assertLogContainsPatterns(LogLevel.INFO, "Creating new classloader for classpath.*".toRegex())
            }
            module.compile {
                assertLogDoesNotContainPatterns(LogLevel.INFO, "Creating new classloader for classpath.*".toRegex())
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
                operation[ENABLE_CLASSLOADER_CACHE] = true
            })

            // cache was already filled when module was being created for the scenario, so we need to clear it
            kotlinToolchains.clearClassloadersCache()

            module.replaceFileWithVersion("main.kt", "step1")
            module.compile {
                assertLogContainsPatterns(LogLevel.INFO, "Creating new classloader for classpath.*".toRegex())
            }

            module.replaceFileWithVersion("main.kt", "step2")
            module.compile {
                assertLogDoesNotContainPatterns(LogLevel.INFO, "Creating new classloader for classpath.*".toRegex())
            }
        }
    }

    private fun KotlinToolchains.clearClassloadersCache() {
        val cache = javaClass.declaredFields.single { it.name == "classloadersCache" }.apply { isAccessible = true }
            .get(this)
        val cachePrivate = cache.javaClass.declaredFields.single { it.name == "cache"}.apply { isAccessible = true }.get(cache)
        val cacheInterface = cachePrivate?.javaClass?.interfaces?.single { it.name == "org.jetbrains.kotlin.com.google.common.cache.Cache" }
        cacheInterface?.methods?.find { it.name == "invalidateAll" && it.parameterCount == 0 }?.invoke(cachePrivate)
    }

    @DefaultStrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Test that plugins loader is not used to cache compiler plugins when disabled")
    fun testPluginsLoaderCacheDisabled(project: ProjectCreator) {
        project {
            assumeTrue(this.defaultStrategyConfig is ExecutionPolicy.InProcess)

            val module = module("sandbox-plugin", moduleCompilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
                operation[ENABLE_CLASSLOADER_CACHE] = false
            })

            module.compile {
                assertLogDoesNotContainPatterns(LogLevel.INFO, "Creating new classloader for classpath.*".toRegex())
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Test that classloader eviction during concurrent compilations does not break in-flight operations")
    fun testParallelCompilationsWithCacheEviction(project: ProjectCreator) {
        project {
            assumeTrue(this.defaultStrategyConfig is ExecutionPolicy.InProcess)

            val concurrentCompilations = 15
            val executor = Executors.newFixedThreadPool(concurrentCompilations)
            try {
                val basePluginPaths = PLUGIN_SANDBOX_PLUGIN.classpath
                val plugins = (0 until concurrentCompilations).map { index ->
                    val copiedPaths = basePluginPaths.map { originalPath ->
                        val target = workingDirectory.resolve("plugin-copy-$index-${originalPath.fileName}")
                        if (originalPath.isDirectory()) {
                            originalPath.copyToRecursively(target, followLinks = false)
                        } else {
                            originalPath.copyTo(target, overwrite = true)
                        }
                        target
                    }
                    CompilerPlugin(
                        pluginId = PLUGIN_SANDBOX_PLUGIN.pluginId,
                        classpath = copiedPaths,
                        rawArguments = PLUGIN_SANDBOX_PLUGIN.rawArguments,
                        orderingRequirements = PLUGIN_SANDBOX_PLUGIN.orderingRequirements,
                    )
                }

                val startLatch = CountDownLatch(concurrentCompilations)
                val projectClass = this.javaClass
                val constructor = projectClass.constructors.first { ctor ->
                    val types = ctor.parameterTypes
                    types.size in 3..4 && types[0] == KotlinToolchains::class.java && types[1] == ExecutionPolicy::class.java && types[2] == Path::class.java
                }
                val futures = (0 until concurrentCompilations).map { index ->
                    executor.submit<Unit> {
                        startLatch.countDown()
                        startLatch.await()
                        val projectDirectory = workingDirectory.resolve("concurrent-project-$index").createDirectories()
                        val projectInstance = if (constructor.parameterCount == 3) {
                            constructor.newInstance(this.kotlinToolchain, this.defaultStrategyConfig, projectDirectory)
                        } else {
                            constructor.newInstance(this.kotlinToolchain, this.defaultStrategyConfig, projectDirectory, false)
                        } as AbstractProject<*, *, *>
                        projectInstance.use { concurrentProject ->
                            val module = concurrentProject.module("sandbox-plugin", moduleCompilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                                    operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(plugins[index])
                                    operation[ENABLE_CLASSLOADER_CACHE] = true
                                })
                            module.compile {}
                        }
                    }
                }

                futures.forEach { it.get(10, TimeUnit.SECONDS) }
            } finally {
                executor.shutdownNow()
            }
        }
    }
}
