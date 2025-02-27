/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName


@DisplayName("Single module IC scenarios with type dependencies")
class TypeResolutionCyclesTest : BaseCompilationTest() {

    @Disabled("Broken, KT-58824")
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Potential first-round errors: cyclic dependency created")
    @TestMetadata("empty")
    fun testCyclicDependencyCreated(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module = module(
                "empty",
                additionalCompilationArguments = listOf("-Xuse-fir-ic"),
                incrementalCompilationOptionsModifier = { incrementalOptions ->
                    (incrementalOptions as ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration).useFirRunner(true)
                }
            )

            module.createFile(
                "File1.kt",
                """
                    fun f1() = f0()
                """.trimIndent()
            )

            module.createFile(
                "File2.kt",
                """
                    fun f0() = 10
                    fun f2() = f1() + 1
                """.trimIndent()
            )

            module.compile { module, scenarioModule ->
                assertCompiledSources(module, "File1.kt", "File2.kt")
            }

            module.changeFile("File2.kt") { contents ->
                """
                    fun f0() = "10"
                    fun f2() = f1() + "1"
                """.trimIndent()
            }

            module.compile { module, scenarioModule ->
                assertCompiledSources(module, "File1.kt", "File2.kt")
            }
        }
    }
}
