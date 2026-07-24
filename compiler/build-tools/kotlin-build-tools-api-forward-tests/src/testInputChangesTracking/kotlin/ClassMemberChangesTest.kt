/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests.compilation

import org.jetbrains.kotlin.buildtools.forward.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.forward.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Class member changes in incremental compilation")
class ClassMemberChangesTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-40656: Making companion object private should recompile usages (same module)")
    @TestMetadata("ic-scenarios/kt-40656-same-module")
    fun testCompanionMadePrivateRecompilesUsagesSameModule(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/kt-40656-same-module")

            mod.replaceFileWithVersion("class1.kt", "make-private")
            mod.compile {
                expectFail()
                assertLogContainsPatterns(
                    LogLevel.ERROR,
                    ".*Cannot access 'companion object Companion : Any': it is private in 'Class1'.".toRegex()
                )
                assertCompiledSources("class1.kt", "usage.kt")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-40656: Making companion object private should recompile usages (different modules)")
    @TestMetadata("ic-scenarios/kt-40656-different-modules")
    fun testCompanionMadePrivateRecompilesUsagesDifferentModules(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("ic-scenarios/kt-40656-different-modules/lib")
            val app = module("ic-scenarios/kt-40656-different-modules/app", listOf(lib))

            lib.replaceFileWithVersion("class1.kt", "make-private")
            lib.compile()
            app.compile {
                expectFail()
                assertLogContainsPatterns(
                    LogLevel.ERROR,
                    ".*Cannot access 'companion object Companion : Any': it is private in 'Class1'.".toRegex()
                )
                assertCompiledSources("usage.kt")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-59509: Renaming a method should recompile call sites that reach it through a chain")
    @TestMetadata("ic-scenarios/kt-59509")
    fun testRenamingMethodAccessedThoughCallChainIsTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val lib = module("ic-scenarios/kt-59509/lib")
            val app = module("ic-scenarios/kt-59509/app", dependencies = listOf(lib))

            lib.replaceFileWithVersion("lib.kt", "rename-and-deprecate")
            lib.compile()
            app.compile {
                expectFail()
                assertCompiledSources("main.kt")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-62632: Raising property visibility should recompile its inlined usage")
    @TestMetadata("ic-scenarios/kt-62632")
    fun testInlineArgVisibilityChangeRecompilesCallSites(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/kt-62632")
            mod.replaceFileWithVersion("Base.kt", "change-property-visibility")
            mod.compile {
                // "Usage.kt" should also be recompiled for correct results,
                // but due to a bug, it does not recompile
                // After the fix, change it to `assertCompiledSources("Base.kt", "Usage.kt")`
                assertCompiledSources("Base.kt")
            }
        }
    }
}
