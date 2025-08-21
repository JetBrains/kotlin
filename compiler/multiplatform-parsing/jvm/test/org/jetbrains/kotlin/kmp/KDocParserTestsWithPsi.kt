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

    /**
     * KDoc ignores everything except KDoc alongside parsing errors in regular code
     */
    override val expectedExampleContainsSyntaxError: Boolean = false

    @Test
    fun testMarkdownLinkWithError() {
        checkOnKotlinCode("""/**
 * [A.B.C...]
 * [....]
 * [A..B..C]
 * [A.]
 */""",

"""KDoc [1:1..6:4)
  KDOC_START `/**` [1:1..4)
  WHITE_SPACE [1:4..2:2)
  KDOC_SECTION [2:2..5:8)
    KDOC_LEADING_ASTERISK `*` [2:2..3)
    KDOC_TEXT ` ` [2:3..4)
    KDOC_MARKDOWN_LINK `[A.B.C...]` [2:4..14)
      LBRACKET `[` [2:4..5)
      KDOC_NAME `A.B.C` [2:5..10)
        KDOC_NAME `A.B` [2:5..8)
          KDOC_NAME `A` [2:5..6)
            IDENTIFIER `A` [2:5..6)
          DOT `.` [2:6..7)
          IDENTIFIER `B` [2:7..8)
        DOT `.` [2:8..9)
        IDENTIFIER `C` [2:9..10)
      ERROR_ELEMENT `` [2:10..10)
      RESERVED `...` [2:10..13)
      RBRACKET `]` [2:13..14)
    WHITE_SPACE [2:14..3:2)
    KDOC_LEADING_ASTERISK `*` [3:2..3)
    KDOC_TEXT ` [....]` [3:3..10)
    WHITE_SPACE [3:10..4:2)
    KDOC_LEADING_ASTERISK `*` [4:2..3)
    KDOC_TEXT ` ` [4:3..4)
    KDOC_MARKDOWN_LINK `[A..B..C]` [4:4..13)
      LBRACKET `[` [4:4..5)
      KDOC_NAME `A` [4:5..6)
        IDENTIFIER `A` [4:5..6)
      ERROR_ELEMENT `` [4:6..6)
      RANGE `..` [4:6..8)
      IDENTIFIER `B` [4:8..9)
      RANGE `..` [4:9..11)
      IDENTIFIER `C` [4:11..12)
      RBRACKET `]` [4:12..13)
    WHITE_SPACE [4:13..5:2)
    KDOC_LEADING_ASTERISK `*` [5:2..3)
    KDOC_TEXT ` ` [5:3..4)
    KDOC_MARKDOWN_LINK `[A.]` [5:4..8)
      LBRACKET `[` [5:4..5)
      KDOC_NAME `A` [5:5..6)
        IDENTIFIER `A` [5:5..6)
      DOT `.` [5:6..7)
      ERROR_ELEMENT `` [5:7..7)
      RBRACKET `]` [5:7..8)
  WHITE_SPACE [5:8..6:2)
  KDOC_END `*/` [6:2..4)""")
    }

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
    fun testIdentifierWithBackticks() {
        checkOnKotlinCode(TestData.IDENTIFIER_WITH_BACKTICKS_IN_KDOC, $$"""KDoc [1:1..17:4)
  KDOC_START `/**` [1:1..4)
  WHITE_SPACE [1:4..2:2)
  KDOC_SECTION [2:2..16:11)
    KDOC_LEADING_ASTERISK `*` [2:2..3)
    WHITE_SPACE [2:3..3:2)
    KDOC_LEADING_ASTERISK `*` [3:2..3)
    KDOC_TEXT ` ` [3:3..4)
    KDOC_MARKDOWN_LINK `[`top level`]` [3:4..17)
      LBRACKET `[` [3:4..5)
      KDOC_NAME ``top level`` [3:5..16)
        IDENTIFIER ``top level`` [3:5..16)
      RBRACKET `]` [3:16..17)
    WHITE_SPACE [3:17..4:2)
    KDOC_LEADING_ASTERISK `*` [4:2..3)
    KDOC_TEXT ` [top level]` [4:3..15)
    WHITE_SPACE [4:15..5:2)
    KDOC_LEADING_ASTERISK `*` [5:2..3)
    KDOC_TEXT ` [O.with space]` [5:3..18)
    WHITE_SPACE [5:18..6:2)
    KDOC_LEADING_ASTERISK `*` [6:2..3)
    KDOC_TEXT ` ` [6:3..4)
    KDOC_MARKDOWN_LINK `[O.`with space`]` [6:4..20)
      LBRACKET `[` [6:4..5)
      KDOC_NAME `O.`with space`` [6:5..19)
        KDOC_NAME `O` [6:5..6)
          IDENTIFIER `O` [6:5..6)
        DOT `.` [6:6..7)
        IDENTIFIER ``with space`` [6:7..19)
      RBRACKET `]` [6:19..20)
    WHITE_SPACE [6:20..7:2)
    KDOC_LEADING_ASTERISK `*` [7:2..3)
    KDOC_TEXT ` ` [7:3..4)
    KDOC_TAG `@see O.with space` [7:4..21)
      KDOC_TAG_NAME `@see` [7:4..8)
      WHITE_SPACE ` ` [7:8..9)
      KDOC_MARKDOWN_LINK `O.with` [7:9..15)
        KDOC_NAME `O.with` [7:9..15)
          KDOC_NAME `O` [7:9..10)
            IDENTIFIER `O` [7:9..10)
          DOT `.` [7:10..11)
          IDENTIFIER `with` [7:11..15)
      WHITE_SPACE ` ` [7:15..16)
      KDOC_TEXT `space` [7:16..21)
    WHITE_SPACE [7:21..8:2)
    KDOC_LEADING_ASTERISK `*` [8:2..3)
    KDOC_TEXT ` ` [8:3..4)
    KDOC_TAG [8:4..10:23)
      KDOC_TAG_NAME `@see` [8:4..8)
      WHITE_SPACE ` ` [8:8..9)
      KDOC_MARKDOWN_LINK `O.`with space`` [8:9..23)
        KDOC_NAME `O.`with space`` [8:9..23)
          KDOC_NAME `O` [8:9..10)
            IDENTIFIER `O` [8:9..10)
          DOT `.` [8:10..11)
          IDENTIFIER ``with space`` [8:11..23)
      WHITE_SPACE [8:23..9:2)
      KDOC_LEADING_ASTERISK `*` [9:2..3)
      KDOC_TEXT ` ` [9:3..4)
      KDOC_MARKDOWN_LINK `[O.without_space]` [9:4..21)
        LBRACKET `[` [9:4..5)
        KDOC_NAME `O.without_space` [9:5..20)
          KDOC_NAME `O` [9:5..6)
            IDENTIFIER `O` [9:5..6)
          DOT `.` [9:6..7)
          IDENTIFIER `without_space` [9:7..20)
        RBRACKET `]` [9:20..21)
      WHITE_SPACE [9:21..10:2)
      KDOC_LEADING_ASTERISK `*` [10:2..3)
      KDOC_TEXT ` ` [10:3..4)
      KDOC_MARKDOWN_LINK `[O.`without_space`]` [10:4..23)
        LBRACKET `[` [10:4..5)
        KDOC_NAME `O.`without_space`` [10:5..22)
          KDOC_NAME `O` [10:5..6)
            IDENTIFIER `O` [10:5..6)
          DOT `.` [10:6..7)
          IDENTIFIER ``without_space`` [10:7..22)
        RBRACKET `]` [10:22..23)
    WHITE_SPACE [10:23..11:2)
    KDOC_LEADING_ASTERISK `*` [11:2..3)
    KDOC_TEXT ` ` [11:3..4)
    KDOC_TAG `@see O.without_space` [11:4..24)
      KDOC_TAG_NAME `@see` [11:4..8)
      WHITE_SPACE ` ` [11:8..9)
      KDOC_MARKDOWN_LINK `O.without_space` [11:9..24)
        KDOC_NAME `O.without_space` [11:9..24)
          KDOC_NAME `O` [11:9..10)
            IDENTIFIER `O` [11:9..10)
          DOT `.` [11:10..11)
          IDENTIFIER `without_space` [11:11..24)
    WHITE_SPACE [11:24..12:2)
    KDOC_LEADING_ASTERISK `*` [12:2..3)
    KDOC_TEXT ` ` [12:3..4)
    KDOC_TAG [12:4..15:8)
      KDOC_TAG_NAME `@see` [12:4..8)
      WHITE_SPACE ` ` [12:8..9)
      KDOC_MARKDOWN_LINK `O.`without_space`` [12:9..26)
        KDOC_NAME `O.`without_space`` [12:9..26)
          KDOC_NAME `O` [12:9..10)
            IDENTIFIER `O` [12:9..10)
          DOT `.` [12:10..11)
          IDENTIFIER ``without_space`` [12:11..26)
      WHITE_SPACE [12:26..13:2)
      KDOC_LEADING_ASTERISK `*` [13:2..3)
      WHITE_SPACE [13:3..14:2)
      KDOC_LEADING_ASTERISK `*` [14:2..3)
      KDOC_TEXT ` // Resolve incorrect code for completion` [14:3..44)
      WHITE_SPACE [14:44..15:2)
      KDOC_LEADING_ASTERISK `*` [15:2..3)
      KDOC_TEXT ` ` [15:3..4)
      KDOC_MARKDOWN_LINK `[O.]` [15:4..8)
        LBRACKET `[` [15:4..5)
        KDOC_NAME `O` [15:5..6)
          IDENTIFIER `O` [15:5..6)
        DOT `.` [15:6..7)
        ERROR_ELEMENT `` [15:7..7)
        RBRACKET `]` [15:7..8)
    WHITE_SPACE [15:8..16:2)
    KDOC_LEADING_ASTERISK `*` [16:2..3)
    KDOC_TEXT ` ` [16:3..4)
    KDOC_TAG `@see O.` [16:4..11)
      KDOC_TAG_NAME `@see` [16:4..8)
      WHITE_SPACE ` ` [16:8..9)
      KDOC_MARKDOWN_LINK `O.` [16:9..11)
        KDOC_NAME `O` [16:9..10)
          IDENTIFIER `O` [16:9..10)
        DOT `.` [16:10..11)
        ERROR_ELEMENT `` [16:11..11)
  WHITE_SPACE [16:11..17:2)
  KDOC_END `*/` [17:2..4)"""
        )
    }
}