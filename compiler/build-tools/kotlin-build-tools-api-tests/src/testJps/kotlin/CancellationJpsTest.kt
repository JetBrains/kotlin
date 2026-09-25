/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

import org.jetbrains.kotlin.buildtools.api.OperationCancelledException
import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.walk

@DisplayName("Cancellation of a JPS-managed compilation")
class CancellationJpsTest : BaseJpsTest() {

    @DisplayName("A cancelled JPS-managed compilation stops the compiler")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("basic-multimodule-project/module-1")
    fun cancellationStopsTheCompiler(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compileAndThrow(
                compilationConfigAction = { it.withJpsIc() },
                compilationAction = { it.cancel() },
            ) { e ->
                assertTrue(e is OperationCancelledException) { "Expected a cancellation, got: $e" }
                assertLogContainsSubstringExactlyTimes(LogLevel.ERROR, COMPILATION_CANCELED_EXCEPTION, 1)
            }
            assertNoClassFiles(module.outputDirectory)
        }
    }

    private fun assertNoClassFiles(outputDirectory: Path) {
        val classFiles = if (outputDirectory.exists()) outputDirectory.walk().filter { it.extension == "class" }.toList() else emptyList()
        assertTrue(classFiles.isEmpty()) { "The compiler did not stop on cancellation and produced: $classFiles" }
    }

    private companion object {
        const val COMPILATION_CANCELED_EXCEPTION = "org.jetbrains.kotlin.progress.CompilationCanceledException"
    }
}
