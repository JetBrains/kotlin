/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.LANGUAGE_VERSION
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.enums.KotlinVersion
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions.Companion.USE_FIR_RUNNER
import org.jetbrains.kotlin.buildtools.api.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertAddedOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertRemovedOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.moduleWithFir
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.util.*


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

            module1.compile {
                assertCompiledSources("foobar.kt")
                assertAddedOutputs("FoobarKt.class") // specify only the difference
            }

            module1.deleteFile(
                "foobar.kt",
            )

            module1.compile {
                assertNoCompiledSources()
                assertRemovedOutputs("FoobarKt.class") // specify only the difference
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
                    compilationOperationConfig = {
                        it.compilerArguments[LANGUAGE_VERSION] = KotlinVersion.V1_9
                    },
                )
            }
        }
    }

    @OptIn(ExperimentalCompilerArgument::class)
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
                    icOptionsConfigAction = {
                        it[USE_FIR_RUNNER] = true
                    },
                )
            }
        }
    }
}
