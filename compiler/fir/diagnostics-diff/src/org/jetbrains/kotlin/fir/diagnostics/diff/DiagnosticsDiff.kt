/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics.diff

import java.io.File
import java.util.*
import java.util.regex.Pattern

object DiagnosticsDiff {
    fun diffTestDataFiles(
        testDataDir: File
    ): OverallResult {
        val originalTestDataFiles =
            testDataDir.walkTopDown()
                .filter { file -> file.name.let { it.endsWith(".kt") && !it.endsWith(".fir.kt") } }

        var matched = 0
        var missing = 0
        var unexpected = 0
        var mismatched = 0
        var numFiles = 0
        val aggregateDiagnosticResults: MutableMap<String, MutableAggregateDiagnosticResult> = mutableMapOf()
        for (originalTestDataFile in originalTestDataFiles) {
            val fileResult = try {
                diffTestDataFile(testDataDir, originalTestDataFile, aggregateDiagnosticResults)
            } catch (t: Throwable) {
                println("Failed to compare: ${originalTestDataFile.absolutePath}: ${t.stackTraceToString()}")
                continue
            }

            matched += fileResult.matched
            missing += fileResult.missing
            unexpected += fileResult.unexpected
            mismatched += fileResult.mismatched
            numFiles++
        }

        return OverallResult(
            matched,
            missing,
            unexpected,
            mismatched,
            numFiles,
            aggregateDiagnosticResults.mapValues { it.value.toImmutable() })
    }

    private fun diffTestDataFile(
        testDataDir: File,
        originalTestDataFile: File,
        aggregateDiagnosticResults: MutableMap<String, MutableAggregateDiagnosticResult>
    ): FileResult {
        val firTestDataFile =
            File(originalTestDataFile.parentFile, originalTestDataFile.nameWithoutExtension + ".fir.kt")

        return if (!firTestDataFile.exists()) {
            // Still comparing so we can get the match count
            diffDiagnosticFiles(
                testDataDir,
                originalTestDataFile,
                originalTestDataFile,
                aggregateDiagnosticResults
            ).also {
                assert(it.missing + it.unexpected + it.mismatched == 0)
            }
        } else {
            diffDiagnosticFiles(testDataDir, originalTestDataFile, firTestDataFile, aggregateDiagnosticResults)
        }
    }

// A lot borrowed from: compiler/frontend/src/org/jetbrains/kotlin/checkers/utils/CheckerTestUtil.kt

    private fun diffDiagnosticFiles(
        testDataDir: File,
        expectedFile: File,
        actualFile: File,
        aggregateDiagnosticResults: MutableMap<String, MutableAggregateDiagnosticResult>
    ): FileResult {
        val expectedFilePath = expectedFile.toRelativeString(testDataDir)
        val expectedRanges = extractRanges(expectedFile)
        val actualRanges = extractRanges(actualFile)

        var matched = 0
        var missing = 0
        var unexpected = 0
        var mismatched = 0

        var expectedIndex = 0
        var actualIndex = 0

        while (expectedIndex < expectedRanges.size || actualIndex < actualRanges.size) {
            fun resultForDiagnostic(diagnostic: String) =
                aggregateDiagnosticResults.getOrPut(diagnostic) { MutableAggregateDiagnosticResult() }

            val currentExpected = expectedRanges.getOrNull(expectedIndex)
            val currentActual = actualRanges.getOrNull(actualIndex)

            if (currentExpected == null) {
                requireNotNull(currentActual)
                unexpected++
                resultForDiagnostic(currentActual.diagnostic).apply {
                    this.unexpected.add(SingleDiagnosticResult(expectedFile, actualFile, null, currentActual))
                    files.add(expectedFilePath)
                }

                actualIndex++
                continue
            }

            if (currentActual == null) {
                missing++
                resultForDiagnostic(currentExpected.diagnostic).apply {
                    this.missing.add(SingleDiagnosticResult(expectedFile, actualFile, currentExpected, null))
                    files.add(expectedFilePath)
                }

                expectedIndex++
                continue
            }

            val expectedStart = currentExpected.start
            val actualStart = currentActual.start
            val expectedEnd = currentExpected.end
            val actualEnd = currentActual.end

            when {
                expectedStart < actualStart -> {
                    missing++
                    resultForDiagnostic(currentExpected.diagnostic).apply {
                        this.missing.add(SingleDiagnosticResult(expectedFile, actualFile, currentExpected, null))
                        files.add(expectedFilePath)
                    }

                    expectedIndex++
                }
                expectedStart > actualStart -> {
                    unexpected++
                    resultForDiagnostic(currentActual.diagnostic).apply {
                        this.unexpected.add(SingleDiagnosticResult(expectedFile, actualFile, null, currentActual))
                        files.add(expectedFilePath)
                    }

                    actualIndex++
                }
                expectedEnd > actualEnd -> {
                    assert(expectedStart == actualStart)
                    missing++
                    resultForDiagnostic(currentExpected.diagnostic).apply {
                        this.missing.add(SingleDiagnosticResult(expectedFile, actualFile, currentExpected, null))
                        files.add(expectedFilePath)
                    }

                    expectedIndex++
                }
                expectedEnd < actualEnd -> {
                    assert(expectedStart == actualStart)
                    unexpected++
                    resultForDiagnostic(currentActual.diagnostic).apply {
                        this.unexpected.add(SingleDiagnosticResult(expectedFile, actualFile, null, currentActual))
                        files.add(expectedFilePath)
                    }

                    actualIndex++
                }
                else -> {
                    assert(expectedStart == actualStart)
                    assert(expectedEnd == actualEnd)

                    // Get all diagnostics in expected and actual within the same range
                    // They should be sorted by diagnostic name
                    val allExpectedInSameRange = mutableListOf(currentExpected)
                    val allActualInSameRange = mutableListOf(currentActual)
                    expectedIndex++
                    actualIndex++
                    while (expectedIndex < expectedRanges.size) {
                        val nextExpected = expectedRanges[expectedIndex]
                        if (nextExpected.start == expectedStart && nextExpected.end == expectedEnd) {
                            allExpectedInSameRange.add(nextExpected)
                            expectedIndex++
                        } else break
                    }
                    while (actualIndex < actualRanges.size) {
                        val nextActual = actualRanges[actualIndex]
                        if (nextActual.start == actualStart && nextActual.end == actualEnd) {
                            allActualInSameRange.add(nextActual)
                            actualIndex++
                        } else break
                    }

                    if (allExpectedInSameRange.size == 1 && allActualInSameRange.size == 1) {
                        // Only one diagnostic in both ranges; this is either a match or mismatch
                        val diagnosticResult =
                            SingleDiagnosticResult(expectedFile, actualFile, currentExpected, currentActual)
                        if (currentActual.diagnostic == currentExpected.diagnostic) {
                            matched++
                            resultForDiagnostic(currentExpected.diagnostic).apply {
                                this.matched.add(diagnosticResult)
                                files.add(expectedFilePath)
                            }
                        } else {
                            // Mismatches are counted for both diagnostics
                            mismatched++
                            resultForDiagnostic(currentExpected.diagnostic).apply {
                                this.mismatched.add(diagnosticResult)
                                files.add(expectedFilePath)
                            }
                            resultForDiagnostic(currentActual.diagnostic).apply {
                                this.mismatched.add(diagnosticResult)
                                files.add(expectedFilePath)
                            }
                        }
                    } else {
                        // There are multiple diagnostics in either expected or actual range
                        // We compare the diagnostics in the ranges similar to how we compare the ranges in the files (i.e., advancing pointers)
                        // The difference is we don't have mismatches here since we don't know which diagnostics correspond to each other when they don't match
                        var expectedInSameRangeIndex = 0
                        var actualInSameRangeIndex = 0

                        while (expectedInSameRangeIndex < allExpectedInSameRange.size || actualInSameRangeIndex < allActualInSameRange.size) {
                            val expectedInSameRange = allExpectedInSameRange.getOrNull(expectedInSameRangeIndex)
                            val actualInSameRange = allActualInSameRange.getOrNull(actualInSameRangeIndex)

                            if (expectedInSameRange == null || actualInSameRange != null && expectedInSameRange.diagnostic > actualInSameRange.diagnostic) {
                                requireNotNull(actualInSameRange)
                                unexpected++
                                resultForDiagnostic(actualInSameRange.diagnostic).apply {
                                    this.unexpected.add(SingleDiagnosticResult(expectedFile, actualFile, null, actualInSameRange))
                                    files.add(expectedFilePath)
                                }

                                actualInSameRangeIndex++
                            } else if (actualInSameRange == null || expectedInSameRange.diagnostic < actualInSameRange.diagnostic) {
                                missing++
                                resultForDiagnostic(expectedInSameRange.diagnostic).apply {
                                    this.missing.add(SingleDiagnosticResult(expectedFile, actualFile, expectedInSameRange, null))
                                    files.add(expectedFilePath)
                                }

                                expectedInSameRangeIndex++
                            } else {
                                assert(expectedInSameRange.diagnostic == actualInSameRange.diagnostic)
                                matched++
                                resultForDiagnostic(expectedInSameRange.diagnostic).apply {
                                    this.matched.add(
                                        SingleDiagnosticResult(
                                            expectedFile,
                                            actualFile,
                                            expectedInSameRange,
                                            actualInSameRange
                                        )
                                    )
                                    files.add(expectedFilePath)
                                }

                                expectedInSameRangeIndex++
                                actualInSameRangeIndex++
                            }
                        }
                    }
                }
            }
        }

        return FileResult(matched, missing, unexpected, mismatched)
    }

    private fun extractRanges(expectedFile: File): MutableList<DiagnosedRange> {
        val diagnostic = """(\w+;)?(\w+:)?(\w+)(\{[\w;]+})?(?:\(((?:".*?")(?:,\s*".*?")*)\))?"""
        val rangeStartOrEndPattern = Pattern.compile("(<!${diagnostic}(,\\s*${diagnostic})*!>)|(<!>)")
        val individualDiagnosticPattern: Pattern = Pattern.compile(diagnostic)

        val fileText = expectedFile.readText()
        val matcher = rangeStartOrEndPattern.matcher(fileText)
        val openRanges = Stack<List<DiagnosedRange>>()
        val openMatchStarts = Stack<Int>()
        var offsetCompensation = 0

        val ranges = mutableListOf<DiagnosedRange>()
        while (matcher.find()) {
            val matchStart = matcher.start()
            val effectiveOffset = matchStart - offsetCompensation
            val matchedText = matcher.group()
            if (matchedText == "<!>") {
                openRanges.pop().let {
                    val openMatchStart = openMatchStarts.pop()
                    for (range in it) {
                        range.end = effectiveOffset
                        range.text = fileText.substring(openMatchStart, matchStart + matchedText.length)
                    }
                }
            } else {
                val diagnosticTypeMatcher = individualDiagnosticPattern.matcher(matchedText)
                val rangesInText = mutableListOf<DiagnosedRange>()
                while (diagnosticTypeMatcher.find()) {
                    // Diagnostic regex contains everything (e.g., message), we just want the diagnostic name
                    val diagnosticText = diagnosticTypeMatcher.group(3) ?: continue

                    // Skip debug info and old inference diagnostics
                    if (diagnosticText.startsWith("DEBUG_INFO_")) continue
                    if (diagnosticTypeMatcher.group(1)?.contains("OI") == true) continue
                    if (diagnosticTypeMatcher.group(4)?.contains("OI") == true) continue

                    val range = DiagnosedRange(diagnosticText, effectiveOffset)
                    rangesInText.add(range)
                }
                rangesInText.sortBy { it.diagnostic }
                ranges.addAll(rangesInText)

                openRanges.push(rangesInText)
                openMatchStarts.push(matchStart)
            }
            offsetCompensation += matchedText.length
        }

        assert(openRanges.isEmpty()) { "Stack is not empty" }
        return ranges
    }

    private class MutableAggregateDiagnosticResult {
        val matched: MutableList<SingleDiagnosticResult> = mutableListOf()
        val missing: MutableList<SingleDiagnosticResult> = mutableListOf()
        val unexpected: MutableList<SingleDiagnosticResult> = mutableListOf()
        val mismatched: MutableList<SingleDiagnosticResult> = mutableListOf()
        var files: MutableSet<String> = mutableSetOf()

        fun toImmutable() = AggregateDiagnosticResult(matched, missing, unexpected, mismatched, files.size)
    }
}
