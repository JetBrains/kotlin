/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kmp.infra.NewLexer
import org.jetbrains.kotlin.kmp.infra.OldLexer
import org.jetbrains.kotlin.kmp.infra.TestDataUtils
import org.jetbrains.kotlin.kmp.infra.dump
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.test.assertEquals

class LexerTests {
    val kotlinCodeSample = """
            fun main() {
                println("Hello, World!")
            }
        """.trimIndent()

    @Test
    fun testSimple() {
        checkLexerOnKotlinCode(kotlinCodeSample)
    }

    @Test
    fun testTokenDump() {
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
        """.trimIndent(), NewLexer().tokenize(kotlinCodeSample).dump())
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

        if (oldTokens.size != newTokens.size) {
            failWithDifferentTokens()
        }

        for (i in oldTokens.indices) {
            val oldToken = oldTokens[i]
            val newToken = newTokens[i]

            if (oldToken.name != newToken.name ||
                oldToken.start != newToken.start ||
                oldToken.end != newToken.end
            ) {
                failWithDifferentTokens()
            }
        }
    }
}