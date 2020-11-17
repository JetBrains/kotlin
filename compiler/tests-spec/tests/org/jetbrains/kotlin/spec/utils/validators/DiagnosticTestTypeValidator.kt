/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.validators

import org.jetbrains.kotlin.checkers.BaseDiagnosticsTest
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.spec.utils.TestCasesByNumbers
import org.jetbrains.kotlin.spec.utils.TestType
import org.jetbrains.kotlin.spec.utils.models.AbstractSpecTest
import java.io.File

class DiagnosticTestTypeValidator(
    testFiles: List<BaseDiagnosticsTest.TestFile>,
    testDataFile: File,
    private val testInfo: AbstractSpecTest
) : AbstractTestValidator(testInfo, testDataFile) {
    private val diagnostics = mutableListOf<Diagnostic>()
    private val diagnosticStats = mutableMapOf<String, Int>()
    private val diagnosticSeverityStats = mutableMapOf<Int, MutableMap<Severity, Int>>()

    init {
        collectDiagnostics(testFiles)
    }

    private fun findTestCases(diagnostic: Diagnostic): TestCasesByNumbers {
        val ranges = diagnostic.textRanges
        val filename = diagnostic.psiFile!!.name
        val foundTestCases = testInfo.cases.byRanges[filename]!!.floorEntry(ranges[0].startOffset)

        if (foundTestCases != null)
            return foundTestCases.value

        throw SpecTestValidationException(SpecTestValidationFailedReason.INVALID_TEST_CASES_STRUCTURE)
    }

    private fun collectDiagnosticStatistic() {
        diagnostics.forEach {
            val testCases = findTestCases(it)
            val severity = it.factory.severity

            for ((caseNumber, _) in testCases) {
                diagnosticSeverityStats.putIfAbsent(caseNumber, mutableMapOf())
                diagnosticSeverityStats[caseNumber]!!.run { put(severity, getOrDefault(severity, 0) + 1) }
            }
        }
    }

    private fun collectDiagnostics(files: List<BaseDiagnosticsTest.TestFile>) {
        files.forEach { file ->
            file.actualDiagnostics.forEach {
                val diagnosticName = it.diagnostic.factory.name!!
                diagnosticStats.run { put(diagnosticName, getOrDefault(diagnosticName, 0) + 1) }
                diagnostics.add(it.diagnostic)
            }
        }
        collectDiagnosticStatistic()
    }

    override fun computeTestTypes() = diagnosticSeverityStats.mapValues {
        if (Severity.ERROR in it.value) TestType.NEGATIVE else TestType.POSITIVE
    }

    fun printDiagnosticStatistic() {
        val diagnostics = if (diagnosticStats.isNotEmpty()) "$diagnosticSeverityStats | $diagnosticStats" else "does not contain"
        println("DIAGNOSTICS: $diagnostics")
    }
}