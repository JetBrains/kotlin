/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.kmp.infra.AbstractTestParser
import org.jetbrains.kotlin.kmp.infra.NewTestParser
import org.jetbrains.kotlin.kmp.infra.OldTestParser
import org.jetbrains.kotlin.kmp.infra.TestDataUtils
import org.jetbrains.kotlin.kmp.infra.compareSyntaxElements
import org.jetbrains.kotlin.kmp.parser.KDocParseNodes
import org.jetbrains.kotlin.toSourceLinesMapping
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.pathString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KDocParserTests {
    companion object {
        init {
            // Make sure the static declarations are initialized before time measurements to get more refined results
            LexerTests.Companion.initializeLexers()
            initializeKDocParsers()
        }

        fun initializeKDocParsers() {
            KDocElementTypes.KDOC_SECTION

            KDocParseNodes.KDOC_SECTION
        }
    }

    @Test
    fun testSimple() {
        val (_, _, parseNodesNumber, linesCount) = checkParserOnKotlinCode(kotlinCodeSample)
        assertEquals(14, linesCount)
        assertEquals(32, parseNodesNumber)
    }

    @Test
    fun testEmpty() {
        val (_, _, parseNodesNumber, linesCount) = checkParserOnKotlinCode("")
        assertEquals(1, linesCount)
        assertEquals(0, parseNodesNumber)
    }

    @Test
    fun testMarkdownLinkWithError() {
        checkParserOnKotlinCode(
            """/**
 * [A.B.C...]
 * [....]
 * [A..B..C]
 * [A.]
 */""")
    }

    @Test
    fun testOldParseNodesDump() = testParseNodesDump(OldTestParser())

    @Test
    fun testNewParseNodesDump() = testParseNodesDump(NewTestParser())

    private fun testParseNodesDump(parser: AbstractTestParser<*>) {
        assertEquals(
            """KDoc [7:1..10:4)
  KDOC_START `/**` [7:1..4)
  WHITE_SPACE [7:4..8:2)
  KDOC_SECTION [8:2..9:23)
    KDOC_LEADING_ASTERISK `*` [8:2..3)
    KDOC_TEXT ` ` [8:3..4)
    KDOC_TAG `@param [C.x] Some parameter.` [8:4..32)
      KDOC_TAG_NAME `@param` [8:4..10)
      WHITE_SPACE ` ` [8:10..11)
      KDOC_MARKDOWN_LINK `[C.x]` [8:11..16)
        LBRACKET `[` [8:11..12)
        KDOC_NAME `C.x` [8:12..15)
          KDOC_NAME `C` [8:12..13)
            IDENTIFIER `C` [8:12..13)
          DOT `.` [8:13..14)
          IDENTIFIER `x` [8:14..15)
        RBRACKET `]` [8:15..16)
      WHITE_SPACE ` ` [8:16..17)
      KDOC_TEXT `Some parameter.` [8:17..32)
    WHITE_SPACE [8:32..9:2)
    KDOC_LEADING_ASTERISK `*` [9:2..3)
    KDOC_TEXT ` ` [9:3..4)
    KDOC_TAG `@return [Exception]` [9:4..23)
      KDOC_TAG_NAME `@return` [9:4..11)
      WHITE_SPACE ` ` [9:11..12)
      KDOC_MARKDOWN_LINK `[Exception]` [9:12..23)
        LBRACKET `[` [9:12..13)
        KDOC_NAME `Exception` [9:13..22)
          IDENTIFIER `Exception` [9:13..22)
        RBRACKET `]` [9:22..23)
  WHITE_SPACE [9:23..10:2)
  KDOC_END `*/` [10:2..4)""",
            parser.parse("kotlinCodeSample.kt", kotlinCodeSample, kDocOnly = true)
                .dump(kotlinCodeSample.toSourceLinesMapping(), kotlinCodeSample)
        )
    }

    @Test
    fun testParserOnTestData() {
        var filesCounter = 0
        var oldParserTotalNanos = 0L
        var newParserTotalNanos = 0L
        var totalLinesNumber = 0L
        var totalParseNodesNumber = 0L

        TestDataUtils.checkKotlinFiles { data, path ->
            val (oldParserNanos, newParserNanos, parseNodesNumber, linesCount) = checkParserOnKotlinCode(data, path)
            oldParserTotalNanos += oldParserNanos
            newParserTotalNanos += newParserNanos
            filesCounter++
            totalParseNodesNumber += parseNodesNumber
            totalLinesNumber += linesCount
        }

        assertTrue(filesCounter > 31000, "Number of tested files (kt, kts, nkt) should be more than 31K")

        println("Number of tested files (kt, kts, nkt): $filesCounter")
        println("Number of lines: $totalLinesNumber")
        println("Number of parse tree nodes: $totalParseNodesNumber")

        // It doesn't make sense to print the total time of old PSI parser because it needs the entire document to be parsed
        // Even if only KDoc nodes are needed
        println("New parser total time: ${TimeUnit.NANOSECONDS.toMillis(newParserTotalNanos)} ms")
    }

    private fun checkParserOnKotlinCode(kotlinCodeSample: String, path: Path? = null): KDocParserStats {
        val sourceLinesMapping = kotlinCodeSample.toSourceLinesMapping()

        val oldParser = OldTestParser()

        val oldParserStartNanos = System.nanoTime()
        val oldKDocTree = oldParser.parse(path?.pathString ?: "", kotlinCodeSample, kDocOnly = true)
        val oldParserNanos = System.nanoTime() - oldParserStartNanos

        val newParser = NewTestParser()

        val newParserStartNanos = System.nanoTime()
        val newKDocTree = newParser.parse(path?.pathString ?: "", kotlinCodeSample, kDocOnly = true)
        val newParserNanos = System.nanoTime() - newParserStartNanos

        val parseNodesNumber = compareSyntaxElements(oldKDocTree, newKDocTree) {
            assertEquals(
                oldKDocTree.dump(sourceLinesMapping, kotlinCodeSample),
                newKDocTree.dump(sourceLinesMapping, kotlinCodeSample),
                path?.let { "Different parse tree nodes on file: $it" }
            )
        }

        return KDocParserStats(oldParserNanos, newParserNanos, parseNodesNumber, sourceLinesMapping.linesCount)
    }

    private data class KDocParserStats(
        val oldNanos: Long,
        val newNanos: Long,
        val parseNodesNumber: Long,
        val linesCount: Int
    )
}