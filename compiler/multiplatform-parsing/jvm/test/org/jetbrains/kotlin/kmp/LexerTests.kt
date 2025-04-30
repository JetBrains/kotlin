/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kmp.infra.AbstractLexer
import org.jetbrains.kotlin.kmp.infra.MultiToken
import org.jetbrains.kotlin.kmp.infra.NewLexer
import org.jetbrains.kotlin.kmp.infra.OldLexer
import org.jetbrains.kotlin.kmp.infra.TestDataUtils
import org.jetbrains.kotlin.kmp.infra.Token
import org.jetbrains.kotlin.kmp.infra.dump
import org.jetbrains.kotlin.toSourceLinesMapping
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LexerTests {
    companion object {
        // Make sure the static declarations are initialized before time measurements to get more refined results
        init {
            org.jetbrains.kotlin.lexer.KtTokens.EOF
            org.jetbrains.kotlin.kdoc.lexer.KDocTokens.START

            org.jetbrains.kotlin.kmp.lexer.KtTokens.EOF
            org.jetbrains.kotlin.kmp.lexer.KDocTokens.START
        }
    }

    val kotlinCodeSample = """
            fun main() {
                println("Hello, World!")
            }

            /**
             * @param [p] Some parameter.
             * @return [Exception]
             */
            fun test(p: String) {
                val badCharacter = ^
                throw Exception()
            }
        """.trimIndent()

    @Test
    fun testSimple() {
        checkLexerOnKotlinCode(kotlinCodeSample)
    }

    @Test
    fun testEmpty() {
        checkLexerOnKotlinCode("")
    }

    @Test
    fun testOldTokensDump() = testTokensDump(OldLexer())

    @Test
    fun testNewTokensDump() = testTokensDump(NewLexer())

    private fun testTokensDump(lexer: AbstractLexer<*>) {
        assertEquals("""
            fun [1:1..1:4)
            WHITE_SPACE [1:4..1:5)
            IDENTIFIER [1:5..1:9)
            LPAR [1:9..1:10)
            RPAR [1:10..1:11)
            WHITE_SPACE [1:11..1:12)
            LBRACE [1:12..1:13)
            WHITE_SPACE [1:13..2:5)
            IDENTIFIER [2:5..2:12)
            LPAR [2:12..2:13)
            OPEN_QUOTE [2:13..2:14)
            REGULAR_STRING_PART [2:14..2:27)
            CLOSING_QUOTE [2:27..2:28)
            RPAR [2:28..2:29)
            WHITE_SPACE [2:29..3:1)
            RBRACE [3:1..3:2)
            WHITE_SPACE [3:2..5:1)
            KDoc [5:1..8:4)
                KDOC_START [5:1..5:4)
                WHITE_SPACE [5:4..6:2)
                KDOC_LEADING_ASTERISK [6:2..6:3)
                KDOC_TEXT [6:3..6:4)
                KDOC_TAG_NAME [6:4..6:10)
                WHITE_SPACE [6:10..6:11)
                KDOC_MARKDOWN_LINK [6:11..6:14)
                WHITE_SPACE [6:14..6:15)
                KDOC_TEXT [6:15..6:30)
                WHITE_SPACE [6:30..7:2)
                KDOC_LEADING_ASTERISK [7:2..7:3)
                KDOC_TEXT [7:3..7:4)
                KDOC_TAG_NAME [7:4..7:11)
                WHITE_SPACE [7:11..7:12)
                KDOC_MARKDOWN_LINK [7:12..7:23)
                WHITE_SPACE [7:23..8:2)
                KDOC_END [8:2..8:4)
            WHITE_SPACE [8:4..9:1)
            fun [9:1..9:4)
            WHITE_SPACE [9:4..9:5)
            IDENTIFIER [9:5..9:9)
            LPAR [9:9..9:10)
            IDENTIFIER [9:10..9:11)
            COLON [9:11..9:12)
            WHITE_SPACE [9:12..9:13)
            IDENTIFIER [9:13..9:19)
            RPAR [9:19..9:20)
            WHITE_SPACE [9:20..9:21)
            LBRACE [9:21..9:22)
            WHITE_SPACE [9:22..10:5)
            val [10:5..10:8)
            WHITE_SPACE [10:8..10:9)
            IDENTIFIER [10:9..10:21)
            WHITE_SPACE [10:21..10:22)
            EQ [10:22..10:23)
            WHITE_SPACE [10:23..10:24)
            BAD_CHARACTER [10:24..10:25)
            WHITE_SPACE [10:25..11:5)
            throw [11:5..11:10)
            WHITE_SPACE [11:10..11:11)
            IDENTIFIER [11:11..11:20)
            LPAR [11:20..11:21)
            RPAR [11:21..11:22)
            WHITE_SPACE [11:22..12:1)
            RBRACE [12:1..12:2)
        """.trimIndent(), lexer.tokenize(kotlinCodeSample).dump(kotlinCodeSample.toSourceLinesMapping()))
    }

    @Test
    fun testLexerOnTestData() {
        var filesCounter = 0
        var oldLexerTotalNanos = 0L
        var newLexerTotalNanos = 0L
        var totalCharsNumber = 0L
        var totalLinesNumber = 0L
        var totalTokensNumber = 0L

        TestDataUtils.checkKotlinFiles { data, path, sourceLinesMapping ->
            val (oldLexerNanos, newLexerNanos, tokensNumber) = checkLexerOnKotlinCode(data, path)
            oldLexerTotalNanos += oldLexerNanos
            newLexerTotalNanos += newLexerNanos
            filesCounter++
            totalCharsNumber += data.length
            totalLinesNumber += sourceLinesMapping.linesCount
            totalTokensNumber += tokensNumber
        }

        val newOldLexerTimeRatio = newLexerTotalNanos.toDouble() / oldLexerTotalNanos

        assertTrue(filesCounter > 31000, "Number of tested files (kt, kts, nkt) should be more than 31K")
        assertEquals(newOldLexerTimeRatio, 1.0, 0.2, "Lexers performance should be almost equal")

        println("Number of tested files (kt, kts, nkt): $filesCounter")
        println("Number of chars: $totalCharsNumber")
        println("Number of lines: $totalLinesNumber")
        println("Number of tokens: $totalTokensNumber")
        println("Old lexer total time: ${TimeUnit.NANOSECONDS.toMillis(oldLexerTotalNanos)} ms")
        println("New lexer total time: ${TimeUnit.NANOSECONDS.toMillis(newLexerTotalNanos)} ms")
        println("New/Old lexer time ratio: %.4f".format(newOldLexerTimeRatio))
    }

    private fun checkLexerOnKotlinCode(kotlinCodeSample: String, path: Path? = null): LexerStats {
        val oldLexer = OldLexer()

        val oldLexerStartNanos = System.nanoTime()
        val oldTokens = oldLexer.tokenize(kotlinCodeSample)
        val oldLexerNanos = System.nanoTime() - oldLexerStartNanos

        val newLexer = NewLexer()

        val newLexerStartNanos = System.nanoTime()
        val newTokens = newLexer.tokenize(kotlinCodeSample)
        val newLexerNanos = System.nanoTime() - newLexerStartNanos

        fun failWithDifferentTokens() {
            assertEquals(oldTokens.dump(), newTokens.dump(), path?.let { "Different tokens on file: $it" })
        }

        var tokensNumber = 0L

        fun compareTokens(oldTokens: List<Token<*>>, newTokens: List<Token<*>>) {
            if (oldTokens.size != newTokens.size) {
                failWithDifferentTokens()
            }

            for (index in oldTokens.indices) {
                val oldToken = oldTokens[index]
                val newToken = newTokens[index]
                tokensNumber++

                if (oldToken.name != newToken.name ||
                    oldToken.start != newToken.start ||
                    oldToken.end != newToken.end
                ) {
                    failWithDifferentTokens()
                }

                if (oldToken is MultiToken<*>) {
                    require(newToken is MultiToken<*>)
                    compareTokens(oldToken.children, newToken.children)
                }
            }
        }

        compareTokens(oldTokens, newTokens)

        return LexerStats(oldLexerNanos, newLexerNanos, tokensNumber)
    }

    private data class LexerStats(
        val oldNanos: Long,
        val newNanos: Long,
        val tokensNumber: Long,
    )
}