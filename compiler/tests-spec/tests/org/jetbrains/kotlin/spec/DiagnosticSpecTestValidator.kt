/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec

import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import java.io.File

class DiagnosticSpecTestValidator(testDataFile: File) : SpecTestValidator(testDataFile, TestArea.DIAGNOSTICS) {
    private lateinit var diagnostics: MutableList<Diagnostic>
    private lateinit var diagnosticStats: MutableMap<String, Int>
    private lateinit var diagnosticSeverityStats: MutableMap<Severity, Int>

    private fun collectDiagnosticStatistic() {
        diagnosticSeverityStats = mutableMapOf()

        diagnostics.forEach {
            val severity = it.factory.severity

            if (diagnosticSeverityStats.contains(severity)) {
                diagnosticSeverityStats[severity] = diagnosticSeverityStats[severity]!! + 1
            } else {
                diagnosticSeverityStats[severity] = 1
            }
        }
    }

    private fun computeTestType(): TestType {
        return if (Severity.ERROR in diagnosticSeverityStats) TestType.NEGATIVE else TestType.POSITIVE
    }

    private fun collectDiagnostics(files: List<BaseDiagnosticsTest.TestFile>) {
        diagnostics = mutableListOf()
        diagnosticStats = mutableMapOf()

        files.forEach {
            it.actualDiagnostics.forEach {
                val diagnosticName = it.diagnostic.factory.name

                if (diagnosticStats.contains(diagnosticName)) {
                    diagnosticStats[diagnosticName] = diagnosticStats[diagnosticName]!! + 1
                } else {
                    diagnosticStats[diagnosticName] = 1
                }

                diagnostics.add(it.diagnostic)
            }
        }

        collectDiagnosticStatistic()
    }

    fun validateTestType(files: List<BaseDiagnosticsTest.TestFile>) {
        if (!this::diagnostics.isInitialized) this.collectDiagnostics(files)

        validateTestType(computeTestType())
    }

    fun printDiagnosticStatistic() {
        val diagnostics = if (diagnosticStats.isNotEmpty()) "$diagnosticSeverityStats | $diagnosticStats" else "does not contain"

        println("DIAGNOSTICS: $diagnostics")
    }
}