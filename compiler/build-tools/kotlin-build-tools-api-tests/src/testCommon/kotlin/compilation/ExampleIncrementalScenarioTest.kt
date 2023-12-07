/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName

@Disabled("Example tests for evaluation purposes of the DSL")
class ExampleIncrementalScenarioTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Sample scenario DSL IC test with a single module")
    fun scenario1Test(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module1 = module("jvm-module-1")

            module1.createFile(
                "foobar.kt",
                addedOutputs = setOf("FoobarKt.class"),
                //language=kt
                """
                fun foobar() {}
                """.trimIndent()
            )

            module1.compile {
                assertCompiledSources("foobar.kt")
            }

            module1.deleteFile(
                "foobar.kt",
                removedOutputs = setOf("FoobarKt.class"),
            )

            module1.compile {
                assertCompiledSources()
            }
        }
    }

    @DisplayName("Another sample scenario DSL IC test with a single module and custom IC options")
    @DefaultStrategyAgnosticCompilationTest
    fun scenario2Test(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module1 = module("jvm-module-1", incrementalCompilationOptionsModifier = { it.keepIncrementalCompilationCachesInMemory(false) })

            module1.createFile(
                "foobar.kt",
                addedOutputs = setOf("FoobarKt.class"),
                //language=kt
                """
                fun foobar() {}
                """.trimIndent()
            )

            module1.compile {
                assertCompiledSources("foobar.kt")
            }

            module1.deleteFile(
                "foobar.kt",
                removedOutputs = setOf("FoobarKt.class"),
            )

            module1.compile {
                assertCompiledSources()
            }
        }
    }

    @DisplayName("Sample scenario DSL IC test with two modules")
    @DefaultStrategyAgnosticCompilationTest
    fun scenario3Test(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module1 = module("jvm-module-1")
            val module2 = module("jvm-module-2", listOf(module1))

            module1.changeFile(
                "bar.kt",
                transform = {
                    //language=kt
                    it.replace("fun bar()", "fun bar(someNumber: Int = 50)")
                }
            )

            module1.compile {
                assertCompiledSources("bar.kt")
            }

            module2.compile {
                assertCompiledSources("b.kt")
            }
        }
    }
}