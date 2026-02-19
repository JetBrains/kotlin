/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.jetbrains.kotlin.kmp.infra.TestSyntaxElement
import org.jetbrains.kotlin.kmp.infra.checkSyntaxElements
import org.jetbrains.kotlin.toSourceLinesMapping
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class AbstractRecognizerTests<OldT, NewT, OldSyntaxElement : TestSyntaxElement<OldT>, NewSyntaxElement : TestSyntaxElement<NewT>> {
    companion object {
        val testDataDirs: List<File> = System.getProperty("test.data.dirs").split(File.pathSeparator).map { File(it) }
    }

    abstract fun recognizeOldSyntaxElement(fileName: String, text: String): OldSyntaxElement
    abstract fun recognizeNewSyntaxElement(fileName: String, text: String): NewSyntaxElement

    abstract val recognizerName: String
    open val oldRecognizerSuffix: String = ""
    abstract val recognizerSyntaxElementName: String

    // It doesn't make sense to print the total time of an old PSI parser because it needs the entire document to be parsed
    // even if only KDoc nodes are needed
    open val printOldRecognizerTimeInfo: Boolean = true

    /**
     * PSI at first place parse blocks and lambdas lazily and expands them later if needed.
     * It may cause different parsing behavior on erroneous code.
     * If such a discrepancy is encountered, the comparison is dropped and the counter of ignored files is incremented.
     * It's not a bug in the new parsing library or the parser since there is no discrepancy with LightTree mode.
     */
    open val ignoreFilesWithSyntaxError: Boolean = false

    @Test
    fun testOnTestData() {
        var filesCounter = 0
        var oldTotalElapsedNanos = 0L
        var newTotalElapsedNanos = 0L
        var totalCharsNumber = 0L
        var totalLinesNumber = 0L
        var totalSyntaxElementNumber = 0L
        var totalNumberOfFilesWithSyntaxErrors = 0L
        var totalNumberOfFilesWithSyntaxErrorsAndTreeDiscrepancy = 0L
        val comparisonFailures = mutableListOf<() -> Unit>()

        files@ for (testDataDir in testDataDirs) {
            testDataDir.walkTopDown()
                .filter { it.isFile && it.extension.let { ext -> (ext == "kt" || ext == "kts") && !it.path.endsWith(".fir.kt") } }
                .forEach { file ->
                    val refinedText = clearTextFromDiagnosticMarkup(file.readText())
                        .replace("\r\n", "\n") // Test infrastructure normalizes line endings

                    val (comparisonFailure, oldNanos, newNanos, oldSyntaxElement, _, linesCount) = getComparisonResult(
                        refinedText,
                        file.toPath()
                    )
                    oldTotalElapsedNanos += oldNanos
                    newTotalElapsedNanos += newNanos
                    filesCounter++
                    totalCharsNumber += refinedText.length
                    val (syntaxElementNumber, hasSyntaxError) = oldSyntaxElement.countSyntaxElements()
                    totalSyntaxElementNumber += syntaxElementNumber

                    if (hasSyntaxError) {
                        totalNumberOfFilesWithSyntaxErrors++
                        if (comparisonFailure != null) {
                            if (!ignoreFilesWithSyntaxError) {
                                comparisonFailures.add(comparisonFailure)
                            } else {
                                totalNumberOfFilesWithSyntaxErrorsAndTreeDiscrepancy++
                            }
                        }
                    } else {
                        comparisonFailures.addIfNotNull(comparisonFailure)
                    }

                    totalLinesNumber += linesCount
                }
        }

        val newOldLexerTimeRatio = newTotalElapsedNanos.toDouble() / oldTotalElapsedNanos

        println("Number of tested files (kt, kts): $filesCounter")
        println("Number of files with syntax errors: $totalNumberOfFilesWithSyntaxErrors")
        if (totalNumberOfFilesWithSyntaxErrorsAndTreeDiscrepancy > 0) {
            println("Number of files with syntax errors and tree discrepancy: $totalNumberOfFilesWithSyntaxErrorsAndTreeDiscrepancy")
        }
        println("Number of chars: $totalCharsNumber")
        println("Number of lines: $totalLinesNumber")
        println("Number of ${recognizerSyntaxElementName}s: $totalSyntaxElementNumber")
        if (comparisonFailures.isNotEmpty()) {
            println("Number of tree mismatches: ${comparisonFailures.size}")
        }
        if (printOldRecognizerTimeInfo) {
            println("Old ${recognizerName + oldRecognizerSuffix} total time: ${TimeUnit.NANOSECONDS.toMillis(oldTotalElapsedNanos)} ms")
        }
        println("New $recognizerName total time: ${TimeUnit.NANOSECONDS.toMillis(newTotalElapsedNanos)} ms")
        if (printOldRecognizerTimeInfo) {
            println("New/Old $recognizerName time ratio: %.4f".format(newOldLexerTimeRatio))
        }

        comparisonFailures.add {
            val approximateNumberOfTestDataFiles = 33400
            assertTrue(filesCounter > approximateNumberOfTestDataFiles, "Number of tested files (kt, kts) should be more than $approximateNumberOfTestDataFiles")
        }

        assertAll(comparisonFailures)
    }

    private fun getComparisonResult(kotlinCodeSample: String, path: Path? = null): ComparisonResult {
        val sourceLinesMapping = kotlinCodeSample.toSourceLinesMapping()

        val oldStartNanos = System.nanoTime()
        val oldSyntaxElement = recognizeOldSyntaxElement(path?.toString() ?: "", kotlinCodeSample)
        val oldElapsedNanos = System.nanoTime() - oldStartNanos

        val newStartNanos = System.nanoTime()
        val newSyntaxElement = recognizeNewSyntaxElement(path?.toString() ?: "", kotlinCodeSample)
        val newElapsedNanos = System.nanoTime() - newStartNanos

        val areStructurallyEqual = checkSyntaxElements(oldSyntaxElement, newSyntaxElement)
        // Use text dumps comparison if only the comparison is failed
        // Because dumping and string comparison work slower
        val comparisonFailure: (() -> Unit)? = if (!areStructurallyEqual) {
            {
                assertEquals(
                    oldSyntaxElement.dump(sourceLinesMapping, kotlinCodeSample),
                    newSyntaxElement.dump(sourceLinesMapping, kotlinCodeSample),
                    path?.let { "Different ${recognizerSyntaxElementName}s on file: $it" }
                )
                fail("Should not be here. Text dumping should correspond tree comparison logic, fix it.")
            }
        } else {
            null
        }

        return ComparisonResult(
            comparisonFailure,
            oldElapsedNanos,
            newElapsedNanos,
            oldSyntaxElement,
            newSyntaxElement,
            sourceLinesMapping.linesCount
        )
    }

    data class ComparisonResult(
        val failure: (() -> Unit)?,
        val oldNanos: Long,
        val newNanos: Long,
        val oldSyntaxElement: TestSyntaxElement<*>,
        val newSyntaxElement: TestSyntaxElement<*>,
        val linesCount: Int,
    )
}
