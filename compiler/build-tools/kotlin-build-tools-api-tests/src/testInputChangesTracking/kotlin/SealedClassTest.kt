/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.junit.jupiter.api.DisplayName

class SealedClassTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Adding a sealed subclass to a multi-file sealed hierarchy triggers recompilation of the base sealed class")
    fun testAddSealedClassToSealedHierarchyHierarchy(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = module(
                "sealed-hierarchy",
            )

            module.replaceFileWithVersion("Bar.kt", "addSealedClass")

            module.compile {
                assertCompiledSources("Bar.kt", "Foo.kt")
            }
        }
    }
}
