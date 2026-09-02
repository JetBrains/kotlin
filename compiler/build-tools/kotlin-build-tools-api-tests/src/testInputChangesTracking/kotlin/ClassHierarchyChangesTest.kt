/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.JsScenarioDsl
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.JvmScenarioDsl
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.WasmScenarioDsl
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Class hierarchy changes in incremental compilation")
class ClassHierarchyChangesTest : BaseCompilationTest() {
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-25455: Removing a supertype should recompile indirect subclasses")
    @TestMetadata("ic-scenarios/kt-25455")
    fun testRemovedSupertypeRecompilesIndirectSubclasses(scenario: ScenarioCreator) {
        scenario {
            val lib = module("ic-scenarios/kt-25455/lib")
            val app = module("ic-scenarios/kt-25455/app", dependencies = listOf(lib))

            lib.replaceFileWithVersion("B.kt", "remove-superclass")
            lib.compile { assertCompiledSources("B.kt") }
            app.compile {
                when (this@scenario) {
                    is JvmScenarioDsl -> {
                        expectFail()
                        assertCompiledSources("C.kt", "D.kt", "main.kt")
                    }
                    is JsScenarioDsl, is WasmScenarioDsl -> {
                        // TODO(KT-89004): the indirect usage `main.kt` is not recompiled across the klib dependency, so the build wrongly succeeds
                        assertCompiledSources("C.kt", "D.kt")
                    }
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-23863: Usage of extension function should be recompiled when receiver type is changed")
    @TestMetadata("ic-scenarios/kt-23863")
    fun testChangedReceiverSupertypeRecompilesUsages(scenario: ScenarioCreator) {
        scenario {
            val module = module("ic-scenarios/kt-23863")

            module.replaceFileWithVersion("Items.kt", "change-items-supertype")
            module.compile {
                assertCompiledSources("Items.kt", "Usage.kt")
            }
        }
    }
}
