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
import kotlin.test.assertEquals

class LexerTests {
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
        TestDataUtils.checkKotlinFiles { data, path ->
            checkLexerOnKotlinCode(data, path)
            filesCounter++
        }
        println("Number of tested files: $filesCounter") // It should print more than 31K
    }

    private fun checkLexerOnKotlinCode(kotlinCodeSample: String, path: Path? = null) {
        val oldLexer = OldLexer()
        val oldTokens = oldLexer.tokenize(kotlinCodeSample)

        val newLexer = NewLexer()
        val newTokens = newLexer.tokenize(kotlinCodeSample)

        fun failWithDifferentTokens() {
            assertEquals(oldTokens.dump(), newTokens.dump(), path?.let { "Different tokens on file: $it" })
        }

        fun compareTokens(oldTokens: List<Token<*>>, newTokens: List<Token<*>>) {
            if (oldTokens.size != newTokens.size) {
                failWithDifferentTokens()
            }

            for (index in oldTokens.indices) {
                val oldToken = oldTokens[index]
                val newToken = newTokens[index]

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
    }
}