/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.buildtools.tests.compilation.util.compile
import org.jetbrains.kotlin.buildtools.tests.compilation.util.execute
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Contract-based smartcasts in incremental compilation")
class ContractsSmartcastTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-77395: recompiling only the usage of a contract function keeps the smartcast correct")
    @TestMetadata("ic-scenarios/kt-77395")
    fun testContractSmartcastAfterUsageOnlyRecompilationExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = module("ic-scenarios/kt-77395")

            module.execute(mainClass = "AppKt", exactOutput = "42")

            module.replaceFileWithVersion("app.kt", "touched")
            module.compile(expectedDirtySet = setOf("app.kt"))

            module.execute(mainClass = "AppKt", exactOutput = "42")
        }
    }
}
