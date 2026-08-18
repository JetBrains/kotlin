/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRendererWithDiagnosticId
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer.Severity
import org.jetbrains.kotlin.buildtools.api.CompilerMessageRenderer.SourceLocation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAndPlatformAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ProjectCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName

class KotlinLoggerCustomRendererTest : BaseCompilationTest() {

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Custom renderer receives structured data")
    fun customRendererReceivesStructuredData(project: ProjectCreator) {
        val renderer = object : CompilerMessageRenderer {
            override fun render(severity: Severity, message: String, location: SourceLocation?): String {
                val loc = location?.let { " ${it.path}:${it.line}:${it.column}" } ?: ""
                return "[CUSTOM $severity]$loc $message"
            }
        }

        project {
            val module = module("deprecated-usage")
            module.compile(compilationConfigAction = {
                it[BaseCompilationOperation.COMPILER_MESSAGE_RENDERER] = renderer
            }) {
                val warnLines = logLines[LogLevel.WARN].orEmpty()
                assertTrue(warnLines.any { "[CUSTOM WARNING]" in it && "deprecated" in it.lowercase() }) {
                    "Expected custom-rendered deprecation warning at WARN level, got: $warnLines"
                }
            }
        }
    }

    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Custom renderer receives compiler diagnostic identifier")
    fun customRendererReceivesDiagnosticIdentifier(project: ProjectCreator) {
        val diagnosticIds = mutableListOf<String?>()
        val renderer = object : CompilerMessageRendererWithDiagnosticId {
            override fun render(severity: Severity, message: String, location: SourceLocation?, diagnosticId: String?): String {
                diagnosticIds += diagnosticId
                return "[CUSTOM $severity][$diagnosticId] $message"
            }
        }

        project {
            val module = module("compilation-error")
            module.compile(compilationConfigAction = {
                it[BaseCompilationOperation.COMPILER_MESSAGE_RENDERER] = renderer
            }) {
                expectFail()
                val errorLines = logLines[LogLevel.ERROR].orEmpty()
                assertTrue(errorLines.any { "[CUSTOM ERROR][UNRESOLVED_REFERENCE]" in it }) {
                    "Expected custom-rendered unresolved reference error at ERROR level, got: $errorLines"
                }

                assertTrue("UNRESOLVED_REFERENCE" in diagnosticIds.filterNotNull()) {
                    "Expected UNRESOLVED_REFERENCE among the diagnostic ids received by the renderer, got: ${diagnosticIds.filterNotNull().distinct()}"
                }
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Custom renderer receives compiler diagnostic identifier during incremental compilation")
    @TestMetadata("basic-multimodule-project/module-1")
    fun customRendererReceivesDiagnosticIdentifierDuringIncrementalCompilation(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val diagnosticIds = mutableListOf<String?>()
        val renderer = object : CompilerMessageRendererWithDiagnosticId {
            override fun render(severity: Severity, message: String, location: SourceLocation?, diagnosticId: String?): String {
                diagnosticIds += diagnosticId
                return "[CUSTOM $severity][$diagnosticId] $message"
            }
        }

        jvmScenario(strategyConfig) {
            val module = module("basic-multimodule-project/module-1", compilationConfigAction = {
                it[BaseCompilationOperation.COMPILER_MESSAGE_RENDERER] = renderer
            })
            diagnosticIds.clear()

            module.changeFile("bar.kt") { it.replace("foo()", "doesNotExist()") }

            module.compile {
                expectFail()
                val errorLines = logLines[LogLevel.ERROR].orEmpty()
                assertTrue(errorLines.any { "[CUSTOM ERROR][UNRESOLVED_REFERENCE]" in it }) {
                    "Expected custom-rendered unresolved reference error at ERROR level, got: $errorLines"
                }
                assertEquals(listOf("UNRESOLVED_REFERENCE"), diagnosticIds.filterNotNull().distinct())
            }
        }
    }
}
