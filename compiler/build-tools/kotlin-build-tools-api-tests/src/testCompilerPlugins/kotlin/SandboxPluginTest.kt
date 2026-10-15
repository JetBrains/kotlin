/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsLines
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInCompilerPlugins
import org.junit.jupiter.api.DisplayName

@MustRunOnChangesInCompilerPlugins
class SandboxPluginTest : BaseCompilationTest() {

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-87217 and KT-87370 Generating top-level callables from compiler plugins shouldn't break IC")
    @TestMetadata("sandbox-plugin")
    fun testGeneratingTopLevelCallables(scenario: ScenarioCreator) {
        scenario {
            val module = module("sandbox-plugin", compilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
            })

            module.compile {}
            repeat(3) { i ->
                module.replaceFileWithVersion("main.kt", "step${i + 1}")
                module.compile {
                    assertCompiledSources("main.kt")
                    assertLogContainsLines(LogLevel.DEBUG, "Incremental compilation completed")
                }
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89912 JS/Wasm IC fails after removing FIR-generated file")
    @TestMetadata("sandbox-plugin")
    fun testGeneratingThenRemovingTopLevelCallables(scenario: ScenarioCreator) {
        scenario {
            val module = module("sandbox-plugin", compilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
            })

            module.compile {}
            module.replaceFileWithVersion("main.kt", "removed_callables")
            module.compile {
                assertCompiledSources("main.kt")
                assertLogContainsLines(LogLevel.DEBUG, "Incremental compilation completed")
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("Changing the name of a plugin-generated file shouldn't break IC")
    @TestMetadata("sandbox-plugin")
    fun testChangingGeneratedFileName(scenario: ScenarioCreator) {
        scenario {
            val module = module("sandbox-plugin", compilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
            })

            module.replaceFileWithVersion("main.kt", "renamed_generated_file")
            module.compile {
                assertCompiledSources("main.kt")
                assertLogContainsLines(LogLevel.DEBUG, "Incremental compilation completed")
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("Deleting a source file that triggers callable generation shouldn't break IC")
    @TestMetadata("sandbox-plugin")
    fun testDeletingSourceFileThatTriggersCallableGeneration(scenario: ScenarioCreator) {
        scenario {
            val module = module("sandbox-plugin", compilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
            })

            module.deleteFile("main.kt")
            module.compile {
                assertNoCompiledSources()
                assertLogContainsLines(LogLevel.DEBUG, "Incremental compilation completed")
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("Removing and then regenerating top-level callables shouldn't break IC")
    @TestMetadata("sandbox-plugin")
    fun testGeneratingRemovingAndRegeneratingTopLevelCallables(scenario: ScenarioCreator) {
        scenario {
            val module = module("sandbox-plugin", compilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
            })

            module.replaceFileWithVersion("main.kt", "removed_callables")
            module.compile {
                assertCompiledSources("main.kt")
                assertLogContainsLines(LogLevel.DEBUG, "Incremental compilation completed")
            }
            module.replaceFileWithVersion("main.kt", "with_callables")
            module.compile {
                assertCompiledSources("main.kt")
                assertLogContainsLines(LogLevel.DEBUG, "Incremental compilation completed")
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("Removed generated callables are not resolvable after incremental compilation")
    @TestMetadata("sandbox-plugin")
    fun testRemovedGeneratedCallablesAreUnresolved(scenario: ScenarioCreator) {
        scenario {
            val module = module("sandbox-plugin", compilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
            })

            module.replaceFileWithVersion("main.kt", "removed_annotations_with_usages")
            module.compile {
                expectFail()
                assertLogContainsPatterns(
                    LogLevel.ERROR,
                    ".*[Uu]nresolved reference.*dummyA.*".toRegex(),
                    ".*[Uu]nresolved reference.*dummyB.*".toRegex(),
                )
            }
            module.replaceFileWithVersion("main.kt", "removed_callables")
            module.compile {
                assertCompiledSources("main.kt")
                assertLogContainsLines(LogLevel.DEBUG, "Incremental compilation completed")
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("Removed generated callables don't conflict with user declarations with the same signatures")
    @TestMetadata("sandbox-plugin")
    fun testReplacingGeneratedCallablesWithUserDeclaredOnes(scenario: ScenarioCreator) {
        scenario {
            val module = module("sandbox-plugin", compilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
            })

            module.replaceFileWithVersion("main.kt", "user_declared_callables")
            module.compile {
                assertCompiledSources("main.kt")
                assertLogContainsLines(LogLevel.DEBUG, "Incremental compilation completed")
            }
        }
    }
}
