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
            fun [0..3)
            WHITE_SPACE [3..4)
            IDENTIFIER [4..8)
            LPAR [8..9)
            RPAR [9..10)
            WHITE_SPACE [10..11)
            LBRACE [11..12)
            WHITE_SPACE [12..17)
            IDENTIFIER [17..24)
            LPAR [24..25)
            OPEN_QUOTE [25..26)
            REGULAR_STRING_PART [26..39)
            CLOSING_QUOTE [39..40)
            RPAR [40..41)
            WHITE_SPACE [41..42)
            RBRACE [42..43)
            WHITE_SPACE [43..45)
            KDoc [45..105)
                KDOC_START [45..48)
                WHITE_SPACE [48..50)
                KDOC_LEADING_ASTERISK [50..51)
                KDOC_TEXT [51..52)
                KDOC_TAG_NAME [52..58)
                WHITE_SPACE [58..59)
                KDOC_MARKDOWN_LINK [59..62)
                WHITE_SPACE [62..63)
                KDOC_TEXT [63..78)
                WHITE_SPACE [78..80)
                KDOC_LEADING_ASTERISK [80..81)
                KDOC_TEXT [81..82)
                KDOC_TAG_NAME [82..89)
                WHITE_SPACE [89..90)
                KDOC_MARKDOWN_LINK [90..101)
                WHITE_SPACE [101..103)
                KDOC_END [103..105)
            WHITE_SPACE [105..106)
            fun [106..109)
            WHITE_SPACE [109..110)
            IDENTIFIER [110..114)
            LPAR [114..115)
            IDENTIFIER [115..116)
            COLON [116..117)
            WHITE_SPACE [117..118)
            IDENTIFIER [118..124)
            RPAR [124..125)
            WHITE_SPACE [125..126)
            LBRACE [126..127)
            WHITE_SPACE [127..132)
            throw [132..137)
            WHITE_SPACE [137..138)
            IDENTIFIER [138..147)
            LPAR [147..148)
            RPAR [148..149)
            WHITE_SPACE [149..150)
            RBRACE [150..151)

        """.trimIndent(), lexer.tokenize(kotlinCodeSample).dump())
    }

    @Test
    fun testLexerOnTestData() {
        var filesCounter = 0
        var oldLexerTotalNanos = 0L
        var newLexerTotalNanos = 0L
        var totalCharsNumber = 0L
        var totalTokensNumber = 0L

        TestDataUtils.checkKotlinFiles { data, path ->
            val (oldLexerNanos, newLexerNanos, tokensNumber) = checkLexerOnKotlinCode(data, path)
            oldLexerTotalNanos += oldLexerNanos
            newLexerTotalNanos += newLexerNanos
            filesCounter++
            totalCharsNumber += data.length
            totalTokensNumber += tokensNumber
        }

        val newOldLexerTimeRatio = newLexerTotalNanos.toDouble() / oldLexerTotalNanos

        assertTrue(filesCounter > 31000, "Number of tested files (kt, kts, nkt) should be more than 31K")
        assertEquals(newOldLexerTimeRatio, 1.0, 0.2, "Lexers performance should be almost equal")

        println("Number of tested files (kt, kts, nkt): $filesCounter")
        println("Number of chars: $totalCharsNumber")
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