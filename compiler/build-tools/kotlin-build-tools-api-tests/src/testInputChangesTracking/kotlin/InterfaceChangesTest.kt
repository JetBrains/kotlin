/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.JsScenarioDsl
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.JvmScenarioDsl
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.WasmScenarioDsl
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Interface ABI changes in incremental compilation")
class InterfaceChangesTest : BaseCompilationTest() {
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-72768: Adding a default method to interface should recompile anonymous implementors used in the same file")
    @TestMetadata("ic-scenarios/kt-72768")
    fun testAddingDefaultMethodRecompilesAnonymousImplementorsUsedInSameFile(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/kt-72768")

            mod.replaceFileWithVersion("A.kt", "add-default-method")
            mod.compile {
                when (this@scenario) {
                    is JvmScenarioDsl -> assertCompiledSources("A.kt", "B.kt")
                    is JsScenarioDsl, is WasmScenarioDsl -> {
                        // TODO(KT-89011): the anonymous implementor `B.kt` is not rebuilt, only the changed interface file `A.kt` is
                        assertCompiledSources("A.kt")
                    }
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-53854: Adding a default method to interface should recompile anonymous implementors used in a different file")
    @TestMetadata("ic-scenarios/kt-53854")
    fun testAddingDefaultMethodRecompilesAnonymousImplementorsUsedInDifferentFile(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/kt-53854")

            mod.replaceFileWithVersion("i.kt", "add-default-method")
            mod.compile {
                when (this@scenario) {
                    is JvmScenarioDsl -> assertCompiledSources("i.kt", "main.kt")
                    is JsScenarioDsl, is WasmScenarioDsl -> {
                        // TODO(KT-89011): the anonymous implementor in `main.kt` is not rebuilt, only the changed interface file `i.kt` is
                        assertCompiledSources("i.kt")
                    }
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-46819: Adding abstract method to interface should recompile object-inheritors")
    @TestMetadata("ic-scenarios/kt-46819")
    fun testAddingAbstractMethodRecompilesObjectInheritor(scenario: ScenarioCreator) {
        scenario {
            val lib = module("ic-scenarios/kt-46819/module-lib")
            val app = module("ic-scenarios/kt-46819/module-app", dependencies = listOf(lib))

            lib.replaceFileWithVersion("iface.kt", "add-abstract-method")
            lib.compile { assertCompiledSources("iface.kt") }
            app.compile {
                when (this@scenario) {
                    is JvmScenarioDsl -> {
                        // Correct: `impl.kt` is recompiled and the build fails, like a clean build.
                        expectFail()
                        assertCompiledSources("impl.kt")
                    }
                    is JsScenarioDsl, is WasmScenarioDsl -> {
                        // TODO(KT-89011): the object implementor `impl.kt` is not recompiled across the klib dependency, so the build wrongly succeeds
                        assertNoCompiledSources()
                    }
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
            }
        }
    }
}
