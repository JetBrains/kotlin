/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class KDocParserTestsWithPsi : AbstractParserTestsWithPsi() {
    override val parseMode: ParseMode = ParseMode.KDocOnly

    override val expectedDumpOnWindowsNewLine: String = ""

    override val printOldRecognizerTimeInfo: Boolean = false

    @Disabled("No dump in KDocOnly mode")
    @Test
    override fun testBinaryOperationPrecedences() {
    }

    @Disabled("No dump in KDocOnly mode")
    @Test
    override fun testElvis() {
    }

    @Disabled("No dump in KDocOnly mode")
    @Test
    override fun testIsExpressions() {
    }


    @Test
    fun testIndentedCodeBlocks() {
        checkOnKotlinCode(
            """/**
              * ~~~
              *   Code block
              * ~~~
              *
              * ```
              * Line 1
              *  Line 2
              *   Line 3
              *    Line 4
              * ```
              * 
              */
            fun test() {}""",
            """KDoc [1:1..13:17)
  KDOC_START `/**` [1:1..4)
  WHITE_SPACE [1:4..2:15)
  KDOC_SECTION [2:15..12:17)
    KDOC_LEADING_ASTERISK `*` [2:15..16)
    KDOC_TEXT ` ~~~` [2:16..20)
    WHITE_SPACE [2:20..3:15)
    KDOC_LEADING_ASTERISK `*` [3:15..16)
    KDOC_CODE_BLOCK_TEXT `   Code block` [3:16..29)
    WHITE_SPACE [3:29..4:15)
    KDOC_LEADING_ASTERISK `*` [4:15..16)
    KDOC_TEXT ` ~~~` [4:16..20)
    WHITE_SPACE [4:20..5:15)
    KDOC_LEADING_ASTERISK `*` [5:15..16)
    WHITE_SPACE [5:16..6:15)
    KDOC_LEADING_ASTERISK `*` [6:15..16)
    KDOC_TEXT ` ```` [6:16..20)
    WHITE_SPACE [6:20..7:15)
    KDOC_LEADING_ASTERISK `*` [7:15..16)
    KDOC_CODE_BLOCK_TEXT ` Line 1` [7:16..23)
    WHITE_SPACE [7:23..8:15)
    KDOC_LEADING_ASTERISK `*` [8:15..16)
    KDOC_CODE_BLOCK_TEXT `  Line 2` [8:16..24)
    WHITE_SPACE [8:24..9:15)
    KDOC_LEADING_ASTERISK `*` [9:15..16)
    KDOC_CODE_BLOCK_TEXT `   Line 3` [9:16..25)
    WHITE_SPACE [9:25..10:15)
    KDOC_LEADING_ASTERISK `*` [10:15..16)
    KDOC_CODE_BLOCK_TEXT `    Line 4` [10:16..26)
    WHITE_SPACE [10:26..11:15)
    KDOC_LEADING_ASTERISK `*` [11:15..16)
    KDOC_TEXT ` ```` [11:16..20)
    WHITE_SPACE [11:20..12:15)
    KDOC_LEADING_ASTERISK `*` [12:15..16)
    KDOC_TEXT ` ` [12:16..17)
  WHITE_SPACE [12:17..13:15)
  KDOC_END `*/` [13:15..17)"""
        )
    }
}