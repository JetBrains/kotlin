/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kmp.LexerTests.Companion.initializeLexers
import org.jetbrains.kotlin.kmp.infra.AbstractParser
import org.jetbrains.kotlin.kmp.infra.NewParser
import org.jetbrains.kotlin.kmp.infra.OldParser
import org.jetbrains.kotlin.kmp.infra.TestDataUtils
import org.jetbrains.kotlin.kmp.infra.compareSyntaxElements
import org.jetbrains.kotlin.toSourceLinesMapping
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.pathString
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParserTests {
    companion object {
        init {
            // Make sure the static declarations are initialized before time measurements to get more refined results
            initializeLexers()
            KDocParserTests.initializeKDocParsers()
            initializeParsers()
        }

        fun initializeParsers() {
            org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.CLASS
            org.jetbrains.kotlin.KtNodeTypes.KT_FILE
        }
    }

    @Test
    @Disabled("Remove after KT-77144 fixing")
    fun testSimple() {
        val (_, _, parseNodesNumber, linesCount) = checkParserOnKotlinCode(kotlinCodeSample)
        assertEquals(12, linesCount)
        assertEquals(40, parseNodesNumber)
    }

    @Test
    @Disabled("Remove after KT-77144 fixing")
    fun testEmpty() {
        val (_, _, parseNodesNumber, linesCount) = checkParserOnKotlinCode("")
        assertEquals(1, linesCount)
        assertEquals(1, parseNodesNumber)
    }

    @Test
    fun testOldParseNodesDump() = testParseNodesDump(OldParser())

    @Test
    @Disabled("Remove after KT-77144 fixing")
    fun testNewParseNodesDump() = testParseNodesDump(NewParser())

    private fun testParseNodesDump(parser: AbstractParser<*>) {
        assertEquals(
"""kotlin.FILE [1:1..14:2)
  PACKAGE_DIRECTIVE [1:1..1:1)
  IMPORT_LIST [1:1..1:1)
  FUN [1:1..3:2)
    fun [1:1..1:4)
    WHITE_SPACE [1:4..1:5)
    IDENTIFIER [1:5..1:9)
    VALUE_PARAMETER_LIST [1:9..1:11)
      LPAR [1:9..1:10)
      RPAR [1:10..1:11)
    WHITE_SPACE [1:11..1:12)
    BLOCK [1:12..3:2)
      LBRACE [1:12..1:13)
      WHITE_SPACE [1:13..2:5)
      CALL_EXPRESSION [2:5..2:29)
        REFERENCE_EXPRESSION [2:5..2:12)
          IDENTIFIER [2:5..2:12)
        VALUE_ARGUMENT_LIST [2:12..2:29)
          LPAR [2:12..2:13)
          VALUE_ARGUMENT [2:13..2:28)
            STRING_TEMPLATE [2:13..2:28)
              OPEN_QUOTE [2:13..2:14)
              LITERAL_STRING_TEMPLATE_ENTRY [2:14..2:27)
                REGULAR_STRING_PART [2:14..2:27)
              CLOSING_QUOTE [2:27..2:28)
          RPAR [2:28..2:29)
      WHITE_SPACE [2:29..3:1)
      RBRACE [3:1..3:2)
  WHITE_SPACE [3:2..5:1)
  CLASS [5:1..5:20)
    class [5:1..5:6)
    WHITE_SPACE [5:6..5:7)
    IDENTIFIER [5:7..5:8)
    PRIMARY_CONSTRUCTOR [5:8..5:20)
      VALUE_PARAMETER_LIST [5:8..5:20)
        LPAR [5:8..5:9)
        VALUE_PARAMETER [5:9..5:19)
          val [5:9..5:12)
          WHITE_SPACE [5:12..5:13)
          IDENTIFIER [5:13..5:14)
          COLON [5:14..5:15)
          WHITE_SPACE [5:15..5:16)
          TYPE_REFERENCE [5:16..5:19)
            USER_TYPE [5:16..5:19)
              REFERENCE_EXPRESSION [5:16..5:19)
                IDENTIFIER [5:16..5:19)
        RPAR [5:19..5:20)
  WHITE_SPACE [5:20..7:1)
  FUN [7:1..14:2)
    KDoc [7:1..10:4)
      KDOC_START [7:1..7:4)
      WHITE_SPACE [7:4..8:2)
      KDOC_SECTION [8:2..9:23)
        KDOC_LEADING_ASTERISK [8:2..8:3)
        KDOC_TEXT [8:3..8:4)
        KDOC_TAG [8:4..8:32)
          KDOC_TAG_NAME [8:4..8:10)
          WHITE_SPACE [8:10..8:11)
          KDOC_MARKDOWN_LINK [8:11..8:16)
            LBRACKET [8:11..8:12)
            KDOC_NAME [8:12..8:15)
              KDOC_NAME [8:12..8:13)
                IDENTIFIER [8:12..8:13)
              DOT [8:13..8:14)
              IDENTIFIER [8:14..8:15)
            RBRACKET [8:15..8:16)
          WHITE_SPACE [8:16..8:17)
          KDOC_TEXT [8:17..8:32)
        WHITE_SPACE [8:32..9:2)
        KDOC_LEADING_ASTERISK [9:2..9:3)
        KDOC_TEXT [9:3..9:4)
        KDOC_TAG [9:4..9:23)
          KDOC_TAG_NAME [9:4..9:11)
          WHITE_SPACE [9:11..9:12)
          KDOC_MARKDOWN_LINK [9:12..9:23)
            LBRACKET [9:12..9:13)
            KDOC_NAME [9:13..9:22)
              IDENTIFIER [9:13..9:22)
            RBRACKET [9:22..9:23)
      WHITE_SPACE [9:23..10:2)
      KDOC_END [10:2..10:4)
    WHITE_SPACE [10:4..11:1)
    fun [11:1..11:4)
    WHITE_SPACE [11:4..11:5)
    IDENTIFIER [11:5..11:9)
    VALUE_PARAMETER_LIST [11:9..11:20)
      LPAR [11:9..11:10)
      VALUE_PARAMETER [11:10..11:19)
        IDENTIFIER [11:10..11:11)
        COLON [11:11..11:12)
        WHITE_SPACE [11:12..11:13)
        TYPE_REFERENCE [11:13..11:19)
          USER_TYPE [11:13..11:19)
            REFERENCE_EXPRESSION [11:13..11:19)
              IDENTIFIER [11:13..11:19)
      RPAR [11:19..11:20)
    WHITE_SPACE [11:20..11:21)
    BLOCK [11:21..14:2)
      LBRACE [11:21..11:22)
      WHITE_SPACE [11:22..12:5)
      PROPERTY [12:5..12:23)
        val [12:5..12:8)
        WHITE_SPACE [12:8..12:9)
        IDENTIFIER [12:9..12:21)
        WHITE_SPACE [12:21..12:22)
        EQ [12:22..12:23)
        ERROR_ELEMENT [12:23..12:23)
      WHITE_SPACE [12:23..12:24)
      ERROR_ELEMENT [12:24..12:25)
        BAD_CHARACTER [12:24..12:25)
      WHITE_SPACE [12:25..13:5)
      THROW [13:5..13:22)
        throw [13:5..13:10)
        WHITE_SPACE [13:10..13:11)
        CALL_EXPRESSION [13:11..13:22)
          REFERENCE_EXPRESSION [13:11..13:20)
            IDENTIFIER [13:11..13:20)
          VALUE_ARGUMENT_LIST [13:20..13:22)
            LPAR [13:20..13:21)
            RPAR [13:21..13:22)
      WHITE_SPACE [13:22..14:1)
      RBRACE [14:1..14:2)""",
            parser.parse("kotlinCodeSample.kt", kotlinCodeSample).dump(kotlinCodeSample.toSourceLinesMapping())
        )
    }

    @Test
    @Disabled("Remove after KT-77144 fixing")
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

        val newOldParserTimeRatio = newParserTotalNanos.toDouble() / oldParserTotalNanos

        assertTrue(filesCounter > 31000, "Number of tested files (kt, kts, nkt) should be more than 31K")
        assertEquals(newOldParserTimeRatio, 1.0, 0.2, "Parsers performance should be almost equal")

        println("Number of tested files (kt, kts, nkt): $filesCounter")
        println("Number of lines: $totalLinesNumber")
        println("Number of parse tree nodes: $totalParseNodesNumber")
        println("Old parser total time: ${TimeUnit.NANOSECONDS.toMillis(oldParserTotalNanos)} ms")
        println("New parser total time: ${TimeUnit.NANOSECONDS.toMillis(newParserTotalNanos)} ms")
        println("New/Old parser time ratio: %.4f".format(newOldParserTimeRatio))
    }

    private fun checkParserOnKotlinCode(kotlinCodeSample: String, path: Path? = null): ParserStats {
        val sourceLinesMapping = kotlinCodeSample.toSourceLinesMapping()

        val oldParser = OldParser()

        val oldParserStartNanos = System.nanoTime()
        val oldParseTree = oldParser.parse(path?.pathString ?: "", kotlinCodeSample)
        val oldParserNanos = System.nanoTime() - oldParserStartNanos

        val newParser = NewParser()

        val newParserStartNanos = System.nanoTime()
        val newParseTree = newParser.parse(path?.pathString ?: "", kotlinCodeSample)
        val newParserNanos = System.nanoTime() - newParserStartNanos

        fun failWithDifferentParseTrees() {
            assertEquals(
                oldParseTree.dump(sourceLinesMapping),
                newParseTree.dump(sourceLinesMapping),
                path?.let { "Different parse tree nodes on file: $it" }
            )
        }

        val parseNodesNumber = compareSyntaxElements(oldParseTree, newParseTree) {
            failWithDifferentParseTrees()
        }

        return ParserStats(oldParserNanos, newParserNanos, parseNodesNumber, sourceLinesMapping.linesCount)
    }

    private data class ParserStats(
        val oldNanos: Long,
        val newNanos: Long,
        val parseNodesNumber: Long,
        val linesCount: Int,
    )
}