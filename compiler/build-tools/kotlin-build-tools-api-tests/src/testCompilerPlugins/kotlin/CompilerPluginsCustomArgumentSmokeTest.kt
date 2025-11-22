/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertClassDeclarationsContain
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class CompilerPluginsCustomArgumentSmokeTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application")
    @TestMetadata("compiler-plugins")
    fun smokeTestCompilerPluginsApplication(strategyConfig: CompilerExecutionStrategyConfiguration) {
        smokeTest(strategyConfig) {
            it.compilerArguments[COMPILER_PLUGINS] = listOf(NOARG_PLUGIN, ASSIGNMENT_PLUGIN)
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application through applyArgumentStrings (default way)")
    @TestMetadata("compiler-plugins")
    fun smokeTestCompilerPluginsApplicationDefaultArgumentsString(strategyConfig: CompilerExecutionStrategyConfiguration) {
        smokeTest(strategyConfig) {
            val compilerPluginArgs = buildList {
                add("-Xplugin=${(NOARG_PLUGIN.classpath + ASSIGNMENT_PLUGIN.classpath).joinToString(",")}")
                add("-P")
                add(buildList {
                    for (arg in NOARG_PLUGIN.rawArguments) {
                        add("plugin:${NOARG_PLUGIN.pluginId}:${arg.key}=${arg.value}")
                    }
                    for (arg in ASSIGNMENT_PLUGIN.rawArguments) {
                        add("plugin:${ASSIGNMENT_PLUGIN.pluginId}:${arg.key}=${arg.value}")
                    }
                }.joinToString(","))
            }
            it.compilerArguments.applyArgumentStrings(it.compilerArguments.toArgumentStrings() + compilerPluginArgs)
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application through applyArgumentStrings (modern way)")
    @TestMetadata("compiler-plugins")
    fun smokeTestCompilerPluginsApplicationModernArgumentsString(strategyConfig: CompilerExecutionStrategyConfiguration) {
        smokeTest(strategyConfig) {
            val compilerPluginArgs = buildList {
                add(
                    "-Xcompiler-plugin=${(NOARG_PLUGIN.classpath).joinToString(",")}=${
                        buildList {
                            for (arg in NOARG_PLUGIN.rawArguments) {
                                add("${arg.key}=${arg.value}")
                            }
                        }.joinToString(",")
                    }"
                )
                add(
                    "-Xcompiler-plugin=${(ASSIGNMENT_PLUGIN.classpath).joinToString(",")}=${
                        buildList {
                            for (arg in ASSIGNMENT_PLUGIN.rawArguments) {
                                add("${arg.key}=${arg.value}")
                            }
                        }.joinToString(",")
                    }"
                )
            }
            it.compilerArguments.applyArgumentStrings(it.compilerArguments.toArgumentStrings() + compilerPluginArgs)
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application through applyArgumentStrings (combo BTA + default way)")
    @TestMetadata("compiler-plugins")
    fun smokeTestCompilerPluginsApplicationComboDefaultArgumentsString(strategyConfig: CompilerExecutionStrategyConfiguration) {
        smokeTest(strategyConfig) {
            val compilerPluginArgs = buildList {
                add("-Xplugin=${(NOARG_PLUGIN.classpath).joinToString(",")}")
                add("-P")
                add(buildList {
                    for (arg in NOARG_PLUGIN.rawArguments) {
                        add("plugin:${NOARG_PLUGIN.pluginId}:${arg.key}=${arg.value}")
                    }
                }.joinToString(","))
            }
            it.compilerArguments[COMPILER_PLUGINS] = listOf(ASSIGNMENT_PLUGIN)
            it.compilerArguments.applyArgumentStrings(it.compilerArguments.toArgumentStrings() + compilerPluginArgs)
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application through applyArgumentStrings (combo BTA + modern way)")
    @TestMetadata("compiler-plugins")
    fun smokeTestCompilerPluginsApplicationComboModernArgumentsString(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("compiler-plugins")
            module.compile(compilationConfigAction = {
                val compilerPluginArgs = buildList {
                    add(
                        "-Xcompiler-plugin=${(NOARG_PLUGIN.classpath).joinToString(",")}=${
                            buildList {
                                for (arg in NOARG_PLUGIN.rawArguments) {
                                    add("${arg.key}=${arg.value}")
                                }
                            }.joinToString(",")
                        }"
                    )
                }
                it.compilerArguments[COMPILER_PLUGINS] = listOf(ASSIGNMENT_PLUGIN)
                it.compilerArguments.applyArgumentStrings(it.compilerArguments.toArgumentStrings() + compilerPluginArgs)
            }) {
                // BTA currently transforms the structured way to the "default way", such a combination is considered illegal
                expectFail()
                assertLogContainsPatterns(
                    LogLevel.ERROR,
                    "Mixing legacy and modern plugin arguments is prohibited. Please use only one syntax.*".toRegex(RegexOption.DOT_MATCHES_ALL)
                )
            }
        }
    }

    private fun smokeTest(
        strategyConfig: CompilerExecutionStrategyConfiguration,
        pluginsConfiguration: (JvmCompilationOperation) -> Unit,
    ) {
        project(strategyConfig) {
            val module = module("compiler-plugins")
            module.compile(compilationConfigAction = pluginsConfiguration) {
                assertOutputs(
                    "GenerateAssignment.class",
                    "GenerateNoArgsConstructor.class",
                    "AssignableClass.class",
                    "AssignableClassKt.class",
                    "SomeClass.class",
                    "AssignmentUsageKt.class", // ensures the usage of assignment was compiled
                )
                assertClassDeclarationsContain(
                    classFqn = "SomeClass",
                    setOf(
                        "public SomeClass();",
                        "public SomeClass(int, AssignableClass);",
                    )
                )
            }
        }
    }
}
