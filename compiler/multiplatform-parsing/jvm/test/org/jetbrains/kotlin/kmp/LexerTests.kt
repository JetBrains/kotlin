/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kmp.infra.AbstractTestLexer
import org.jetbrains.kotlin.kmp.infra.NewTestLexer
import org.jetbrains.kotlin.kmp.infra.OldTestLexer
import org.jetbrains.kotlin.kmp.infra.TestDataUtils
import org.jetbrains.kotlin.kmp.infra.compareSyntaxElements
import org.jetbrains.kotlin.toSourceLinesMapping
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LexerTests {
    companion object {
        init {
            // Make sure the static declarations are initialized before time measurements to get more refined results
            initializeLexers()
        }

        fun initializeLexers() {
            org.jetbrains.kotlin.lexer.KtTokens.EOF
            org.jetbrains.kotlin.kdoc.lexer.KDocTokens.START

            org.jetbrains.kotlin.kmp.lexer.KtTokens.EOF
            org.jetbrains.kotlin.kmp.lexer.KDocTokens.START
        }
    }

    @Test
    fun testSimple() {
        val (_, _, tokensNumber, linesCount) = checkLexerOnKotlinCode(kotlinCodeSample)
        assertEquals(14, linesCount)
        assertEquals(83, tokensNumber)
    }

    @Test
    fun testEmpty() {
        val (_, _, tokensNumber, linesCount) = checkLexerOnKotlinCode("")
        assertEquals(1, linesCount)
        assertEquals(0, tokensNumber)
    }

    @Test
    fun testOldTokensDump() = testTokensDump(OldTestLexer())

    @Test
    fun testNewTokensDump() = testTokensDump(NewTestLexer())

    private fun testTokensDump(lexer: AbstractTestLexer<*>) {
        assertEquals("""fun [1:1..4)
WHITE_SPACE ` ` [1:4..5)
IDENTIFIER `main` [1:5..9)
LPAR `(` [1:9..10)
RPAR `)` [1:10..11)
WHITE_SPACE ` ` [1:11..12)
LBRACE `{` [1:12..13)
WHITE_SPACE [1:13..2:5)
IDENTIFIER `println` [2:5..12)
LPAR `(` [2:12..13)
OPEN_QUOTE `"` [2:13..14)
REGULAR_STRING_PART `Hello, World!` [2:14..27)
CLOSING_QUOTE `"` [2:27..28)
RPAR `)` [2:28..29)
WHITE_SPACE [2:29..3:1)
RBRACE `}` [3:1..2)
WHITE_SPACE [3:2..5:1)
class [5:1..6)
WHITE_SPACE ` ` [5:6..7)
IDENTIFIER `C` [5:7..8)
LPAR `(` [5:8..9)
val [5:9..12)
WHITE_SPACE ` ` [5:12..13)
IDENTIFIER `x` [5:13..14)
COLON `:` [5:14..15)
WHITE_SPACE ` ` [5:15..16)
IDENTIFIER `Int` [5:16..19)
RPAR `)` [5:19..20)
WHITE_SPACE [5:20..7:1)
KDoc [7:1..10:4)
  KDOC_START `/**` [7:1..4)
  WHITE_SPACE [7:4..8:2)
  KDOC_LEADING_ASTERISK `*` [8:2..3)
  KDOC_TEXT ` ` [8:3..4)
  KDOC_TAG_NAME `@param` [8:4..10)
  WHITE_SPACE ` ` [8:10..11)
  KDOC_MARKDOWN_LINK `[C.x]` [8:11..16)
    LBRACKET `[` [8:11..12)
    IDENTIFIER `C` [8:12..13)
    DOT `.` [8:13..14)
    IDENTIFIER `x` [8:14..15)
    RBRACKET `]` [8:15..16)
  WHITE_SPACE ` ` [8:16..17)
  KDOC_TEXT `Some parameter.` [8:17..32)
  WHITE_SPACE [8:32..9:2)
  KDOC_LEADING_ASTERISK `*` [9:2..3)
  KDOC_TEXT ` ` [9:3..4)
  KDOC_TAG_NAME `@return` [9:4..11)
  WHITE_SPACE ` ` [9:11..12)
  KDOC_MARKDOWN_LINK `[Exception]` [9:12..23)
    LBRACKET `[` [9:12..13)
    IDENTIFIER `Exception` [9:13..22)
    RBRACKET `]` [9:22..23)
  WHITE_SPACE [9:23..10:2)
  KDOC_END `*/` [10:2..4)
WHITE_SPACE [10:4..11:1)
fun [11:1..4)
WHITE_SPACE ` ` [11:4..5)
IDENTIFIER `test` [11:5..9)
LPAR `(` [11:9..10)
IDENTIFIER `p` [11:10..11)
COLON `:` [11:11..12)
WHITE_SPACE ` ` [11:12..13)
IDENTIFIER `String` [11:13..19)
RPAR `)` [11:19..20)
WHITE_SPACE ` ` [11:20..21)
LBRACE `{` [11:21..22)
WHITE_SPACE [11:22..12:5)
val [12:5..8)
WHITE_SPACE ` ` [12:8..9)
IDENTIFIER `badCharacter` [12:9..21)
WHITE_SPACE ` ` [12:21..22)
EQ `=` [12:22..23)
WHITE_SPACE ` ` [12:23..24)
BAD_CHARACTER `^` [12:24..25)
WHITE_SPACE [12:25..13:5)
throw [13:5..10)
WHITE_SPACE ` ` [13:10..11)
IDENTIFIER `Exception` [13:11..20)
LPAR `(` [13:20..21)
RPAR `)` [13:21..22)
WHITE_SPACE [13:22..14:1)
RBRACE `}` [14:1..2)""", lexer.tokenize(kotlinCodeSample).dump(kotlinCodeSample.toSourceLinesMapping(), kotlinCodeSample))
    }

    @Test
    fun testLexerOnTestData() {
        var filesCounter = 0
        var oldLexerTotalNanos = 0L
        var newLexerTotalNanos = 0L
        var totalCharsNumber = 0L
        var totalLinesNumber = 0L
        var totalTokensNumber = 0L

        TestDataUtils.checkKotlinFiles { data, path ->
            val (oldLexerNanos, newLexerNanos, tokensNumber, linesCount) = checkLexerOnKotlinCode(data, path)
            oldLexerTotalNanos += oldLexerNanos
            newLexerTotalNanos += newLexerNanos
            filesCounter++
            totalCharsNumber += data.length
            totalTokensNumber += tokensNumber
            totalLinesNumber += linesCount
        }

        val newOldLexerTimeRatio = newLexerTotalNanos.toDouble() / oldLexerTotalNanos

        assertTrue(filesCounter > 31000, "Number of tested files (kt, kts, nkt) should be more than 31K")

        println("Number of tested files (kt, kts, nkt): $filesCounter")
        println("Number of chars: $totalCharsNumber")
        println("Number of lines: $totalLinesNumber")
        println("Number of tokens: $totalTokensNumber")
        println("Old lexer total time: ${TimeUnit.NANOSECONDS.toMillis(oldLexerTotalNanos)} ms")
        println("New lexer total time: ${TimeUnit.NANOSECONDS.toMillis(newLexerTotalNanos)} ms")
        println("New/Old lexer time ratio: %.4f".format(newOldLexerTimeRatio))
    }

    private fun checkLexerOnKotlinCode(kotlinCodeSample: String, path: Path? = null): LexerStats {
        val sourceLinesMapping = kotlinCodeSample.toSourceLinesMapping()

        val oldLexer = OldTestLexer()

        val oldLexerStartNanos = System.nanoTime()
        val oldToken = oldLexer.tokenize(kotlinCodeSample)
        val oldLexerNanos = System.nanoTime() - oldLexerStartNanos

        val newLexer = NewTestLexer()

        val newLexerStartNanos = System.nanoTime()
        val newToken = newLexer.tokenize(kotlinCodeSample)
        val newLexerNanos = System.nanoTime() - newLexerStartNanos

        val tokensNumber = compareSyntaxElements(oldToken, newToken) {
            assertEquals(
                oldToken.dump(sourceLinesMapping, kotlinCodeSample),
                newToken.dump(sourceLinesMapping, kotlinCodeSample),
                path?.let { "Different tokens on file: $it" }
            )
        }

        return LexerStats(oldLexerNanos, newLexerNanos, tokensNumber, sourceLinesMapping.linesCount)
    }

    private data class LexerStats(
        val oldNanos: Long,
        val newNanos: Long,
        val tokensNumber: Long,
        val linesCount: Int,
    )
}