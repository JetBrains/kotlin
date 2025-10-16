/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.arguments.CommonToolArguments.Companion.VERBOSE
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.assertNoOutputSetChanges
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DisplayName

class CancellationCompatibilitySmokeTest : BaseCompilationTest() {

    @DisplayName("Sample non-incremental compilation test with cancellation")
    @DefaultStrategyAgnosticCompilationTest
    fun myTest(strategyConfig: CompilerExecutionStrategyConfiguration) {
        Assumptions.assumeFalse(strategyConfig.first.javaClass.simpleName.contains("V1Adapter"))
        val hasCancellationSupport =
            KotlinToolingVersion(strategyConfig.first.getCompilerVersion()) > KotlinToolingVersion(2, 3, 0, "Beta1")
        project(strategyConfig) {
            val module1 = module("jvm-module-1")

            // you should handle the right order of compilation between modules yourself
            module1.compile(compilationConfigAction = { operation ->
                try {
                    operation.cancel()
                } catch (_: IllegalStateException) {
                    assertFalse(hasCancellationSupport)
                }
            }) { module ->
                if (hasCancellationSupport) {
                    expectCompilationResult(CompilationResult.COMPILER_INTERNAL_ERROR)
                    assertNoCompiledSources(module)
                    assertLogContainsSubstringExactlyTimes(LogLevel.ERROR, "org.jetbrains.kotlin.progress.CompilationCanceledException", 1)
                }
            }
        }
    }
}
