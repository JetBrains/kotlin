/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName

@DisplayName("The JPS inline constant tracker")
class InlineConstTrackerJpsTest : BaseJpsTest() {

    @DisplayName("A Java constant inlined into Kotlin code is reported")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("kotlin-java-constant")
    fun inlinedJavaConstantIsReported(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val fixture = "kotlin-java-constant"
        jvmProject(strategyConfig) {
            val module = module(fixture)
            val inlineConstTracker = RecordingInlineConstTracker()
            module.compile(compilationConfigAction = { builder ->
                builder.withJpsIc {
                    this[JvmJpsManagedIncrementalCompilationConfiguration.INLINE_CONST_TRACKER] = inlineConstTracker
                }
            }) {
                assertTrue(inlineConstTracker.reports.isNotEmpty()) { "Inline const tracker didn't produce any output" }
                // a set: the FIR and the IR path may both report the same constant
                val actual = inlineConstTracker.reports
                    .map { it.copy(filePath = it.filePath.relativeToModule(fixture)) }
                    .toSet()
                assertEquals(
                    setOf(RecordingInlineConstTracker.Report("/src/usage.kt", "JavaConstants", "VERSION", "String")),
                    actual,
                )
            }
        }
    }
}
