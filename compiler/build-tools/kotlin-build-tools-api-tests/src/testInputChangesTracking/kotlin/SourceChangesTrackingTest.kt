/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_EXPLICIT_BACKING_FIELDS
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.assertAddedOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.ScenarioModule
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.assertRemovedOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class SourceChangesTrackingTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Intra-module IC tracks source changes in consecutive builds")
    @TestMetadata("jvm-module-1")
    fun testConsequentBuilds(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module1 = trackedModule("jvm-module-1")
            module1.createPredefinedFile("secret.kt", "new-file")
            module1.compile {
                assertCompiledSources("secret.kt")
                assertAddedOutputs("SecretKt.class")
            }

            // replaces bar.kt with bar.kt.1
            module1.replaceFileWithVersion("bar.kt", "add-default-argument")
            module1.deleteFile("secret.kt")

            module1.compile {
                assertCompiledSources("bar.kt")
                assertRemovedOutputs("SecretKt.class")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("IC removes the stale output when an externally-tracked source file is deleted")
    @TestMetadata("source-file-modification")
    fun testSourceFileDeletedExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            // the internally-tracked deletion path is already covered by `testConsequentBuilds`.
            val module = module("source-file-modification")

            module.deleteFile("Dummy.kt")

            module.compile {
                assertNoCompiledSources()
                assertRemovedOutputs("Dummy.class")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("IC removes the stale output and produces a new one when an internally-tracked class is renamed")
    @TestMetadata("source-file-modification")
    fun testClassRenamedInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            trackedModule("source-file-modification").renameClassAndAssertOutputs()
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("IC removes the stale output and produces a new one when an externally-tracked class is renamed")
    @TestMetadata("source-file-modification")
    fun testClassRenamedExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            module("source-file-modification").renameClassAndAssertOutputs()
        }
    }

    private fun ScenarioModule.renameClassAndAssertOutputs() {
        changeFile("Dummy.kt") { it.replace("Dummy", "ForDummies") }

        compile {
            assertCompiledSources("Dummy.kt")
            // `Dummy.class` is intentionally absent: IC must drop the stale output of the renamed class while
            // producing `ForDummies.class` and leaving the untouched `Stay.class` in place.
            assertOutputs("Stay.class", "ForDummies.class")
        }
    }

    @DisplayName("Explicit backing fields don't alter the compilability of a module")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("explicit-backing-fields-incremental-1")
    fun testExplicitBackingFieldsIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = trackedModule(
                moduleName = "explicit-backing-fields-incremental-1",
                compilationConfigAction = {
                    @OptIn(ExperimentalCompilerArgument::class)
                    it.compilerArguments[X_EXPLICIT_BACKING_FIELDS] = true
                },
            )

            // `Main.kt` won't compile even for the first time because of the second `OPT_IN_USAGE_ERROR`s.
            // Note that we'd still only see the second one, though, because of KT-63767.
            // Also note that compiling `Main.kt` will prevent the second compilation call from reusing
            // the pre-compiled `A.kt` result, hence dropping `Main.kt` during the first run.
            module.createPredefinedFile("A.kt", "1")
            module.compile {
                assertCompiledSources("A.kt")
                assertAddedOutputs("A.class", "Experimental.class")
            }
            module.createPredefinedFile("Main.kt", "1")

            module.compile {
                expectFail()
                assertCompiledSources("Main.kt")
                assertLogContainsPatterns(LogLevel.DEBUG, ".*Incremental compilation completed".toRegex())
                assertLogDoesNotContainPatterns(LogLevel.ERROR, ".*Main\\.kt:8:19 This declaration needs opt-in\\. Its usage must be marked with '@Experimental' or '@OptIn\\(Experimental::class\\)'.*".toRegex())
                assertLogContainsPatterns(LogLevel.ERROR, ".*Main\\.kt:14:19 This declaration needs opt-in\\. Its usage must be marked with '@Experimental' or '@OptIn\\(Experimental::class\\)'.*".toRegex())
            }
        }
    }
}
