/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertExactOutput
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName


// When adding or changing tests, make sure that test sources don't have unintended changes in whitespace, copyright notices and similar things:
// debug info is sensitive to changes in line numbers, and it's part of default inline function abiHash
class InlinedLambdaChangeTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("When inlined lambda's body changes, its call site is recompiled")
    @TestMetadata("ic-scenarios/inline-local-class/lambda-body-change/lib")
    fun testMainCase(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/lambda-body-change/lib")
            val app = module(
                "ic-scenarios/inline-local-class/lambda-body-change/app",
                dependencies = listOf(lib),
                snapshotInlinedClassesInDependencies = true,
            )

            app.executeCompiledCode("com.example.ictest.CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }

            lib.replaceFileWithVersion("com/example/ictest/inlinedLambda.kt", "changeLambdaBody")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "com/example/ictest/inlinedLambda.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, "com/example/ictest/callSite.kt")
            }
            app.executeCompiledCode("com.example.ictest.CallSiteKt") {
                assertExactOutput(WITH_NEW_LAMBDA_BODY)
            }

            lib.replaceFileWithVersion("com/example/ictest/inlinedLambda.kt", "changeFunctionBody")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "com/example/ictest/inlinedLambda.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, "com/example/ictest/callSite.kt")
            }
            app.executeCompiledCode("com.example.ictest.CallSiteKt") {
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
                assertCompiledSources(module, "inlinedLambda.kt")
                //interestingly, in this version we don't recompile the call site, but the build works.
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
            val app = module(
                "ic-scenarios/inline-local-class/local-in-local/app",
                dependencies = listOf(lib),
                snapshotInlinedClassesInDependencies = true,
            )

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
                assertExactOutput(WITH_NEW_LAMBDA_BODY)
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Catch bad implementation: if lambda is used multiple times, handle its changes (hierarchical version)")
    @TestMetadata("ic-scenarios/inline-local-class/local-uses-local-deduplication/lib")
    fun testInlinedClassDeduplication(strategyConfig: CompilerExecutionStrategyConfiguration) {
        /**
         * This is basically a "bad implementation catcher" test: if we use xor for multihashing, we *must* deduplicate all classes
         * mixed into the inline function's hash. Otherwise this happens:
         *
         * A xor A = 0L //for any A
         * fun uses classes (A, B); B uses A; if brokenhash(A,B,A)==brokenhash(B), then any change to A won't be caught be the snapshotter
         */
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/local-uses-local-deduplication/lib")
            val app = module(
                "ic-scenarios/inline-local-class/local-uses-local-deduplication/app",
                dependencies = listOf(lib),
                snapshotInlinedClassesInDependencies = true,
            )

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
                assertExactOutput("46")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Catch bad implementation: if lambda is used multiple times, handle its changes (flat version)")
    @TestMetadata("ic-scenarios/inline-local-class/local-uses-local-deduplication-v2/lib")
    fun testInlinedClassDeduplicationV2(strategyConfig: CompilerExecutionStrategyConfiguration) {
        // same as [testInlinedClassDeduplication]
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/local-uses-local-deduplication-v2/lib")
            val app = module(
                "ic-scenarios/inline-local-class/local-uses-local-deduplication-v2/app",
                dependencies = listOf(lib),
                snapshotInlinedClassesInDependencies = true,
            )

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
                assertExactOutput("46")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes in unused code should not trigger recompilation of call site")
    @TestMetadata("ic-scenarios/inline-local-class/no-recompile/lib")
    fun testNoRecompilationNeeded(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/no-recompile/lib")
            val app = module(
                "ic-scenarios/inline-local-class/no-recompile/app",
                dependencies = listOf(lib),
                snapshotInlinedClassesInDependencies = true,
            )

            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }

            lib.replaceFileWithVersion("inlinedFunction.kt", "changeUnusedCode")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedFunction.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, setOf())
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes in lambda inside inline function B affect call site of inline function A that calls B")
    @TestMetadata("ic-scenarios/inline-local-class/nested-inline/lib")
    fun testNestedInlineFunctions(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/nested-inline/lib")
            val app = module(
                "ic-scenarios/inline-local-class/nested-inline/app",
                dependencies = listOf(lib),
                snapshotInlinedClassesInDependencies = true,
            )

            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }

            lib.replaceFileWithVersion("inlinedB.kt", "changeLambdaInB")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedB.kt")
                // It so happens that we don't need to recompile A.
                // As far as IC is concerned, the compiler can do whatever it wants:
                // the main criteria for these tests is that the expected output is generated by the built & executed app.
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, "callSite.kt")
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(WITH_NEW_LAMBDA_BODY)
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes in lambda inside inline property getter trigger recompilation")
    @TestMetadata("ic-scenarios/inline-local-class/inline-property/lib")
    fun testInlineProperty(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/inline-property/lib")
            val app = module(
                "ic-scenarios/inline-local-class/inline-property/app",
                dependencies = listOf(lib),
                snapshotInlinedClassesInDependencies = true,
            )

            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }

            lib.replaceFileWithVersion("inlinedProperty.kt", "changeLambdaBody")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedProperty.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, "callSite.kt")
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(WITH_NEW_LAMBDA_BODY)
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes in multiple unused lambdas should not trigger recompilation")
    @TestMetadata("ic-scenarios/inline-local-class/no-recompile-lambdas/lib")
    fun testMultipleUnusedLambdas(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/no-recompile-lambdas/lib")
            val app = module(
                "ic-scenarios/inline-local-class/no-recompile-lambdas/app",
                dependencies = listOf(lib),
                snapshotInlinedClassesInDependencies = true,
            )

            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }

            lib.replaceFileWithVersion("inlinedFunction.kt", "changeUnusedLambdas")

            lib.compile { module, scenarioModule ->
                assertCompiledSources(module, "inlinedFunction.kt")
            }
            app.compile { module, scenarioModule ->
                assertCompiledSources(module, setOf())
            }
            app.executeCompiledCode("CallSiteKt") {
                assertExactOutput(INITIAL_OUTPUT)
            }
        }
    }

    private companion object {
        const val INITIAL_OUTPUT = "42"
        const val WITH_NEW_LAMBDA_BODY = "45"
        const val WITH_BOTH_CHANGES = "48"
    }
}
