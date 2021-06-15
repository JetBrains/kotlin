/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics.diff

import java.io.File

class CsvReportGenerator(private val overallResult: OverallResult, private val outputFile: File) {
    fun generateReport() {
        fun Double.toPercent() = if (isInfinite() || isNaN()) 0.0 else this

        with(overallResult) {
            outputFile.printWriter().use { out ->
                val nonMatched = missing + unexpected + mismatched
                val total = matched + nonMatched
                val overallMatchPct = matched.toDouble() / total
                out.println("diagnostic,total,matchPct,matched,nonMatched,missing,unexpected,mismatched,numFiles")
                out.println("<OVERALL>,$total,${overallMatchPct.toPercent()},$matched,$nonMatched,$missing,$unexpected,$mismatched,$numFiles")

                for (diagnostic in allDiagnosticsSortedByNonMatchedDescending) {
                    val matchedCount = matchedFor(diagnostic)
                    val missingCount = missingFor(diagnostic)
                    val unexpectedCount = unexpectedFor(diagnostic)
                    val mismatchedCount = mismatchedFor(diagnostic)
                    val fileCount = numFilesFor(diagnostic)
                    val nonMatchedCount = missingCount + unexpectedCount + mismatchedCount
                    val totalCount = matchedCount + nonMatchedCount
                    val matchPct = matchedCount.toDouble() / totalCount
                    out.println("$diagnostic,$totalCount,${matchPct.toPercent()},$matchedCount,$nonMatchedCount,$missingCount,$unexpectedCount,$mismatchedCount,$fileCount")
                }
            }
        }
    }
}