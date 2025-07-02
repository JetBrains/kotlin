/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.SnapshotConfig
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.compile
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class ClasspathSnapshottingTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Class abiHash sensitivity in presence of a lambda - happy version")
    @TestMetadata("empty")
    fun testMainCase(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("empty")
            val app = module(
                "empty2",
                dependencies = listOf(lib),
                snapshotConfig = SnapshotConfig(
                    ClassSnapshotGranularity.CLASS_LEVEL,
                    useInlineLambdaSnapshotting = false //TODO if fixed, test with both flags
                )
            )

            app.createFile(
                "callSite.kt",
                """
                    val result = Calc().calculate()

                    fun main(args: Array<String>) {
                        println(result)
                    }
                """.trimIndent()
            )

            lib.createFile(
                "calc.kt",
                """
                    class Calc {
                        fun problemFun() {
                            val casualLambdaToMakeSnapshotsJumpy = { 321 }
                        }
                    
                        fun calculate(): Int {
                            return 123
                        }
                    }
                """.trimIndent()
            )

            lib.compile()
            app.compile()

            lib.changeFile("calc.kt") {
                it.replace(
                    "return 123",
                    """
                        val unused = 42
                        return 1234
                    """.trimIndent()
                )
            }

            lib.compile(expectedDirtySet = setOf("calc.kt"))
            app.compile(expectedDirtySet = emptySet())
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Class abiHash sensitivity in presence of a lambda - unhappy version")
    @TestMetadata("empty")
    fun testMainCaseWithCrossinline(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("empty")
            val app = module(
                "empty2",
                dependencies = listOf(lib),
                snapshotConfig = SnapshotConfig(
                    ClassSnapshotGranularity.CLASS_LEVEL,
                    useInlineLambdaSnapshotting = false //TODO if fixed, test with both flags
                )
            )

            app.createFile(
                "callSite.kt",
                """
                    val result = Calc().calculate()

                    fun main(args: Array<String>) {
                        println(result)
                    }
                """.trimIndent()
            )

            lib.createFile(
                "calc.kt",
                """
                    class Calc {
                        fun problemFun() {
                            // note that forEach is not actually marked as crossinline
                            listOf<String>().forEach { it ->
                                // do nothing
                            }
                        }
                    
                        fun calculate(): Int {
                            return 123
                        }
                    }
                """.trimIndent()
            )

            lib.compile()
            app.compile()

            lib.changeFile("calc.kt") {
                it.replace(
                    "return 123",
                    """
                        val unused = 42
                        return 1234
                    """.trimIndent()
                )
            }

            lib.compile(expectedDirtySet = setOf("calc.kt"))
            app.compile(expectedDirtySet = emptySet())
        }
    }
}
