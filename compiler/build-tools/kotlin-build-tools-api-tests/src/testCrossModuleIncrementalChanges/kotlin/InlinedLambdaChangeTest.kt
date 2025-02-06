/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompletion
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertExactOutput
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertFailure
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

//TODO there was some heavy junie usage, check descriptions and comments at cleanup stage
class InlinedLambdaChangeTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("When inlined lambda's body changes, its call site is recompiled")
    @TestMetadata("ic-scenarios/inline-local-class/status-quo/lib")
    //TODO rename testdata from status-quo to ???
    fun testMainCase(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/status-quo/lib")
            val app = module("ic-scenarios/inline-local-class/status-quo/app", dependencies = listOf(lib))

            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }

            lib.replaceFileWithVersion("inlinedLambda.kt", "changeLambdaBody")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedLambda.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, "callSite.kt")
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(WITH_NEW_LAMBDA_BODY)
            }

            lib.replaceFileWithVersion("inlinedLambda.kt", "changeFunctionBody")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedLambda.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, "callSite.kt")
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(WITH_BOTH_CHANGES)
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Single-module version of inlined lambda changes")
    @TestMetadata("ic-scenarios/inline-local-class/single-module/app")
    fun testSingleModule(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val app = module("ic-scenarios/inline-local-class/single-module/app")

            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }

            app.replaceFileWithVersion("inlinedLambda.kt", "changeLambdaBody")

            app.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedLambda.kt", "callSite.kt")
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(WITH_NEW_LAMBDA_BODY)
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes in local class inside inlined local class")
    @TestMetadata("ic-scenarios/inline-local-class/local-in-local/lib")
    fun testLocalClassInLocal(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/local-in-local/lib")
            val app = module("ic-scenarios/inline-local-class/local-in-local/app", dependencies = listOf(lib))

            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }

            lib.replaceFileWithVersion("inlinedLocalClass.kt", "changeInnerComputation")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedLocalClass.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, "callSite.kt")
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(LOCAL_CLASS_CHANGED)
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes in unused code should not trigger recompilation of call site")
    @TestMetadata("ic-scenarios/inline-local-class/no-recompile/lib")
    fun testNoRecompilationNeeded(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/no-recompile/lib")
            val app = module("ic-scenarios/inline-local-class/no-recompile/app", dependencies = listOf(lib))

            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }

            lib.replaceFileWithVersion("inlinedFunction.kt", "changeUnusedCode")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedFunction.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module)  // No files should be recompiled
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)  // Output should remain the same
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes in lambda inside inline function B affect call site of inline function A that calls B")
    @TestMetadata("ic-scenarios/inline-local-class/nested-inline/lib")
    fun testNestedInlineFunctions(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/nested-inline/lib")
            val app = module("ic-scenarios/inline-local-class/nested-inline/app", dependencies = listOf(lib))

            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }

            lib.replaceFileWithVersion("inlinedB.kt", "changeLambdaInB")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedB.kt", "inlinedA.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, "callSite.kt")  // Call site of A should be recompiled
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(NESTED_LAMBDA_CHANGED)
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes in lambda inside inline function in anonymous class")
    @TestMetadata("ic-scenarios/inline-local-class/anonymous/lib")
    fun testAnonymousClass(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/anonymous/lib")
            val app = module("ic-scenarios/inline-local-class/anonymous/app", dependencies = listOf(lib))

            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }

            lib.replaceFileWithVersion("inlinedAnonymous.kt", "changeLambdaInAnonymous")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedAnonymous.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, "callSite.kt")
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(ANONYMOUS_CLASS_CHANGED)
            }
        }
    }

    private companion object {
        const val INITIAL_OUTPUT = "42"
        const val WITH_NEW_LAMBDA_BODY = "45"
        const val WITH_BOTH_CHANGES = "48"
        const val LOCAL_CLASS_CHANGED = "45"
        const val NESTED_LAMBDA_CHANGED = "45"
        const val ANONYMOUS_CLASS_CHANGED = "45"
    }
}
