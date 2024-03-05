/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertAddedOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertNoOutputSetChanges
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertRemovedOutputs
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class ExampleIncrementalScenarioTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Sample scenario DSL IC test with a single module")
    @TestMetadata("jvm-module-1")
    fun testScenario1(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module1 = module("jvm-module-1")

            module1.createFile(
                "foobar.kt",
                //language=kt
                """
                fun foobar() {}
                """.trimIndent()
            )

            module1.compile {
                assertCompiledSources("foobar.kt")
                assertAddedOutputs("FoobarKt.class")
            }

            module1.deleteFile(
                "foobar.kt",
            )

            module1.compile {
                assertNoCompiledSources()
                assertRemovedOutputs("FoobarKt.class")
            }
        }
    }

    @DisplayName("Another sample scenario DSL IC test with a single module and custom IC options")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun testScenario2(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module1 = module("jvm-module-1", incrementalCompilationOptionsModifier = { it.keepIncrementalCompilationCachesInMemory(false) })

            module1.createFile(
                "foobar.kt",
                //language=kt
                """
                fun foobar() {}
                """.trimIndent()
            )

            module1.compile {
                assertCompiledSources("foobar.kt")
                assertAddedOutputs("FoobarKt.class")
            }

            module1.deleteFile(
                "foobar.kt",
            )

            module1.compile {
                assertNoCompiledSources()
                assertRemovedOutputs("FoobarKt.class")
            }
        }
    }

    @DisplayName("Sample scenario DSL IC test with two modules")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun testScenario3(strategyConfig: CompilerExecutionStrategyConfiguration) {
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
                assertNoOutputSetChanges()
            }

            module2.compile {
                assertCompiledSources("b.kt")
                assertNoOutputSetChanges()
            }
        }
    }
}