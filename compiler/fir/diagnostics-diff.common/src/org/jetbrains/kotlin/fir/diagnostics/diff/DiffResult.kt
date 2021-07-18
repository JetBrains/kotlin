/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics.diff

import java.io.File

sealed class OverallResult {
    abstract val matched: Int
    abstract val missing: Int
    abstract val unexpected: Int
    abstract val mismatched: Int
    abstract val numFiles: Int

    abstract val allDiagnostics: Collection<String>
    abstract fun matchedFor(diagnostic: String): Int
    abstract fun missingFor(diagnostic: String): Int
    abstract fun unexpectedFor(diagnostic: String): Int
    abstract fun mismatchedFor(diagnostic: String): Int
    abstract fun numFilesFor(diagnostic: String): Int

    val nonMatched: Int get() = missing + unexpected + mismatched
    val total: Int get() = nonMatched + matched
    open val matchPct: Double get() = matched.toDouble() / total

    val allDiagnosticsSortedByNonMatchedDescending: Collection<String>
        get() = allDiagnostics.sorted().sortedByDescending { diagnostic -> nonMatchedFor(diagnostic) }

    fun nonMatchedFor(diagnostic: String): Int =
        missingFor(diagnostic) + unexpectedFor(diagnostic) + mismatchedFor(diagnostic)

    fun totalFor(diagnostic: String): Int = nonMatchedFor(diagnostic) + matchedFor(diagnostic)

    open fun matchPctFor(diagnostic: String): Double = matchedFor(diagnostic).toDouble() / totalFor(diagnostic)
}

class SingleCommitOverallResult(
    override val matched: Int,
    override val missing: Int,
    override val unexpected: Int,
    override val mismatched: Int,
    override val numFiles: Int,
    val aggregateDiagnosticResults: Map<String, AggregateDiagnosticResult>,
) : OverallResult() {
    override val allDiagnostics: Collection<String> = aggregateDiagnosticResults.keys

    override fun matchedFor(diagnostic: String) = aggregateDiagnosticResults[diagnostic]?.matched?.size ?: 0
    override fun missingFor(diagnostic: String) = aggregateDiagnosticResults[diagnostic]?.missing?.size ?: 0
    override fun unexpectedFor(diagnostic: String) = aggregateDiagnosticResults[diagnostic]?.unexpected?.size ?: 0
    override fun mismatchedFor(diagnostic: String) = aggregateDiagnosticResults[diagnostic]?.mismatched?.size ?: 0
    override fun numFilesFor(diagnostic: String) = aggregateDiagnosticResults[diagnostic]?.numFiles ?: 0
}

data class SingleDiagnosticResult constructor(
    val expectedFilePath: String,
    val actualFilePath: String,
    val expectedRange: DiagnosedRange?,
    val actualRange: DiagnosedRange?,
) {
    val expectedFile = File(expectedFilePath)
    val actualFile = File(actualFilePath)

    constructor(
        expectedFile: File,
        actualFile: File,
        expectedRange: DiagnosedRange?,
        actualRange: DiagnosedRange?
    ) : this(expectedFile.path, actualFile.path, expectedRange, actualRange)
}

class AggregateDiagnosticResult(
    val matched: Collection<SingleDiagnosticResult>,
    val missing: Collection<SingleDiagnosticResult>,
    val unexpected: Collection<SingleDiagnosticResult>,
    val mismatched: Collection<SingleDiagnosticResult>,
    val numFiles: Int,
) {
    fun isEmpty() = matched.isEmpty() && missing.isEmpty() && unexpected.isEmpty() && mismatched.isEmpty() && numFiles == 0
}

class FileResult(
    val matched: Int,
    val missing: Int,
    val unexpected: Int,
    val mismatched: Int,
)

data class DiagnosedRange(
    val diagnostic: String,
    val start: Int,
    var end: Int = -1,
) {
    var text: String = ""
}

class DeltaOverallResult(
    private val before: SingleCommitOverallResult,
    private val after: SingleCommitOverallResult
) : OverallResult() {
    override val matched: Int
    override val missing: Int
    override val unexpected: Int
    override val mismatched: Int
    override val numFiles: Int
    val removed: Map<String, AggregateDiagnosticResult>
    val added: Map<String, AggregateDiagnosticResult>

    init {
        matched = after.matched - before.matched
        missing = after.missing - before.missing
        unexpected = after.unexpected - before.unexpected
        mismatched = after.mismatched - before.mismatched
        numFiles = after.numFiles - before.numFiles

        fun diffResults(
            afterResults: Collection<SingleDiagnosticResult>,
            beforeResults: Collection<SingleDiagnosticResult>
        ): Pair<Collection<SingleDiagnosticResult>, Collection<SingleDiagnosticResult>> { // "removed" to "added"
            val afterSet = afterResults.toSet()
            val beforeSet = beforeResults.toSet()

            return (beforeSet - afterSet) to (afterResults - beforeSet)
        }

        removed = mutableMapOf()
        added = mutableMapOf()
        val allDiagnostics = after.aggregateDiagnosticResults.keys + before.aggregateDiagnosticResults.keys
        for (diagnostic in allDiagnostics) {
            val currentResult = after.aggregateDiagnosticResults[diagnostic]
            val baselineResult = before.aggregateDiagnosticResults[diagnostic]
            require(currentResult != null || baselineResult != null)
            when {
                currentResult == null -> removed[diagnostic] = baselineResult!!
                baselineResult == null -> added[diagnostic] = currentResult
                else -> {
                    val matchedDiff = diffResults(currentResult.matched, baselineResult.matched)
                    val missingDiff = diffResults(currentResult.missing, baselineResult.missing)
                    val unexpectedDiff = diffResults(currentResult.unexpected, baselineResult.unexpected)
                    val mismatchedDiff = diffResults(currentResult.mismatched, baselineResult.mismatched)
                    val numFilesDiff = currentResult.numFiles - baselineResult.numFiles
                    AggregateDiagnosticResult(
                        matchedDiff.first,
                        missingDiff.first,
                        unexpectedDiff.first,
                        mismatchedDiff.first,
                        -numFilesDiff
                    ).let { if (!it.isEmpty()) removed[diagnostic] = it }
                    AggregateDiagnosticResult(
                        matchedDiff.second,
                        missingDiff.second,
                        unexpectedDiff.second,
                        mismatchedDiff.second,
                        numFilesDiff
                    ).let { if (!it.isEmpty()) added[diagnostic] = it }
                }
            }
        }
    }

    override val matchPct = after.matchPct - before.matchPct

    override val allDiagnostics: Collection<String> = removed.keys + added.keys

    override fun matchedFor(diagnostic: String) = after.matchedFor(diagnostic) - before.matchedFor(diagnostic)
    override fun missingFor(diagnostic: String) = after.missingFor(diagnostic) - before.missingFor(diagnostic)
    override fun unexpectedFor(diagnostic: String) = after.unexpectedFor(diagnostic) - before.unexpectedFor(diagnostic)
    override fun mismatchedFor(diagnostic: String) = after.mismatchedFor(diagnostic) - before.mismatchedFor(diagnostic)
    override fun numFilesFor(diagnostic: String) = after.numFilesFor(diagnostic) - before.numFilesFor(diagnostic)
    override fun matchPctFor(diagnostic: String) = after.matchPctFor(diagnostic) - before.matchPctFor(diagnostic)
}
