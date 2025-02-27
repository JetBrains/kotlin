/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.*
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.*
import org.junit.jupiter.api.DisplayName
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.assertThrows
import java.util.UUID


@DisplayName("Single module IC scenarios for FIR runner")
class SingleModuleFirRunnerIncrementalTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding and removing the class")
    @TestMetadata("jvm-module-1")
    fun testScenario1(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module1 = moduleWithFir("jvm-module-1")

            val randomString = UUID.randomUUID().toString()
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

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Throws an exception on using LV 1.9")
    @TestMetadata("jvm-module-1")
    fun testScenario2(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            assertThrows<IllegalStateException>(
                message = "Compilation does not fail on LV 1.9"
            ) {
                // Throws on initial compilation
                moduleWithFir(
                    moduleName = "jvm-module-1",
                    additionalCompilerArguments = listOf("-language-version=1.9")
                )
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Throws an exception on missing -Xuse-fir-ic")
    @TestMetadata("jvm-module-1")
    fun testScenario3(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            assertThrows<IllegalStateException>(
                message = "Compilation does not fail on missing -Xuse-fir-ic"
            ) {
                // Throws on initial compilation
                module(
                    moduleName = "jvm-module-1",
                    incrementalCompilationOptionsModifier = { incrementalOptions ->
                        (incrementalOptions as ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration).useFirRunner(true)
                    }
                )
            }
        }
    }

    private fun Scenario.moduleWithFir(
        moduleName: String,
        additionalCompilerArguments: List<String> = emptyList()
    ) = module(
        moduleName = moduleName,
        additionalCompilationArguments = additionalCompilerArguments + listOf("-Xuse-fir-ic"),
        incrementalCompilationOptionsModifier = { incrementalOptions ->
            (incrementalOptions as ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration).useFirRunner(true)
        }
    )
}
