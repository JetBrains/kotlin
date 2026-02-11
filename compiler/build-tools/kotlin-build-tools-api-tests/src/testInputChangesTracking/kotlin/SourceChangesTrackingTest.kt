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
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.assertAddedOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.assertRemovedOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class SourceChangesTrackingTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Intra-module IC tracks source changes in consecutive builds")
    @TestMetadata("jvm-module-1")
    fun testConsequentBuilds(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
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

    @DisplayName("Explicit backing fields don't alter the compilability of a module")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("explicit-backing-fields-incremental-1")
    fun testExplicitBackingFieldsIncremental(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module = trackedModule(
                moduleName = "explicit-backing-fields-incremental-1",
                compilationConfigAction = {
                    @OptIn(ExperimentalCompilerArgument::class)
                    it.compilerArguments[X_EXPLICIT_BACKING_FIELDS] = true
                },
            )

            // Ensure no `OPT_IN_USAGE_ERROR`s are produced
            module.createPredefinedFile("A.kt", "1")
            module.createPredefinedFile("Main.kt", "1")
            module.compile {
                assertCompiledSources("A.kt", "Main.kt")
                assertAddedOutputs(
                    "A.class", "Experimental.class",
                    "MainKt.class", $$"MainKt$main$1.class", $$"I$DefaultImpls.class", "I.class", $$"MainKt$main$2.class",
                )
            }
            module.changeFile("Main.kt") { "$it // DUMMY_CHANGE" }

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