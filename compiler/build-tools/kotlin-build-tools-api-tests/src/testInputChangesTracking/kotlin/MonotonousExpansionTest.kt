/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompilationSteps
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class MonotonousExpansionTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("KT-29860 KT-55940 Circular dependency does not lead to infinite IC recursion")
    @TestMetadata("jvm-circular-dependency")
    fun circularDependency(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module = module("jvm-circular-dependency")
            // MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION is enabled by default

            module.replaceFileWithVersion("b.kt", "addCircularDependencyOnA")

            module.compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, ".*Type checking has run into a recursive problem.*".toRegex())
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Inline change with monotonous expansion compiles successfully and includes previously compiled files")
    @TestMetadata("jvm-circular-dependency")
    fun inlineChangeWithExpansion(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module = module("jvm-circular-dependency")

            module.replaceFileWithVersion("b.kt", "justChange")

            module.compile {
                assertCompilationSteps(
                    setOf("b.kt"),
                    setOf("a.kt", "b.kt"),
                )
            }
        }
    }

    @OptIn(ExperimentalCompilerArgument::class)
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Inline change without monotonous expansion recompiles only affected files")
    @TestMetadata("jvm-circular-dependency")
    fun inlineChangeWithoutExpansion(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module = module("jvm-circular-dependency", icOptionsConfigAction = {
                it[MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION] = false
            })

            module.replaceFileWithVersion("b.kt", "justChange")

            module.compile {
                assertCompilationSteps(
                    setOf("b.kt"),
                    setOf("a.kt"),
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Non-inline ABI change with monotonous expansion includes previously compiled files")
    @TestMetadata("jvm-non-inline-expansion")
    fun nonInlineAbiChangeWithExpansion(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module = module("jvm-non-inline-expansion")

            module.replaceFileWithVersion("b.kt", "addOverload")

            module.compile {
                assertCompilationSteps(
                    setOf("b.kt"),
                    setOf("a.kt", "b.kt"),
                )
            }
        }
    }

    @OptIn(ExperimentalCompilerArgument::class)
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Non-inline ABI change without monotonous expansion recompiles only affected files")
    @TestMetadata("jvm-non-inline-expansion")
    fun nonInlineAbiChangeWithoutExpansion(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module = module("jvm-non-inline-expansion", icOptionsConfigAction = {
                it[MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION] = false
            })

            module.replaceFileWithVersion("b.kt", "addOverload")

            module.compile {
                assertCompilationSteps(
                    setOf("b.kt"),
                    setOf("a.kt"),
                )
            }
        }
    }
}
