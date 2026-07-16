/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.compilation

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.X_EXPLICIT_BACKING_FIELDS
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.forward.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.scenario.ScenarioModule
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.scenario.assertAddedOutputs
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.scenario.assertNoOutputSetChanges
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.scenario.assertRemovedOutputs
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class SourceChangesTrackingTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Intra-module IC tracks source changes in consecutive builds")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testConsequentBuilds(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module1 = trackedModule("basic-multimodule-project/module-1")
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

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-13204: IC resolves typealias correctly after visibility change (externally tracked)")
    @TestMetadata("class-visibility-change")
    fun testClassVisibilityChangeRecompilesDirectUsersExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            module("class-visibility-change").changeClassVisibilityAndAssertDirtySet()
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-13204: IC resolves typealias correctly after visibility change (internally tracked)")
    @TestMetadata("class-visibility-change")
    fun testClassVisibilityChangeRecompilesDirectUsersInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            trackedModule("class-visibility-change").changeClassVisibilityAndAssertDirtySet()
        }
    }

    private fun ScenarioModule.changeClassVisibilityAndAssertDirtySet() {
        // KT-13204: flip `Curry` to `internal`; `UseCurry.kt` references it via typealias FN2, `Dummy.kt` does not.
        replaceFileWithVersion("Curry.kt", "internal")

        compile {
            // `Dummy.kt` is intentionally absent: only the changed class and its direct user recompile.
            assertCompiledSources("Curry.kt", "UseCurry.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-69042: IC recompiles the Kotlin usage when an inlined Java constant changes (externally tracked)")
    @TestMetadata("kotlin-java-constant")
    fun testKotlinTracksJavaConstantChangeExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            module("kotlin-java-constant").changeJavaConstantAndAssertUsageRecompiled()
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-69042: IC recompiles the Kotlin usage when an inlined Java constant changes (internally tracked)")
    @TestMetadata("kotlin-java-constant")
    fun testKotlinTracksJavaConstantChangeInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            trackedModule("kotlin-java-constant").changeJavaConstantAndAssertUsageRecompiled()
        }
    }

    private fun ScenarioModule.changeJavaConstantAndAssertUsageRecompiled() {
        replaceFileWithVersion("JavaConstants.java", "new-value")

        compile {
            // Regression test for KT-69042: under K2 a changed Java constant must recompile the Kotlin
            // file that reads it, otherwise the stale value stays inlined in the usage.
            assertCompiledSources("usage.kt")
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

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-49023: Renaming a file with a case-only change should not cause redeclaration errors")
    @TestMetadata("ic-scenarios/kt-49023")
    fun testCaseOnlyFileRenameDoesNotCauseRedeclarationErrors(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/kt-49023")

            mod.deleteFile("Foo.kt")
            mod.createPredefinedFile("foo.kt", "different-case")

            mod.compile {
                assertCompiledSources("foo.kt")
                assertNoOutputSetChanges()
            }
        }
    }

    @DisplayName("Irrelevant ABI change in dependency does not cause any recompilation")
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @TestMetadata("basic-multimodule-project")
    fun testUnusedAbiChange(scenario: ScenarioCreator) {
        scenario {
            val module1 = module("basic-multimodule-project/module-1")
            val module2 = module("basic-multimodule-project/module-2", listOf(module1))

            module1.createPredefinedFile("secret.kt", "new-file")
            module1.compile {
                assertCompiledSources("secret.kt")
            }
            module2.compile {
                assertNoCompiledSources()
                assertNoOutputSetChanges()
            }
        }
    }

    @DisplayName("Mixed source + dependency change recompiles expected files")
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @TestMetadata("basic-multimodule-project")
    fun testMixedSourceAndDependencyChange(scenario: ScenarioCreator) {
        scenario {
            val module1 = module("basic-multimodule-project/module-1")
            val module2 = module("basic-multimodule-project/module-2", listOf(module1))

            module1.replaceFileWithVersion("bar.kt", "add-default-argument")
            module2.replaceFileWithVersion("a.kt", "change-value")

            module1.compile {
                assertCompiledSources("bar.kt")
            }
            module2.compile {
                assertCompiledSources("a.kt", "b.kt")
            }
        }
    }
}
