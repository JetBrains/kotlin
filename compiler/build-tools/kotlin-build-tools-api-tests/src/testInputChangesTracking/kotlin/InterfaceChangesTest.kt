/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Interface ABI changes in incremental compilation")
class InterfaceChangesTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-72768: Adding a default method to interface should recompile anonymous implementors used in the same file")
    @TestMetadata("ic-scenarios/kt-72768")
    fun testAddingDefaultMethodRecompilesAnonymousImplementorsUsedInSameFile(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/kt-72768")

            mod.replaceFileWithVersion("A.kt", "add-default-method")
            mod.compile {
                assertCompiledSources("A.kt", "B.kt")
            }
        }
    }
}
