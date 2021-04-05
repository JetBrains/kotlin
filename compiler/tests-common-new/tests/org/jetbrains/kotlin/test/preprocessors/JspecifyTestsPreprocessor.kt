/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.preprocessors

import org.jetbrains.kotlin.checkers.DiagnosedRange
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.test.KtAssert
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JSPECIFY_MUTE
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.JavaTypeEnhancementState
import org.jetbrains.kotlin.utils.ReportLevel
import java.util.*

class JspecifyTestsPreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    val shouldAutoApplyChanges = System.getProperty("autoApply") == "true"

    private fun getJspecifyMarkRegex(jspecifyMark: String) = Regex("""^\s*// $jspecifyMark$""")
    private fun checkIfAllJspecifyMarksByDiagnosticsArePresent(
        diagnosedRanges: List<DiagnosedRange>,
        lineIndexesByRanges: TreeMap<Int, Int>,
        textLines: List<String>,
        compilerDiagnosticsToJspecifyMarksMap: Map<String, String>
    ) {
        for (diagnosticRange in diagnosedRanges) {
            val lineIndex = lineIndexesByRanges.floorEntry(diagnosticRange.start).value

            for (diagnostic in diagnosticRange.getDiagnostics()) {
                val requiredJspecifyMark = compilerDiagnosticsToJspecifyMarksMap[diagnostic.name] ?: continue

                fun getErrorMessage(lineIndex: Int) =
                    "Jspecify mark '$requiredJspecifyMark' not found for diagnostic '${diagnostic}' at ${lineIndex + 1} line.\n" +
                            "It should be located at the previous line as a comment."

                assert(lineIndex != 0) { getErrorMessage(0) }

                val previousLine = textLines[lineIndex - 1]

                assert(getJspecifyMarkRegex(requiredJspecifyMark).matches(previousLine)) { getErrorMessage(lineIndex) }
            }
        }
    }

    private fun checkIfAllDiagnosticsByJspecifyMarksArePresent(
        diagnosedRanges: List<DiagnosedRange>,
        lineIndexesByRanges: TreeMap<Int, Int>,
        textLines: List<String>,
        compilerDiagnosticsToJspecifyMarksMap: Map<String, List<String>>
    ) {
        for ((jspecifyMark, possibleDiagnostics) in compilerDiagnosticsToJspecifyMarksMap) {
            val diagnosticRanges = diagnosedRanges.mapNotNull {
                val relevantDiagnostics = it.getDiagnostics().filter { it.name in possibleDiagnostics }
                if (relevantDiagnostics.isEmpty()) return@mapNotNull null
                it.start
            }

            val lineIndexesWithJspecifyMarks =
                textLines.mapIndexedNotNull { index, it -> getJspecifyMarkRegex(jspecifyMark).find(it)?.let { index } }

            if (diagnosticRanges.isEmpty()) {
                if (lineIndexesWithJspecifyMarks.isEmpty()) {
                    continue
                } else {
                    KtAssert.fail(
                        "None of \"${possibleDiagnostics.joinToString(", ")}\" diagnostics not found " +
                                "for jspecify mark '$jspecifyMark' at lines: ${lineIndexesWithJspecifyMarks.map { it + 1 }.joinToString()}"
                    )
                }
            }

            for (lineIndex in lineIndexesWithJspecifyMarks) {
                val lineStartPosition = lineIndexesByRanges.entries.find { (_, index) -> index == lineIndex + 1 }?.key
                val errorMessage = "None of \"${possibleDiagnostics.joinToString()}\" diagnostics not found " +
                        "for jspecify mark '$jspecifyMark' at ${lineIndex + 1} line"

                KtAssert.assertTrue(errorMessage, lineStartPosition != null)

                val lineEndPosition = lineStartPosition!! + textLines[lineIndex + 1].length
                val isCorrespondingDiagnosticPresent = diagnosticRanges.any { it in lineStartPosition..lineEndPosition }

                KtAssert.assertTrue(errorMessage, isCorrespondingDiagnosticPresent)
            }
        }
    }

    override fun process(file: TestFile, content: String): String {
        if (!file.relativePath.endsWith(".kt")) return content

        val textWithDiagnostics = content.substringAfter(MAIN_KT_FILE_DIRECTIVE).removeSuffix("\n")
        val diagnosedRanges = mutableListOf<DiagnosedRange>()
        val textWithoutDiagnostics = CheckerTestUtil.parseDiagnosedRanges(textWithDiagnostics, diagnosedRanges)

        val textLines = textWithoutDiagnostics.lines()
        val lineIndexesByRanges = TreeMap<Int, Int>().apply {
            textLines.scanIndexed(0) { index, position, line ->
                put(position, index)
                position + line.length + 1 // + new line symbol
            }
        }

        val jspecifyMode = file.directives[ForeignAnnotationsDirectives.JSPECIFY_STATE].singleOrNull()
            ?: JavaTypeEnhancementState.DEFAULT_REPORT_LEVEL_FOR_JSPECIFY
        val compilerDiagnosticsToJspecifyMarksMap = when (jspecifyMode) {
            ReportLevel.STRICT -> diagnosticsToJspecifyMarksMapForStrictMode
            ReportLevel.WARN -> diagnosticsToJspecifyMarksMapForWarnMode
            ReportLevel.IGNORE -> mapOf()
        }
        val jspecifyMarksToCompilerDiagnosticsMap = when (jspecifyMode) {
            ReportLevel.STRICT -> jspecifyMarksToPossibleDiagnosticsForStrictMode
            ReportLevel.WARN -> jspecifyMarksToPossibleDiagnosticsForWarnMode
            ReportLevel.IGNORE -> mapOf()
        }

        if (shouldAutoApplyChanges || JSPECIFY_MUTE in testServices.moduleStructure.allDirectives) {
            return content
        }

        checkIfAllJspecifyMarksByDiagnosticsArePresent(
            diagnosedRanges,
            lineIndexesByRanges,
            textLines,
            compilerDiagnosticsToJspecifyMarksMap
        )
        checkIfAllDiagnosticsByJspecifyMarksArePresent(
            diagnosedRanges,
            lineIndexesByRanges,
            textLines,
            jspecifyMarksToCompilerDiagnosticsMap
        )

        return content
    }

    companion object {
        const val MAIN_KT_FILE_DIRECTIVE = "// FILE: main.kt\n"

        val diagnosticsToJspecifyMarksMapForWarnMode = mapOf(
            ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.name to "jspecify_nullness_mismatch",
            Errors.TYPE_MISMATCH.name to "jspecify_nullness_mismatch",
            Errors.NULL_FOR_NONNULL_TYPE.name to "jspecify_nullness_mismatch",
            Errors.NOTHING_TO_OVERRIDE.name to "jspecify_nullness_mismatch",
            Errors.RETURN_TYPE_MISMATCH_ON_OVERRIDE.name to "jspecify_nullness_mismatch",
            Errors.UPPER_BOUND_VIOLATED.name to "jspecify_nullness_mismatch",
            ErrorsJvm.NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.name to "jspecify_nullness_mismatch",
            ErrorsJvm.RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS.name to "jspecify_nullness_mismatch",
            Errors.UNSAFE_CALL.name to "jspecify_nullness_mismatch",
        )

        val jspecifyMarksToPossibleDiagnosticsForWarnMode =
            diagnosticsToJspecifyMarksMapForWarnMode.entries.groupBy({ it.value }, { it.key })

        val diagnosticsToJspecifyMarksMapForStrictMode = diagnosticsToJspecifyMarksMapForWarnMode

        val jspecifyMarksToPossibleDiagnosticsForStrictMode =
            diagnosticsToJspecifyMarksMapForStrictMode.entries.groupBy({ it.value }, { it.key })
    }
}
