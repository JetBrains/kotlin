/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.OperationCancelledException
import org.jetbrains.kotlin.buildtools.tests.SmokeCompilationTest.Companion.jvmNonIncrementalCompilationOperation
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.writeText

class LinkageFixTest : BaseCompilationTest() {

    @DisplayName("KT-87597: test that LinkageError is not thrown from KotlinLoggerMessageCollectorAdapter")
    @DefaultForwardCompatibilityCompilationTest
    fun testNonIncrementalCompilation(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val sourceA = workingDirectory.resolve("a.kt")

        val sources = listOf(
            sourceA.apply { writeText("fun a() = 42") },
        )
        val destination = workingDirectory.resolve("classes")

        val [toolchain, executionPolicy] = strategyConfig

        toolchain.createBuildSession().use {
            val compilation = jvmNonIncrementalCompilationOperation(sources, destination)
            compilation.cancel()
            assertThrows<OperationCancelledException> { it.executeOperation(compilation, executionPolicy) }
        }
    }
}
