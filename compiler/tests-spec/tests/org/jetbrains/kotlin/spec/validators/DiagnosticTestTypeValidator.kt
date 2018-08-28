/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.validators

import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity

class DiagnosticTestTypeValidator(testFiles: List<BaseDiagnosticsTest.TestFile>) {
    private val diagnostics = mutableListOf<Diagnostic>()
    private val diagnosticStats = mutableMapOf<String, Int>()
    private val diagnosticSeverityStats = mutableMapOf<Severity, Int>()

    init {
        collectDiagnostics(testFiles)
    }

    private fun collectDiagnosticStatistic() {
        diagnostics.forEach {
            val severity = it.factory.severity
            diagnosticSeverityStats.run { put(severity, getOrDefault(severity, 0) + 1) }
        }
    }

    private fun collectDiagnostics(files: List<BaseDiagnosticsTest.TestFile>) {
        files.forEach {
            it.actualDiagnostics.forEach {
                val diagnosticName = it.diagnostic.factory.name
                diagnosticStats.run { put(diagnosticName, getOrDefault(diagnosticName, 0) + 1) }
                diagnostics.add(it.diagnostic)
            }
        }
        collectDiagnosticStatistic()
    }

    fun computeTestType() =
        if (Severity.ERROR in diagnosticSeverityStats) TestType.NEGATIVE else TestType.POSITIVE

    fun printDiagnosticStatistic() {
        val diagnostics = if (diagnosticStats.isNotEmpty()) "$diagnosticSeverityStats | $diagnosticStats" else "does not contain"
        println("DIAGNOSTICS: $diagnostics")
    }
}