/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.compilation

import org.jetbrains.kotlin.buildtools.forward.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.scenario.jvmScenario
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

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-53854: Adding a default method to interface should recompile anonymous implementors used in a different file")
    @TestMetadata("ic-scenarios/kt-53854")
    fun testAddingDefaultMethodRecompilesAnonymousImplementorsUsedInDifferentFile(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/kt-53854")

            mod.replaceFileWithVersion("i.kt", "add-default-method")
            mod.compile {
                assertCompiledSources("i.kt", "main.kt")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-46819: Adding abstract method to interface should recompile object-inheritors")
    @TestMetadata("ic-scenarios/kt-46819")
    fun testAddingAbstractMethodRecompilesObjectInheritor(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("ic-scenarios/kt-46819/module-lib")
            val app = module("ic-scenarios/kt-46819/module-app", dependencies = listOf(lib))

            lib.replaceFileWithVersion("iface.kt", "add-abstract-method")
            lib.compile { assertCompiledSources("iface.kt") }
            app.compile {
                expectFail()
                assertCompiledSources("impl.kt")
            }
        }
    }
}
