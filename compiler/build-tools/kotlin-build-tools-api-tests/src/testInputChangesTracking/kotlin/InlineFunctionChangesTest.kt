/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertExactOutput
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputFileBytesContain
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

// Debug info is a part of the inlined code, so the test data is sensitive to changes in line numbers.
@DisplayName("Inline function changes in incremental compilation")
class InlineFunctionChangesTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-89522: Changing a default value of an inline function parameter should recompile its call sites")
    @TestMetadata("ic-scenarios/inline-fun-default-value")
    fun testChangingDefaultValueOfInlineFunction(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/inline-fun-default-value")

            mod.replaceFileWithVersion("a.kt", "new-default")

            mod.compile {
                // TODO(KT-89522): only `a.kt` is recompiled, so `b.kt` keeps the inlined old default value
                assertCompiledSources("a.kt", "b.kt")
            }
            mod.executeCompiledCode("BKt") {
                assertExactOutput("new")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-89649: Changing an anonymous object capturing a crossinline lambda should recompile the call sites of its inline function")
    @TestMetadata("ic-scenarios/inline-fun-anonymous-object")
    fun testChangingAnonymousObjectInInlineFunction(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/inline-fun-anonymous-object")

            mod.replaceFileWithVersion("a.kt", "changed-object-body")

            mod.compile {
                // TODO(KT-89649): only `a.kt` is recompiled, so the copy of the object regenerated in `b.kt` keeps the old body
                assertCompiledSources("a.kt", "b.kt")
            }
            mod.executeCompiledCode("BKt") {
                assertExactOutput("o2!")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-89613: Changing an anonymous object using a reified type parameter should recompile the call sites of its inline function")
    @TestMetadata("ic-scenarios/inline-fun-reified-object")
    fun testChangingAnonymousObjectUsingReifiedTypeParameter(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/inline-fun-reified-object")

            mod.replaceFileWithVersion("a.kt", "changed-object-body")

            mod.compile {
                // TODO(KT-89613): only `a.kt` is recompiled, so the copy of the object reified in `b.kt` keeps the old body
                assertCompiledSources("a.kt", "b.kt")
            }
            mod.executeCompiledCode("BKt") {
                assertExactOutput("o2 Int")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Making the file of an inline function longer should update the SMAP of the anonymous object copies at its call sites")
    @TestMetadata("ic-scenarios/inline-fun-anonymous-object")
    fun testAppendingDeclarationAfterInlineFunctionWithAnonymousObject(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/inline-fun-anonymous-object")

            mod.replaceFileWithVersion("a.kt", "appended-function")

            mod.compile {
                // TODO(KT-89649, KT-89613): only `a.kt` is recompiled, while the SMAP of the object copy in `b.kt` covers all lines of `a.kt`
                assertCompiledSources("a.kt", "b.kt")
                // the stale copy maps the lines `1#1,6:1`
                assertOutputFileBytesContain($$$"BKt$main$$inlined$f$1.class", "*L\n1#1,8:1\n")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName($$$"Changing only the $$forInline template of a suspend inline function should recompile the suspend inline functions calling it")
    @TestMetadata("ic-scenarios/suspend-inline-fun-template")
    fun testChangingForInlineTemplateOfSuspendInlineFunction(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/suspend-inline-fun-template")

            mod.replaceFileWithVersion("a.kt", "changed-dead-branch")

            mod.compile {
                // IC hashes only the optimized `f`, where the dead branch is removed, but `g$$forInline` in `b.kt` inlines the unoptimized
                // `f$$forInline` template, which keeps the dead branch
                assertCompiledSources("a.kt", "b.kt")
                assertOutputFileBytesContain("BKt.class", "dead2")
            }
        }
    }
}
