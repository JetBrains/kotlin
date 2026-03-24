/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.CommonJsAndWasmArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.VERBOSE
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.js.jsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation.Companion.INCREMENTAL_COMPILATION
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.*
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.assertNoOutputSetChanges
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.*
import org.junit.jupiter.api.condition.OS

class IncrementalCompilationSmokeTest : BaseCompilationTest() {
    @DisplayName("IC works with the externally tracked changes, similarly to Gradle")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun multiModuleExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        runMultiModuleTest(strategyConfig, useTrackedModules = false)
    }

    @DisplayName("IC works with the changes tracking via our internal machinery, similarly to Maven")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun multiModuleInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val kotlinToolchain = strategyConfig.first
        Assumptions.assumeTrue(
            KotlinToolingVersion(kotlinToolchain.getCompilerVersion()) >= KotlinToolingVersion(2, 1, 20, "Beta1"),
            "Internal tracking is supported only since Kotlin 2.1.20-Beta1: KT-70556, the current version is ${kotlinToolchain.getCompilerVersion()}"
        )
        runMultiModuleTest(strategyConfig, useTrackedModules = true)
    }

    @DisplayName("IC works with a mixed Java+Kotlin project via our internal machinery, similarly to Maven")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("kotlin-java-mixed")
    fun mixedModuleInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val kotlinToolchain = strategyConfig.first
        Assumptions.assumeTrue(
            KotlinToolingVersion(kotlinToolchain.getCompilerVersion()) >= KotlinToolingVersion(2, 1, 20, "Beta1"),
            "Internal tracking is supported only since Kotlin 2.1.20-Beta1: KT-70556, the current version is ${kotlinToolchain.getCompilerVersion()}"
        )
        runMixedModuleTest(strategyConfig, useTrackedModules = true)
    }

    @DisplayName("IC works with a mixed Java+Kotlin project with the externally tracked changes, similarly to Gradle")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("kotlin-java-mixed")
    fun mixedModuleExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        runMixedModuleTest(strategyConfig, useTrackedModules = false)
    }

    @OptIn(ExperimentalCompilerArgument::class)
    @DisplayName("Basic IC setup works for JS project")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("js-ic-basic")
    fun jsBasicIcWorks(strategyConfig: CompilerExecutionStrategyConfiguration) {
        assumeTrue(strategyConfig.first.getCompilerVersion().startsWith("2.4"))
        val toolchain = strategyConfig.first
        val stdlibKlib = Path(System.getProperty("kotlin.build-tools-api.test.jsStdlibClasspath"))

        project(strategyConfig) {
            val appModule = module("js-ic-basic-app")
            val libModule = module("js-ic-basic-lib")

            val libSources = libModule.sourcesDirectory.walk().filter { it.name.endsWith(".kt") }.toList()
            val appSources = appModule.sourcesDirectory.walk().filter { it.name.endsWith(".kt") }.toList()

            val modulesInfo = listOf(
                IncrementalModule(
                    "app",
                    appModule.outputDirectory,
                    appModule.buildDirectory,
                    appModule.icCachesDir,
                ),
                IncrementalModule(
                    "lib",
                    libModule.outputDirectory.resolve("lib.klib"),
                    libModule.buildDirectory,
                    libModule.icCachesDir,
                ),
            )

            val compilationOperation1 = toolchain.js.jsKlibCompilationOperation(
                libSources,
                libModule.outputDirectory
            ) {
                compilerArguments[CommonJsAndWasmArguments.LIBRARIES] = listOf(stdlibKlib)
                compilerArguments[CommonJsAndWasmArguments.IR_OUTPUT_NAME] = "lib"
                this[INCREMENTAL_COMPILATION] = this.historyBasedIcConfigurationBuilder(
                    projectDirectory,
                    libModule.icCachesDir,
                    SourcesChanges.ToBeCalculated,
                    modulesInfo
                ).build()
            }

            val compilationOperation2 = toolchain.js.jsKlibCompilationOperation(
                appSources,
                appModule.outputDirectory
            ) {
                compilerArguments[CommonJsAndWasmArguments.LIBRARIES] = listOf(
                    stdlibKlib,
                    libModule.outputDirectory.resolve("lib.klib")
                )
                compilerArguments[CommonJsAndWasmArguments.IR_OUTPUT_NAME] = "app"
                this[INCREMENTAL_COMPILATION] = this.historyBasedIcConfigurationBuilder(
                    projectDirectory,
                    appModule.icCachesDir,
                    SourcesChanges.Unknown,
                    modulesInfo
                ).build()
            }
            toolchain.createBuildSession().use {
                var logger = TestKotlinLogger()
                try {
                    var result = it.executeOperation(compilationOperation1, strategyConfig.second, logger)
                    assertEquals(CompilationResult.COMPILATION_SUCCESS, result)

                    logger = TestKotlinLogger()
                    result = it.executeOperation(compilationOperation2, strategyConfig.second, logger)
                    assertEquals(CompilationResult.COMPILATION_SUCCESS, result)

                    val modifiedFile = libSources.find { file -> file.name == "A.kt" } ?: error("No A.kt file in test project")
                    modifiedFile.writeText(
                        """
                    class A {
                        val x = "a"
                    }
                """.trimIndent()
                    )
                    logger = TestKotlinLogger()
                    result = it.executeOperation(compilationOperation1.toBuilder().build(), strategyConfig.second, logger)
                    assertEquals(CompilationResult.COMPILATION_SUCCESS, result)

                    var expectedCompiledSources = listOf("A.kt", "useAInLibMain.kt")
                    assertCompiledSources(logger, expectedCompiledSources, libModule)

                    logger = TestKotlinLogger()
                    result = it.executeOperation(compilationOperation2.toBuilder().apply {
                        val previousIc = this[INCREMENTAL_COMPILATION] as JsHistoryBasedIncrementalCompilationConfiguration
                        this[INCREMENTAL_COMPILATION] = historyBasedIcConfigurationBuilder(
                            projectDirectory,
                            previousIc.workingDirectory,
                            SourcesChanges.Known(listOf(libModule.outputDirectory.resolve("lib.klib").toFile()), emptyList()),
                            previousIc.modulesInformation
                        ).build()
                    }.build(), strategyConfig.second, logger)
                    assertEquals(CompilationResult.COMPILATION_SUCCESS, result)

                    expectedCompiledSources = listOf("useAInAppMain.kt")
                    assertCompiledSources(logger, expectedCompiledSources, appModule)
                } catch (e: Throwable) {
                    logger.printBuildOutput(LogLevel.DEBUG)
                    throw e
                }
            }
        }
    }

    private fun runMixedModuleTest(strategyConfig: CompilerExecutionStrategyConfiguration, useTrackedModules: Boolean) {
        scenario(strategyConfig) {
            val compilerArgumentsConf: (JvmCompilationOperation.Builder) -> Unit = {
                it.compilerArguments[VERBOSE] = true
            }
            val module1 = if (useTrackedModules) {
                trackedModule("kotlin-java-mixed", compilationConfigAction = compilerArgumentsConf)
            } else {
                module("kotlin-java-mixed", compilationConfigAction = compilerArgumentsConf)
            }

            module1.replaceFileWithVersion("main.kt", "add-argument")
            module1.replaceFileWithVersion("apkg/AClass.java", "add-argument")
            module1.compile {
                assertCompiledSources("main.kt", "bpkg/BClass.kt")
                assertOutputs("bpkg/MainKt.class", "bpkg/BClass.class")
                if (strategyConfig.first::class.simpleName != "KotlinToolchainsV1Adapter") { // v1 is not producing some logs and that's expected
                    val count = if (useTrackedModules) {
                        1
                    } else {
                        2 // the second occurrence is in the SourcesChanges log line
                    }
                    assertLogContainsSubstringExactlyTimes(LogLevel.DEBUG, "AClass.java", count) // no duplication of java sources
                }
            }
        }
    }

    private fun runMultiModuleTest(strategyConfig: CompilerExecutionStrategyConfiguration, useTrackedModules: Boolean) {
        val kotlinToolchain = strategyConfig.first
        Assumptions.assumeFalse(
            KotlinToolingVersion(kotlinToolchain.getCompilerVersion()) == KotlinToolingVersion("2.2.21") && OS.MAC.isCurrentOs,
            "Known failure on Mac with 2.2.21"
        )
        scenario(strategyConfig) {
            val module1 = if (useTrackedModules) {
                trackedModule("jvm-module-1")
            } else {
                module("jvm-module-1")
            }

            val module2 = if (useTrackedModules) {
                trackedModule("jvm-module-2", listOf(module1))
            } else {
                module("jvm-module-2", listOf(module1))
            }

            module1.createPredefinedFile("secret.kt", "new-file")
            module1.replaceFileWithVersion("bar.kt", "add-default-argument")
            module1.deleteFile("baz.kt")
            module1.compile {
                assertCompiledSources("secret.kt", "bar.kt")
                // SecretKt is added, BazKt is removed
                assertOutputs("SecretKt.class", "Bar.class", "FooKt.class")
            }
            module2.compile {
                assertCompiledSources("b.kt")
                assertNoOutputSetChanges()
            }
        }
    }
}

private fun assertCompiledSources(
    logger: TestKotlinLogger,
    expectedCompiledSources: List<String>,
    appModule: Module,
) {
    val actualCompiledSources = (logger.logMessagesByLevel[LogLevel.DEBUG] ?: emptyList())
        .map { it.removePrefix("[KOTLIN] ") }
        .filter { it.startsWith("compile iteration") }
        .flatMap { it.replace("compile iteration: ", "").trim().split(", ") }
        .toSet()
    val normalizedPaths = expectedCompiledSources
        .map { appModule.sourcesDirectory.resolve(it) }
        .map { it.relativeTo(appModule.project.projectDirectory) }
        .map(Path::toString)
        .toSet()
    assertEquals(normalizedPaths, actualCompiledSources) {
        """
            Compiled sources do not match. Set diff:
            Unexpected: ${actualCompiledSources - normalizedPaths}
            Missing: ${normalizedPaths - actualCompiledSources}
            
            Full sets:
        """.trimIndent()
    }
}
