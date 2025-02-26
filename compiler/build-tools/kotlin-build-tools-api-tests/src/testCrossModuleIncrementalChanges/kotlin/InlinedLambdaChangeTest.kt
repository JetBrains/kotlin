/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.SnapshotConfig
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.compile
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.execute
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName


// When adding or changing tests, make sure that test sources don't have unintended changes in whitespace, copyright notices and similar things:
// debug info is sensitive to changes in line numbers, and it's part of default inline function abiHash
@Disabled("KT-62555")
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
                snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
            )

            app.execute(mainClass = "com.example.ictest.CallSiteKt", exactOutput = INITIAL_OUTPUT)

            lib.replaceFileWithVersion("com/example/ictest/inlinedLambda.kt", "changeLambdaBody")

            lib.compile(expectedDirtySet = setOf("com/example/ictest/inlinedLambda.kt"))
            app.compile(expectedDirtySet = setOf("com/example/ictest/callSite.kt"))
            app.execute(mainClass = "com.example.ictest.CallSiteKt", exactOutput = WITH_NEW_LAMBDA_BODY)

            lib.replaceFileWithVersion("com/example/ictest/inlinedLambda.kt", "changeFunctionBody")

            lib.compile(expectedDirtySet = setOf("com/example/ictest/inlinedLambda.kt"))
            app.compile(expectedDirtySet = setOf("com/example/ictest/callSite.kt"))
            app.execute(mainClass = "com.example.ictest.CallSiteKt", exactOutput = WITH_BOTH_CHANGES)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Single-module version of inlined lambda changes")
    @TestMetadata("ic-scenarios/inline-local-class/single-module/app")
    fun testSingleModule(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val app = module("ic-scenarios/inline-local-class/single-module/app")

            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)

            app.replaceFileWithVersion("inlinedLambda.kt", "changeLambdaBody")

            app.compile(expectedDirtySet = setOf("inlinedLambda.kt"))
            //interestingly, in this version we don't recompile the call site, but the build works.
            app.execute(mainClass = "CallSiteKt", exactOutput = WITH_NEW_LAMBDA_BODY)
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
                snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
            )

            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)

            lib.replaceFileWithVersion("inlinedLocalClass.kt", "changeInnerComputation")

            lib.compile(expectedDirtySet = setOf("inlinedLocalClass.kt"))
            app.compile(expectedDirtySet = setOf("callSite.kt"))
            app.execute(mainClass = "CallSiteKt", exactOutput = WITH_NEW_LAMBDA_BODY)
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
                snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
            )

            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)

            lib.replaceFileWithVersion("inlinedLocalClass.kt", "changeInnerComputation")

            lib.compile(expectedDirtySet = setOf("inlinedLocalClass.kt"))
            app.compile(expectedDirtySet = setOf("callSite.kt"))
            app.execute(mainClass = "CallSiteKt", exactOutput = NEW_LAMBDA_BODY_WITH_DUPLICATED_LAMBDA_USAGE)
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
                snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
            )

            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)

            lib.replaceFileWithVersion("inlinedLocalClass.kt", "changeInnerComputation")

            lib.compile(expectedDirtySet = setOf("inlinedLocalClass.kt"))
            app.compile(expectedDirtySet = setOf("callSite.kt"))
            app.execute(mainClass = "CallSiteKt", exactOutput = NEW_LAMBDA_BODY_WITH_DUPLICATED_LAMBDA_USAGE)
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
                snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
            )

            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)

            lib.replaceFileWithVersion("inlinedFunction.kt", "changeUnusedCode")

            lib.compile(expectedDirtySet = setOf("inlinedFunction.kt"))
            app.compile(expectedDirtySet = setOf())
            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)
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
                snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
            )

            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)

            lib.replaceFileWithVersion("inlinedB.kt", "changeLambdaInB")

            // It so happens that we don't need to recompile A.
            // As far as IC is concerned, the compiler can do whatever it wants:
            // the main criteria for these tests is that the expected output is generated by the built & executed app.
            lib.compile(expectedDirtySet = setOf("inlinedB.kt"))
            app.compile(expectedDirtySet = setOf("callSite.kt"))
            app.execute(mainClass = "CallSiteKt", exactOutput = WITH_NEW_LAMBDA_BODY)
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
                snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
            )

            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)

            lib.replaceFileWithVersion("inlinedProperty.kt", "changeLambdaBody")

            lib.compile(expectedDirtySet = setOf("inlinedProperty.kt"))
            app.compile(expectedDirtySet = setOf("callSite.kt"))
            app.execute(mainClass = "CallSiteKt", exactOutput = WITH_NEW_LAMBDA_BODY)
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
                snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
            )

            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)

            lib.replaceFileWithVersion("inlinedFunction.kt", "changeUnusedLambdas")

            lib.compile(expectedDirtySet = setOf("inlinedFunction.kt"))
            app.compile(expectedDirtySet = setOf())
            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Basic interaction with crossinline")
    @TestMetadata("ic-scenarios/inline-local-class/inline-crossinline/lib")
    fun testCrossInlineLambdaChange(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/inline-crossinline/lib")
            val app = module(
                "ic-scenarios/inline-local-class/inline-crossinline/app",
                dependencies = listOf(lib),
                snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
            )

            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)

            lib.replaceFileWithVersion("inlinedB.kt", "changeLambdaBody")

            lib.compile(expectedDirtySet = setOf("inlinedB.kt"))
            app.compile(expectedDirtySet = setOf("callSite.kt"))
            app.execute(mainClass = "CallSiteKt", exactOutput = WITH_NEW_LAMBDA_BODY)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Recompilation of call site affected by a anonymous object - basic")
    @TestMetadata("ic-scenarios/inline-local-class/inline-anonymous-object/lib")
    fun testAnonymousObjectBaseTypeChange(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/inline-anonymous-object/lib")
            val app = module(
                "ic-scenarios/inline-local-class/inline-anonymous-object/app",
                dependencies = listOf(lib),
            )
            // doesn't require inlined class snapshotting in this case

            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)

            lib.replaceFileWithVersion("baseType.kt", "changeCompute")

            lib.compile(expectedDirtySet = setOf("baseType.kt"))
            app.compile(expectedDirtySet = setOf())
            app.execute(mainClass = "CallSiteKt", exactOutput = WITH_NEW_LAMBDA_BODY)
        }
    }

    @Disabled("broken! other snapshotting strategies might work better here")
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Recompilation of call site affected by a anonymous object - slightly evil")
    @TestMetadata("ic-scenarios/inline-local-class/inline-anonymous-object-evil/lib")
    fun testAnonymousObjectBaseTypeChangeWithOverloads(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-local-class/inline-anonymous-object-evil/lib")
            val app = module(
                "ic-scenarios/inline-local-class/inline-anonymous-object-evil/app",
                dependencies = listOf(lib),
                snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
            )

            app.execute(mainClass = "CallSiteKt", exactOutput = INITIAL_OUTPUT)

            lib.replaceFileWithVersion("SomeClass.kt", "withOverload")

            lib.compile(expectedDirtySet = setOf("SomeClass.kt", "callable.kt"))
            app.compile(expectedDirtySet = setOf())
            app.execute(mainClass = "CallSiteKt", exactOutput = WITH_NEW_LAMBDA_BODY)
        }
    }

    private companion object {
        const val INITIAL_OUTPUT = "42"
        const val WITH_NEW_LAMBDA_BODY = "45"
        const val WITH_BOTH_CHANGES = "48"
        const val NEW_LAMBDA_BODY_WITH_DUPLICATED_LAMBDA_USAGE = "46"
    }
}
