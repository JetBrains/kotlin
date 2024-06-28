/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertAddedOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertNoOutputSetChanges
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertRemovedOutputs
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.util.UUID
import kotlin.random.Random

/**
 * The preferred way to write incremental compilation tests.
 */
class ExampleIncrementalScenarioTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Sample scenario DSL IC test with a single module")
    @TestMetadata("jvm-module-1")
    fun testScenario1(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module1 = module("jvm-module-1")
            // at this moment, the module is already initially built and ready for further incremental compilations

            val randomString = UUID.randomUUID().toString()
            // Use this overload to create file with some dynamic content
            module1.createFile(
                "foobar.kt",
                //language=kt
                """
                fun foobar() {
                    println("$randomString")
                }
                """.trimIndent()
            )

            module1.compile { module, scenarioModule ->
                assertCompiledSources(module, "foobar.kt")
                assertAddedOutputs(module, scenarioModule, "FoobarKt.class") // specify only the difference
            }

            module1.deleteFile(
                "foobar.kt",
            )

            module1.compile { module, scenarioModule ->
                assertNoCompiledSources(module)
                assertRemovedOutputs(module, scenarioModule, "FoobarKt.class") // specify only the difference
            }
        }
    }

    @DisplayName("Another sample scenario DSL IC test with a single module and custom IC options")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("jvm-module-1")
    fun testScenario2(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            // compilation options may be modified
            val module1 = module("jvm-module-1", incrementalCompilationOptionsModifier = { it.keepIncrementalCompilationCachesInMemory(false) })

            module1.createFile(
                "foobar.kt",
                //language=kt
                """
                fun foobar() {}
                """.trimIndent()
            )

            module1.compile { module, scenarioModule ->
                assertCompiledSources(module, "foobar.kt")
                assertAddedOutputs(module, scenarioModule, "FoobarKt.class")
            }

            module1.deleteFile(
                "foobar.kt",
            )

            module1.compile { module, scenarioModule ->
                assertNoCompiledSources(module)
                assertRemovedOutputs(module, scenarioModule, "FoobarKt.class")
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

            val randomInt = Random.nextInt()
            // Use this overload to modify file dynamically
            module1.changeFile(
                "bar.kt",
                transform = {
                    //language=kt
                    it.replace("fun bar()", "fun bar(someNumber: Int = $randomInt)")
                }
            )

            // you should handle the right order of compilation between modules yourself
            module1.compile { module, scenarioModule ->
                assertCompiledSources(module, "bar.kt")
                assertNoOutputSetChanges(module, scenarioModule)
            }

            module2.compile { module, scenarioModule ->
                assertCompiledSources(module, "b.kt")
                assertNoOutputSetChanges(module, scenarioModule)
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Sample scenario DSL IC test with versioned source file modification")
    @TestMetadata("jvm-module-1")
    fun testScenario4(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module1 = module("jvm-module-1")

            // replaces bar.kt with bar.kt.1
            module1.replaceFileWithVersion("bar.kt", "add-default-argument")

            module1.compile { module, scenarioModule ->
                assertCompiledSources(module, "bar.kt")
                assertNoOutputSetChanges(module, scenarioModule)
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Sample scenario DSL IC test with versioned source file creation")
    @TestMetadata("jvm-module-1")
    fun testScenario5(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module1 = module("jvm-module-1")

            // creates secret.kt from secret.kt.1
            module1.createPredefinedFile("secret.kt", "new-file")

            module1.compile { module, scenarioModule ->
                assertCompiledSources(module, "secret.kt")
                assertAddedOutputs(module, scenarioModule, "SecretKt.class")
            }
        }
    }
}