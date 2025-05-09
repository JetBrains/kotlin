/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.psi.PsiElement
import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.kmp.infra.NewParserTestNode
import org.jetbrains.kotlin.kmp.infra.NewTestParser
import org.jetbrains.kotlin.kmp.infra.OldTestParser
import org.jetbrains.kotlin.kmp.infra.TestParseNode
import org.jetbrains.kotlin.kmp.infra.TestSyntaxElement
import org.jetbrains.kotlin.kmp.infra.checkSyntaxElements
import org.jetbrains.kotlin.toSourceLinesMapping
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class AbstractRecognizerTests<OldT, NewT, OldSyntaxElement : TestSyntaxElement<OldT>, NewSyntaxElement : TestSyntaxElement<NewT>> {
    companion object {
        val testDataDirs: List<File> = System.getProperty("test.data.dirs").split(File.pathSeparator).map { File(it) }

        // TODO: for some reason, it's not possible to depend on `:compiler:test-infrastructure-utils` here
        // See org.jetbrains.kotlin.codeMetaInfo.CodeMetaInfoParser
        private val openingDiagnosticRegex = """(<!([^"]*?((".*?")(, ".*?")*?)?[^"]*?)!>)""".toRegex()
        private val closingDiagnosticRegex = """(<!>)""".toRegex()

        private val xmlLikeTagsRegex = """(</?(?:selection|expr|caret)>)""".toRegex()

        private val allMetadataRegex =
            """(${closingDiagnosticRegex.pattern}|${openingDiagnosticRegex.pattern}|${xmlLikeTagsRegex.pattern})""".toRegex()
    }

    abstract fun recognizeOldSyntaxElement(fileName: String, text: String): OldSyntaxElement
    abstract fun recognizeNewSyntaxElement(fileName: String, text: String): NewSyntaxElement

    abstract val recognizerName: String
    open val oldRecognizerSuffix: String = ""
    abstract val recognizerSyntaxElementName: String

    abstract val expectedExampleDump: String
    abstract val expectedExampleSyntaxElementsNumber: Long
    open val expectedEmptySyntaxElementsNumber: Long = 0

    // It doesn't make sense to print the total time of old PSI parser because it needs the entire document to be parsed
    // even if only KDoc nodes are needed
    open val printOldRecognizerTimeInfo: Boolean = true

    @Test
    open fun testSimple() {
        val (_, _, _, oldSyntaxElement, _, linesCount) = checkOnKotlinCode(
            """fun main() {
    println("Hello, World!")
}

class C(val x: Int)

/**
 * @param [C.x] Some parameter.
 * @return [Exception]
 */
fun test(p: String) {
    val badCharacter = ^
    throw Exception()
}""",
            expectedExampleDump
        )
        assertEquals(14, linesCount)
        assertEquals(expectedExampleSyntaxElementsNumber, oldSyntaxElement.countSyntaxElements())
    }

    @Test
    open fun testEmpty() {
        val (_, _, _, oldSyntaxElement, _, linesCount) = checkOnKotlinCode("")
        assertEquals(1, linesCount)
        assertEquals(expectedEmptySyntaxElementsNumber, oldSyntaxElement.countSyntaxElements())
    }

    @Test
    open fun testOnTestData() {
        var filesCounter = 0
        var oldTotalElapsedNanos = 0L
        var newTotalElapsedNanos = 0L
        var totalCharsNumber = 0L
        var totalLinesNumber = 0L
        var totalSyntaxElementNumber = 0L
        val comparisonFailures = mutableListOf<() -> Unit>()

        for (testDataDir in testDataDirs) {
            testDataDir.walkTopDown()
                .filter { it.isFile && it.extension.let { ext -> ext == "kt" || ext == "kts" || ext == "nkt" } }
                .forEach { file ->
                    val refinedText = file.readText().replace(allMetadataRegex, "")

                    val (comparisonFailure, oldNanos, newNanos, oldSyntaxElement, _, linesCount) = getComparisonResult(
                        refinedText,
                        file.toPath()
                    )
                    comparisonFailures.addIfNotNull(comparisonFailure)
                    oldTotalElapsedNanos += oldNanos
                    newTotalElapsedNanos += newNanos
                    filesCounter++
                    totalCharsNumber += refinedText.length
                    totalSyntaxElementNumber += oldSyntaxElement.countSyntaxElements()
                    totalLinesNumber += linesCount
                }
        }

        val newOldLexerTimeRatio = newTotalElapsedNanos.toDouble() / oldTotalElapsedNanos

        println("Number of tested files (kt, kts, nkt): $filesCounter")
        println("Number of chars: $totalCharsNumber")
        println("Number of lines: $totalLinesNumber")
        println("Number of ${recognizerSyntaxElementName}s: $totalSyntaxElementNumber")
        if (comparisonFailures.isNotEmpty()) {
            println("Number of errors: ${comparisonFailures.size}")
        }
        if (printOldRecognizerTimeInfo) {
            println("Old ${recognizerName + oldRecognizerSuffix} total time: ${TimeUnit.NANOSECONDS.toMillis(oldTotalElapsedNanos)} ms")
        }
        println("New $recognizerName total time: ${TimeUnit.NANOSECONDS.toMillis(newTotalElapsedNanos)} ms")
        if (printOldRecognizerTimeInfo) {
            println("New/Old $recognizerName time ratio: %.4f".format(newOldLexerTimeRatio))
        }

        comparisonFailures.add {
            assertTrue(filesCounter > 31000, "Number of tested files (kt, kts, nkt) should be more than 31K")
        }

        assertAll(comparisonFailures)
    }

    protected fun checkOnKotlinCode(text: String, expectedDump: String? = null, isScript: Boolean = false): ComparisonResult {
        val comparisonResult = getComparisonResult(
            text,
            Path("sample." + (if (isScript) "kts" else "kt"))
        )

        val assertionFailures = mutableListOf<() -> Unit>()

        if (expectedDump != null) {
            // Assume old tree representation is reference.
            assertionFailures.add { assertEquals(expectedDump, comparisonResult.oldSyntaxElement.dump(text.toSourceLinesMapping(), text)) }
        }

        assertionFailures.addIfNotNull(comparisonResult.failure)

        assertAll(assertionFailures)

        return comparisonResult
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

abstract class AbstractParserTests : AbstractRecognizerTests<PsiElement, NewParserTestNode, TestParseNode<out PsiElement>, TestParseNode<out NewParserTestNode>>() {
    abstract val kDocOnly: Boolean
    override fun recognizeOldSyntaxElement(fileName: String, text: String): TestParseNode<out PsiElement> =
        OldTestParser().parse(fileName, text, kDocOnly)

    override fun recognizeNewSyntaxElement(fileName: String, text: String): TestParseNode<out NewParserTestNode> =
        NewTestParser().parse(fileName, text, kDocOnly)

    override val recognizerName: String = "parser"
    override val oldRecognizerSuffix: String = " (PSI)" // A bit later LightTree mode also will be implemented

    override val recognizerSyntaxElementName: String = "parse node"
}



