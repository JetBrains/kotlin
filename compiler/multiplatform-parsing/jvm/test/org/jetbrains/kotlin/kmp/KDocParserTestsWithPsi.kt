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
 */""")
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
        checkOnKotlinCode(TestData.IDENTIFIER_WITH_BACKTICKS_IN_KDOC, """KDoc [1:1..17:4)
  KDOC_START `/**` [1:1..4)
  WHITE_SPACE [1:4..2:2)
  KDOC_SECTION [2:2..16:50)
    KDOC_LEADING_ASTERISK `*` [2:2..3)
    KDOC_CODE_BLOCK_TEXT `                        // Resolved?` [2:3..39)
    WHITE_SPACE [2:39..3:2)
    KDOC_LEADING_ASTERISK `*` [3:2..3)
    KDOC_TEXT ` [`top level`]          // Expect: ✅; Actual: ❌` [3:3..50)
    WHITE_SPACE [3:50..4:2)
    KDOC_LEADING_ASTERISK `*` [4:2..3)
    KDOC_TEXT ` [top level]            // Expect: ❌; Actual: ❌` [4:3..50)
    WHITE_SPACE [4:50..5:2)
    KDOC_LEADING_ASTERISK `*` [5:2..3)
    KDOC_TEXT ` [O.with space]         // Expect: ❌; Actual: ❌` [5:3..50)
    WHITE_SPACE [5:50..6:2)
    KDOC_LEADING_ASTERISK `*` [6:2..3)
    KDOC_TEXT ` [O.`with space`]       // Expect: ✅; Actual: ❌` [6:3..50)
    WHITE_SPACE [6:50..7:2)
    KDOC_LEADING_ASTERISK `*` [7:2..3)
    KDOC_TEXT ` ` [7:3..4)
    KDOC_TAG `@see O.with space      // Expect: ❌; Actual: ❌` [7:4..50)
      KDOC_TAG_NAME `@see` [7:4..8)
      WHITE_SPACE ` ` [7:8..9)
      KDOC_MARKDOWN_LINK `O.with` [7:9..15)
        KDOC_NAME `O.with` [7:9..15)
          KDOC_NAME `O` [7:9..10)
            IDENTIFIER `O` [7:9..10)
          DOT `.` [7:10..11)
          IDENTIFIER `with` [7:11..15)
      WHITE_SPACE ` ` [7:15..16)
      KDOC_TEXT `space      // Expect: ❌; Actual: ❌` [7:16..50)
    WHITE_SPACE [7:50..8:2)
    KDOC_LEADING_ASTERISK `*` [8:2..3)
    KDOC_TEXT ` ` [8:3..4)
    KDOC_TAG [8:4..10:50)
      KDOC_TAG_NAME `@see` [8:4..8)
      WHITE_SPACE ` ` [8:8..9)
      KDOC_MARKDOWN_LINK `O.` [8:9..11)
        KDOC_NAME `O` [8:9..10)
          IDENTIFIER `O` [8:9..10)
        DOT `.` [8:10..11)
        ERROR_ELEMENT `` [8:11..11)
      KDOC_TEXT ``with space`    // Expect: ✅; Actual: ❌` [8:11..50)
      WHITE_SPACE [8:50..9:2)
      KDOC_LEADING_ASTERISK `*` [9:2..3)
      KDOC_TEXT ` ` [9:3..4)
      KDOC_MARKDOWN_LINK `[O.]` [9:4..8)
        LBRACKET `[` [9:4..5)
        KDOC_NAME `O` [9:5..6)
          IDENTIFIER `O` [9:5..6)
        DOT `.` [9:6..7)
        ERROR_ELEMENT `` [9:7..7)
        RBRACKET `]` [9:7..8)
      KDOC_TEXT `]                  // Expect: ❌; Actual: ❌` [9:8..50)
      WHITE_SPACE [9:50..10:2)
      KDOC_LEADING_ASTERISK `*` [10:2..3)
      KDOC_TEXT ` [O.`]`]                // Expect: ✅; Actual: ❌` [10:3..50)
    WHITE_SPACE [10:50..11:2)
    KDOC_LEADING_ASTERISK `*` [11:2..3)
    KDOC_TEXT ` ` [11:3..4)
    KDOC_TAG `@see O.]               // Expect: ❌; Actual: ❌` [11:4..50)
      KDOC_TAG_NAME `@see` [11:4..8)
      WHITE_SPACE ` ` [11:8..9)
      KDOC_MARKDOWN_LINK `O.` [11:9..11)
        KDOC_NAME `O` [11:9..10)
          IDENTIFIER `O` [11:9..10)
        DOT `.` [11:10..11)
        ERROR_ELEMENT `` [11:11..11)
      KDOC_TEXT `]               // Expect: ❌; Actual: ❌` [11:11..50)
    WHITE_SPACE [11:50..12:2)
    KDOC_LEADING_ASTERISK `*` [12:2..3)
    KDOC_TEXT ` ` [12:3..4)
    KDOC_TAG [12:4..14:50)
      KDOC_TAG_NAME `@see` [12:4..8)
      WHITE_SPACE ` ` [12:8..9)
      KDOC_MARKDOWN_LINK `O.` [12:9..11)
        KDOC_NAME `O` [12:9..10)
          IDENTIFIER `O` [12:9..10)
        DOT `.` [12:10..11)
        ERROR_ELEMENT `` [12:11..11)
      KDOC_TEXT ``]`             // Expect: ✅; Actual: ❌` [12:11..50)
      WHITE_SPACE [12:50..13:2)
      KDOC_LEADING_ASTERISK `*` [13:2..3)
      KDOC_TEXT ` ` [13:3..4)
      KDOC_MARKDOWN_LINK `[O.without_space]` [13:4..21)
        LBRACKET `[` [13:4..5)
        KDOC_NAME `O.without_space` [13:5..20)
          KDOC_NAME `O` [13:5..6)
            IDENTIFIER `O` [13:5..6)
          DOT `.` [13:6..7)
          IDENTIFIER `without_space` [13:7..20)
        RBRACKET `]` [13:20..21)
      KDOC_TEXT `      // Expect: ✅; Actual: ✅` [13:21..50)
      WHITE_SPACE [13:50..14:2)
      KDOC_LEADING_ASTERISK `*` [14:2..3)
      KDOC_TEXT ` [O.`without_space`]    // Expect: ✅; Actual: ❌` [14:3..50)
    WHITE_SPACE [14:50..15:2)
    KDOC_LEADING_ASTERISK `*` [15:2..3)
    KDOC_TEXT ` ` [15:3..4)
    KDOC_TAG `@see O.without_space   // Expect: ✅; Actual: ✅` [15:4..50)
      KDOC_TAG_NAME `@see` [15:4..8)
      WHITE_SPACE ` ` [15:8..9)
      KDOC_MARKDOWN_LINK `O.without_space` [15:9..24)
        KDOC_NAME `O.without_space` [15:9..24)
          KDOC_NAME `O` [15:9..10)
            IDENTIFIER `O` [15:9..10)
          DOT `.` [15:10..11)
          IDENTIFIER `without_space` [15:11..24)
      WHITE_SPACE `   ` [15:24..27)
      KDOC_TEXT `// Expect: ✅; Actual: ✅` [15:27..50)
    WHITE_SPACE [15:50..16:2)
    KDOC_LEADING_ASTERISK `*` [16:2..3)
    KDOC_TEXT ` ` [16:3..4)
    KDOC_TAG `@see O.`without_space` // Expect: ✅; Actual: ❌` [16:4..50)
      KDOC_TAG_NAME `@see` [16:4..8)
      WHITE_SPACE ` ` [16:8..9)
      KDOC_MARKDOWN_LINK `O.` [16:9..11)
        KDOC_NAME `O` [16:9..10)
          IDENTIFIER `O` [16:9..10)
        DOT `.` [16:10..11)
        ERROR_ELEMENT `` [16:11..11)
      KDOC_TEXT ``without_space` // Expect: ✅; Actual: ❌` [16:11..50)
  WHITE_SPACE [16:50..17:2)
  KDOC_END `*/` [17:2..4)""")
    }
}