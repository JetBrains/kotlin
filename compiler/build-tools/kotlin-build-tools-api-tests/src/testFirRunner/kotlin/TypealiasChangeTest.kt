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


@DisplayName("Single module IC scenarios with typealiases")
class TypealiasChangeTest : BaseCompilationTest() {

    @Disabled("Broken, KT-28233")
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Potential first-round errors: typealias change")
    @TestMetadata("empty")
    fun testTypealiasChange(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val module = module(
                "empty",
                additionalCompilationArguments = listOf("-Xuse-fir-ic"),
                incrementalCompilationOptionsModifier = { incrementalOptions ->
                    (incrementalOptions as ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration).useFirRunner(true)
                }
            )

            module.createFile(
                "Foo.kt",
                """
                    interface Foo {
                        val values: Type
                    }
                """.trimIndent()
            )

            module.createFile(
                "FooImpl.kt",
                """
                    class FooImpl : Foo {
                        override val values: Type
                            get() = "0"
                    }
                """.trimIndent()
            )

            module.createFile(
                "types.kt",
                """
                    typealias Type = String
                """.trimIndent()
            )

            module.compile { module, scenarioModule ->
                assertCompiledSources(module, "Foo.kt", "FooImpl.kt", "types.kt")
            }

            module.changeFile("types.kt") { contents -> contents.replace("String", "Int") }
            module.changeFile("FooImpl.kt") { contents -> contents.replace("\"0\"", "0") }

            module.compile { module, scenarioModule ->
                assertCompiledSources(module, "Foo.kt", "FooImpl.kt", "types.kt")
            }
        }
    }
}
