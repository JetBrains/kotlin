/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.kmp.infra.AbstractParser
import org.jetbrains.kotlin.kmp.infra.NewParser
import org.jetbrains.kotlin.kmp.infra.OldParser
import org.jetbrains.kotlin.kmp.infra.TestDataUtils
import org.jetbrains.kotlin.kmp.infra.compareSyntaxElements
import org.jetbrains.kotlin.kmp.infra.dump
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
        assertEquals(21, parseNodesNumber)
    }

    @Test
    fun testEmpty() {
        val (_, _, parseNodesNumber, linesCount) = checkParserOnKotlinCode("")
        assertEquals(1, linesCount)
        assertEquals(0, parseNodesNumber)
    }

    @Test
    fun testOldParseNodesDump() = testParseNodesDump(OldParser())

    @Test
    fun testNewParseNodesDump() = testParseNodesDump(NewParser())

    private fun testParseNodesDump(parser: AbstractParser<*>) {
        assertEquals(
            """KDoc [7:1..10:4)
  KDOC_START [7:1..7:4)
  WHITE_SPACE [7:4..8:2)
  KDOC_SECTION [8:2..9:23)
    KDOC_LEADING_ASTERISK [8:2..8:3)
    KDOC_TEXT [8:3..8:4)
    KDOC_TAG [8:4..8:32)
      KDOC_TAG_NAME [8:4..8:10)
      WHITE_SPACE [8:10..8:11)
      KDOC_MARKDOWN_LINK [8:11..8:16)
      WHITE_SPACE [8:16..8:17)
      KDOC_TEXT [8:17..8:32)
    WHITE_SPACE [8:32..9:2)
    KDOC_LEADING_ASTERISK [9:2..9:3)
    KDOC_TEXT [9:3..9:4)
    KDOC_TAG [9:4..9:23)
      KDOC_TAG_NAME [9:4..9:11)
      WHITE_SPACE [9:11..9:12)
      KDOC_MARKDOWN_LINK [9:12..9:23)
  WHITE_SPACE [9:23..10:2)
  KDOC_END [10:2..10:4)""",
            parser.parseKDocOnlyNodes("kotlinCodeSample.kt", kotlinCodeSample).dump(kotlinCodeSample.toSourceLinesMapping())
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

        val oldParser = OldParser()

        val oldParserStartNanos = System.nanoTime()
        val oldKDocTrees = oldParser.parseKDocOnlyNodes(path?.pathString ?: "", kotlinCodeSample)
        val oldParserNanos = System.nanoTime() - oldParserStartNanos

        val newParser = NewParser()

        val newParserStartNanos = System.nanoTime()
        val newKDocTrees = newParser.parseKDocOnlyNodes(path?.pathString ?: "", kotlinCodeSample)
        val newParserNanos = System.nanoTime() - newParserStartNanos

        fun failWithDifferentParseTrees() {
            assertEquals(
                oldKDocTrees.dump(sourceLinesMapping),
                newKDocTrees.dump(sourceLinesMapping),
                path?.let { "Different parse tree nodes on file: $it" }
            )
        }

        var parseNodesNumber = 0L

        if (oldKDocTrees.size != newKDocTrees.size) {
            failWithDifferentParseTrees()
        }

        for (index in oldKDocTrees.indices) {
            parseNodesNumber += compareSyntaxElements(oldKDocTrees[index], newKDocTrees[index]) {
                failWithDifferentParseTrees()
            }
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