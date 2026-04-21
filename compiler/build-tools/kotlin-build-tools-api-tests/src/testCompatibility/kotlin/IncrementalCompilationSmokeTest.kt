/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.VERBOSE
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.*
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.*
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.deleteRecursively
import kotlin.io.path.name
import kotlin.io.path.walk
import kotlin.io.path.writeText

class IncrementalCompilationSmokeTest : BaseCompilationTest() {
    @DisplayName("IC works with the externally tracked changes, similarly to Gradle")
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @TestMetadata("jvm-module-1")
    fun multiModuleExternallyTracked(scenario: ScenarioCreator) {
        runMultiModuleTest(scenario, useTrackedModules = false)
    }

    @DisplayName("IC works with the changes tracking via our internal machinery, similarly to Maven")
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @TestMetadata("jvm-module-1")
    fun multiModuleInternallyTracked(scenario: ScenarioCreator) {
        runMultiModuleTest(scenario, useTrackedModules = true)
    }

    @DisplayName("IC works with a mixed Java+Kotlin project via our internal machinery, similarly to Maven")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("kotlin-java-mixed")
    fun mixedModuleInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val kotlinToolchain = strategyConfig.first
        assumeTrue(
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
        jsProject(strategyConfig) {
            val libModule = module("js-ic-basic-lib")
            val appModule = module("js-ic-basic-app", dependencies = listOf(libModule))

            libModule.otherModules += appModule
            appModule.otherModules += libModule

            val libSources = libModule.sourcesDirectory.walk().filter { it.name.endsWith(".kt") }.toList()

            libModule.compileIncrementally(SourcesChanges.ToBeCalculated)
            appModule.compileIncrementally(SourcesChanges.ToBeCalculated)

            val modifiedFile = libSources.find { file -> file.name == "A.kt" } ?: error("No A.kt file in test project")
            modifiedFile.writeText(
                """
                    class A {
                        val x = "a"
                    }
                """.trimIndent()
            )
            libModule.compileIncrementally(SourcesChanges.ToBeCalculated) {
                val expectedCompiledSources = setOf("A.kt", "useAInLibMain.kt")
                assertCompiledSources(expectedCompiledSources)
            }
            appModule.compileIncrementally(SourcesChanges.Known(libModule.outputDirectory.walk().map(Path::toFile).toList(), emptyList())) {
                val expectedCompiledSources = setOf("useAInAppMain.kt")
                assertCompiledSources(expectedCompiledSources)
            }
        }
    }

    private fun runMixedModuleTest(strategyConfig: CompilerExecutionStrategyConfiguration, useTrackedModules: Boolean) {
        jvmScenario(strategyConfig) {
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

    private fun runMultiModuleTest(scenario: ScenarioCreator, useTrackedModules: Boolean) {
        scenario {
            Assumptions.assumeFalse(
                KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) == KotlinToolingVersion("2.2.21") && OS.MAC.isCurrentOs,
                "Known failure on Mac with 2.2.21"
            )
            if (useTrackedModules) {
                assumeTrue(
                    KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) >= KotlinToolingVersion(2, 1, 20, "Beta1"),
                    "Internal tracking is supported only since Kotlin 2.1.20-Beta1: KT-70556, the current version is ${kotlinToolchains.getCompilerVersion()}"
                )
                assumeFalse(this is JsScenarioDsl) // internal tracking currently doesn't fully work for JS
            }
            val moduleSpecs: List<ModuleSpec<BaseCompilationOperation.Builder, BaseIncrementalCompilationConfiguration.Builder>> = listOf(
                ModuleSpec("jvm-module-1"),
                ModuleSpec("jvm-module-2", dependencies = listOf("jvm-module-1"))
            )
            val (module1, module2) = if (useTrackedModules) {
                trackedModules(*moduleSpecs.toTypedArray())
            } else {
                modules(*moduleSpecs.toTypedArray())
            }

            module1.createPredefinedFile("secret.kt", "new-file")
            module1.replaceFileWithVersion("bar.kt", "add-default-argument")
            module1.deleteFile("baz.kt")
            module1.compile {
                assertCompiledSources("secret.kt", "bar.kt")
                if (this is JvmCompilationOperation.Builder) {
                    // SecretKt is added, BazKt is removed
                    assertOutputs("SecretKt.class", "Bar.class", "FooKt.class")
                }
            }
            module2.compile {
                assertCompiledSources("b.kt")
                assertNoOutputSetChanges()
            }
        }
    }
}
