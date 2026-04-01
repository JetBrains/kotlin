/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer.Severity
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer.SourceLocation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.project
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName

class KotlinLoggerCustomRendererTest : BaseCompilationTest() {

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Custom renderer receives structured data")
    fun customRendererReceivesStructuredData(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val renderer = object : CompilerMessageRenderer {
            override fun render(severity: Severity, message: String, location: SourceLocation?): String {
                val loc = location?.let { " ${it.path}:${it.line}:${it.column}" } ?: ""
                return "[CUSTOM $severity]$loc $message"
            }
        }

        project(strategyConfig) {
            val module = module("deprecated-usage")
            module.compile(compilationConfigAction = {
                it[JvmCompilationOperation.COMPILER_MESSAGE_RENDERER] = renderer
            }) {
                val warnLines = logLines[LogLevel.WARN].orEmpty()
                assertTrue(warnLines.any { "[CUSTOM WARNING]" in it && "deprecated" in it.lowercase() }) {
                    "Expected custom-rendered deprecation warning at WARN level, got: $warnLines"
                }
            }
        }
    }
}