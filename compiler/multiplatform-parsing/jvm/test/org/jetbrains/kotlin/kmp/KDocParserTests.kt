/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.kmp.parser.KDocParseNodes
import org.junit.jupiter.api.Test

class KDocParserTests : AbstractParserTests() {
    companion object {
        init {
            // Make sure the static declarations are initialized before time measurements to get more refined results
            LexerTests.initializeLexers()
            initializeKDocParsers()
        }

        fun initializeKDocParsers() {
            KDocElementTypes.KDOC_SECTION

            KDocParseNodes.KDOC_SECTION
        }
    }

    override val kDocOnly: Boolean = true

    override val expectedExampleDump: String = """KDoc [7:1..10:4)
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
  KDOC_END `*/` [10:2..4)"""

    override val expectedExampleSyntaxElementsNumber: Long = 32

    override val expectedDumpOnWindowsNewLine: String = ""

    override val printOldRecognizerTimeInfo: Boolean = false

    @Test
    fun testMarkdownLinkWithError() {
        checkOnKotlinCode(
            """/**
 * [A.B.C...]
 * [....]
 * [A..B..C]
 * [A.]
 */""")
    }
}