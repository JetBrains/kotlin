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

@DisplayName("Class hierarchy changes in incremental compilation")
class ClassHierarchyChangesTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-25455: Removing a supertype should recompile indirect subclasses")
    @TestMetadata("ic-scenarios/kt-25455")
    fun testRemovedSupertypeRecompilesIndirectSubclasses(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("ic-scenarios/kt-25455/lib")
            val app = module("ic-scenarios/kt-25455/app", dependencies = listOf(lib))

            lib.replaceFileWithVersion("B.kt", "remove-superclass")
            lib.compile { assertCompiledSources("B.kt") }
            app.compile {
                expectFail()
                assertCompiledSources("C.kt", "D.kt", "main.kt")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-23863: Usage of extension function should be recompiled when receiver type is changed")
    @TestMetadata("ic-scenarios/kt-23863")
    fun testChangedReceiverSupertypeRecompilesUsages(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = module("ic-scenarios/kt-23863")

            module.replaceFileWithVersion("Items.kt", "change-items-supertype")
            module.compile {
                assertCompiledSources("Items.kt", "Usage.kt")
            }
        }
    }
}
