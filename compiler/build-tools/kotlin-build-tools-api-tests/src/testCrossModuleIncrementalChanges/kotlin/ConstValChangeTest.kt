/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.Scenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class ConstValChangeTest : BaseCompilationTest() {

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("Changing a const val in a dependency recompiles its usages in the dependent module")
    @TestMetadata("ic-scenarios/const-val-change/lib")
    fun testConstValChangeRecompilesUsages(scenario: ScenarioCreator) {
        scenario {
            checkConstValChangeRecompilesUsages()
        }
    }

    private fun Scenario<*, *>.checkConstValChangeRecompilesUsages() {
        val lib = module("ic-scenarios/const-val-change/lib")
        val app = module(
            "ic-scenarios/const-val-change/app",
            dependencies = listOf(lib),
        )

        lib.replaceFileWithVersion("constants.kt", "change-value")

        lib.compile {
            assertCompiledSources("constants.kt")
        }
        app.compile {
            assertCompiledSources("useConstant.kt")
        }
    }
}
