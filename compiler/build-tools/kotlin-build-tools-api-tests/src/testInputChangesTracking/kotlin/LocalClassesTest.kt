/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoWarnings
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputFileContains
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.JsScenarioDsl
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.JvmScenarioDsl
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.WasmScenarioDsl
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class LocalClassesTest : BaseCompilationTest() {
    @DisplayName("Changing super type of local class should trigger rebuild of the local class")
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @TestMetadata("ic-scenarios/KT-85074")
    fun localClassSuperTypeChange(scenario: ScenarioCreator) {
        scenario {
            val module = module("ic-scenarios/KT-85074")

            module.replaceFileWithVersion("Foo.kt", "addVal")

            module.compile {
                when (this@scenario) {
                    is JvmScenarioDsl -> {
                        // Correct: `FooImpl.kt` is recompiled and the build fails, like a clean build.
                        expectFail()
                        assertLogContainsPatterns(
                            LogLevel.ERROR,
                            ".*Class '<anonymous>' is not abstract and does not implement abstract member:\nval bar: String".toRegex()
                        )
                    }
                    is JsScenarioDsl, is WasmScenarioDsl -> {
                        // TODO(KT-88963): `FooImpl.kt` is not rebuilt, so the build wrongly succeeds
                        assertCompiledSources("Foo.kt")
                    }
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
            }
        }
    }

    @DisplayName("Changing local class should not trigger rebuild of usages")
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @TestMetadata("ic-scenarios/local-class-change")
    fun localClassChange(scenario: ScenarioCreator) {
        scenario {
            val module = module("ic-scenarios/local-class-change")

            module.replaceFileWithVersion("Foo.kt", "changeLocalClass")

            module.compile {
                assertCompiledSources("Foo.kt")
            }
        }
    }

    @DisplayName("KT-61023: Removing and adding methods or properties to a delegated interface should not produce invalid binaries")
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @TestMetadata("ic-scenarios/kt-61023")
    fun testInterfaceMemberChangeIsPropagatedToDelegate(scenario: ScenarioCreator) {
        scenario {
            val module = module("ic-scenarios/kt-61023")

            module.replaceFileWithVersion("f1.kt", "remove-value")
            module.replaceFileWithVersion("main.kt", "remove-value")

            module.compile {
                val expectedSources = when (this@scenario) {
                    is JvmScenarioDsl -> setOf("f1.kt", "main.kt", "f2.kt")
                    is JsScenarioDsl, is WasmScenarioDsl -> setOf("f1.kt", "main.kt")
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
                assertCompiledSources(expectedSources)
            }

            module.replaceFileWithVersion("f2.kt", "add-empty-line")

            module.compile {
                assertCompiledSources("f2.kt")
            }

            module.replaceFileWithVersion("f1.kt", "original")
            module.replaceFileWithVersion("main.kt", "original")

            module.compile {
                val expectedSources = when (this@scenario) {
                    is JvmScenarioDsl -> setOf("f1.kt", "main.kt", "f2.kt")
                    is JsScenarioDsl, is WasmScenarioDsl -> setOf("f1.kt", "main.kt")
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
                assertCompiledSources(expectedSources)
            }
            module.link {
                val fileName = when (this@scenario) {
                    is JsScenarioDsl -> "ic-scenarios_kt-61023.js"
                    is WasmScenarioDsl -> "ic-scenarios_kt-61023.wasm"
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
                // Actually, it should not contain this, but due to the bug it's there.
                // After non-incremental compilation, it's not there.
                assertOutputFileContains(
                    fileName,
                    "Abstract property accessor 'getValue.<get-getValue>' is not implemented in non-abstract anonymous object"
                )
            }
        }
    }

    @DisplayName("KT-59153: Modifying the signature of an interface method with a default implementation produces correct bytecode in anonymous inheritors")
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @TestMetadata("ic-scenarios/kt-59153")
    fun testDefaultInterfaceMethodSignatureChangeProducesCorrectBytecodeInInheritors(scenario: ScenarioCreator) {
        scenario {
            val module = module("ic-scenarios/kt-59153")

            module.replaceFileWithVersion("f1.kt", "add-receiver")

            module.compile {
                val expectedSources = buildSet {
                    add("f1.kt")
                    add("main.kt")
                    if (this@scenario is JvmScenarioDsl) {
                        add("f2.kt")
                    }
                }
                assertCompiledSources(expectedSources)
            }

            module.link {
                assertNoWarnings()
            }
        }
    }
}
