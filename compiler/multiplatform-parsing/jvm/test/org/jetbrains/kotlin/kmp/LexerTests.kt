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
        
        class C(val x: Int)
        
        /**
         * @param [C.x] Some parameter.
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
            class [5:1..5:6)
            WHITE_SPACE [5:6..5:7)
            IDENTIFIER [5:7..5:8)
            LPAR [5:8..5:9)
            val [5:9..5:12)
            WHITE_SPACE [5:12..5:13)
            IDENTIFIER [5:13..5:14)
            COLON [5:14..5:15)
            WHITE_SPACE [5:15..5:16)
            IDENTIFIER [5:16..5:19)
            RPAR [5:19..5:20)
            WHITE_SPACE [5:20..7:1)
            KDoc [7:1..10:4)
                KDOC_START [7:1..7:4)
                WHITE_SPACE [7:4..8:2)
                KDOC_LEADING_ASTERISK [8:2..8:3)
                KDOC_TEXT [8:3..8:4)
                KDOC_TAG_NAME [8:4..8:10)
                WHITE_SPACE [8:10..8:11)
                KDOC_MARKDOWN_LINK [8:11..8:16)
                    LBRACKET [8:11..8:12)
                    IDENTIFIER [8:12..8:13)
                    DOT [8:13..8:14)
                    IDENTIFIER [8:14..8:15)
                    RBRACKET [8:15..8:16)
                WHITE_SPACE [8:16..8:17)
                KDOC_TEXT [8:17..8:32)
                WHITE_SPACE [8:32..9:2)
                KDOC_LEADING_ASTERISK [9:2..9:3)
                KDOC_TEXT [9:3..9:4)
                KDOC_TAG_NAME [9:4..9:11)
                WHITE_SPACE [9:11..9:12)
                KDOC_MARKDOWN_LINK [9:12..9:23)
                    LBRACKET [9:12..9:13)
                    IDENTIFIER [9:13..9:22)
                    RBRACKET [9:22..9:23)
                WHITE_SPACE [9:23..10:2)
                KDOC_END [10:2..10:4)
            WHITE_SPACE [10:4..11:1)
            fun [11:1..11:4)
            WHITE_SPACE [11:4..11:5)
            IDENTIFIER [11:5..11:9)
            LPAR [11:9..11:10)
            IDENTIFIER [11:10..11:11)
            COLON [11:11..11:12)
            WHITE_SPACE [11:12..11:13)
            IDENTIFIER [11:13..11:19)
            RPAR [11:19..11:20)
            WHITE_SPACE [11:20..11:21)
            LBRACE [11:21..11:22)
            WHITE_SPACE [11:22..12:5)
            val [12:5..12:8)
            WHITE_SPACE [12:8..12:9)
            IDENTIFIER [12:9..12:21)
            WHITE_SPACE [12:21..12:22)
            EQ [12:22..12:23)
            WHITE_SPACE [12:23..12:24)
            BAD_CHARACTER [12:24..12:25)
            WHITE_SPACE [12:25..13:5)
            throw [13:5..13:10)
            WHITE_SPACE [13:10..13:11)
            IDENTIFIER [13:11..13:20)
            LPAR [13:20..13:21)
            RPAR [13:21..13:22)
            WHITE_SPACE [13:22..14:1)
            RBRACE [14:1..14:2)
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