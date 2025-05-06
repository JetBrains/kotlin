/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.kmp.infra.NewParserTestNode
import org.jetbrains.kotlin.kmp.infra.NewTestParser
import org.jetbrains.kotlin.kmp.infra.OldTestParser
import org.jetbrains.kotlin.kmp.infra.TestParseNode
import org.jetbrains.kotlin.kmp.infra.TestSyntaxElement
import org.jetbrains.kotlin.kmp.infra.compareSyntaxElements
import org.jetbrains.kotlin.toSourceLinesMapping
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class AbstractRecognizerTests<OldT, NewT, OldSyntaxElement : TestSyntaxElement<OldT>, NewSyntaxElement : TestSyntaxElement<NewT>> {
    companion object {
        const val KOTLIN_CODE_SAMPLE = """fun main() {
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
}"""

        const val KOTLIN_CODE_SAMPLE_FILE_NAME = "kotlinCodeSample.kt"

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
    abstract val recognizerSyntaxElementName: String

    abstract val expectedExampleDump: String
    abstract val expectedExampleSyntaxElementsNumber: Long

    // It doesn't make sense to print the total time of old PSI parser because it needs the entire document to be parsed
    // even if only KDoc nodes are needed
    open val printOldRecognizerTimeInfo: Boolean = true

    @Test
    open fun testSimple() {
        val (_, _, syntaxElementsNumber, linesCount) = checkOnKotlinCode(KOTLIN_CODE_SAMPLE)
        assertEquals(14, linesCount)
        assertEquals(expectedExampleSyntaxElementsNumber, syntaxElementsNumber)
    }

    @Test
    open fun testEmpty() {
        val (_, _, syntaxElementsNumber, linesCount) = checkOnKotlinCode("")
        assertEquals(1, linesCount)
        assertEquals(0, syntaxElementsNumber)
    }

    @Test
    fun testOldDump() {
        assertEquals(
            expectedExampleDump,
            recognizeOldSyntaxElement(KOTLIN_CODE_SAMPLE_FILE_NAME, KOTLIN_CODE_SAMPLE).dump(
                KOTLIN_CODE_SAMPLE.toSourceLinesMapping(),
                KOTLIN_CODE_SAMPLE
            )
        )
    }

    @Test
    open fun testNewDump() {
        assertEquals(
            expectedExampleDump,
            recognizeNewSyntaxElement(KOTLIN_CODE_SAMPLE_FILE_NAME, KOTLIN_CODE_SAMPLE).dump(
                KOTLIN_CODE_SAMPLE.toSourceLinesMapping(),
                KOTLIN_CODE_SAMPLE
            )
        )
    }

    @Test
    open fun testOnTestData() {
        var filesCounter = 0
        var oldTotalElapsedNanos = 0L
        var newTotalElapsedNanos = 0L
        var totalCharsNumber = 0L
        var totalLinesNumber = 0L
        var totalSyntaxElementNumber = 0L

        for (testDataDir in testDataDirs) {
            testDataDir.walkTopDown()
                .filter { it.isFile && it.extension.let { ext -> ext == "kt" || ext == "kts" || ext == "nkt" } }
                .forEach { file ->
                    val refinedText = file.readText().replace(allMetadataRegex, "")

                    val (oldElapsedNanos, newElapsedNanos, syntaxElementNumber, linesCount) = checkOnKotlinCode(refinedText, file.toPath())
                    oldTotalElapsedNanos += oldElapsedNanos
                    newTotalElapsedNanos += newElapsedNanos
                    filesCounter++
                    totalCharsNumber += refinedText.length
                    totalSyntaxElementNumber += syntaxElementNumber
                    totalLinesNumber += linesCount
                }
        }

        val newOldLexerTimeRatio = newTotalElapsedNanos.toDouble() / oldTotalElapsedNanos

        assertTrue(filesCounter > 31000, "Number of tested files (kt, kts, nkt) should be more than 31K")

        println("Number of tested files (kt, kts, nkt): $filesCounter")
        println("Number of chars: $totalCharsNumber")
        println("Number of lines: $totalLinesNumber")
        println("Number of ${recognizerSyntaxElementName}s: $totalSyntaxElementNumber")
        if (printOldRecognizerTimeInfo) {
            println("Old $recognizerName total time: ${TimeUnit.NANOSECONDS.toMillis(oldTotalElapsedNanos)} ms")
        }
        println("New $recognizerName total time: ${TimeUnit.NANOSECONDS.toMillis(newTotalElapsedNanos)} ms")
        if (printOldRecognizerTimeInfo) {
            println("New/Old $recognizerName time ratio: %.4f".format(newOldLexerTimeRatio))
        }
    }

    protected fun checkOnKotlinCode(kotlinCodeSample: String, path: Path? = null): RecognizerStats {
        val sourceLinesMapping = kotlinCodeSample.toSourceLinesMapping()

        val oldStartNanos = System.nanoTime()
        val oldSyntaxElement = recognizeOldSyntaxElement(path?.toString() ?: "", kotlinCodeSample)
        val oldElapsedNanos = System.nanoTime() - oldStartNanos

        val newStartNanos = System.nanoTime()
        val newSyntaxElement = recognizeNewSyntaxElement(path?.toString() ?: "", kotlinCodeSample)
        val newElapsedNanos = System.nanoTime() - newStartNanos

        val syntaxElementsNumber = compareSyntaxElements(oldSyntaxElement, newSyntaxElement) {
            assertEquals(
                oldSyntaxElement.dump(sourceLinesMapping, kotlinCodeSample),
                newSyntaxElement.dump(sourceLinesMapping, kotlinCodeSample),
                path?.let { "Different ${recognizerSyntaxElementName}s on file: $it" }
            )
            fail("Should not be here. Text dumping should correspond tree comparison logic, fix it.")
        }

        return RecognizerStats(
            oldElapsedNanos,
            newElapsedNanos,
            syntaxElementsNumber,
            sourceLinesMapping.linesCount
        )
    }

    data class RecognizerStats(
        val oldNanos: Long,
        val newNanos: Long,
        val elementsNumber: Long,
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

    override val recognizerSyntaxElementName: String = "parse node"
}



