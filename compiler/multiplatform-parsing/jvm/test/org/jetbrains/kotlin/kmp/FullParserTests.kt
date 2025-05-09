/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import org.jetbrains.kotlin.kmp.LexerTests.Companion.initializeLexers
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class FullParserTests : AbstractParserTests() {
    companion object {
        init {
            // Make sure the static declarations are initialized before time measurements to get more refined results
            initializeLexers()
            KDocParserTests.initializeKDocParsers()
            initializeParsers()
        }

        fun initializeParsers() {
            org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.CLASS
            org.jetbrains.kotlin.KtNodeTypes.KT_FILE
        }
    }

    override val kDocOnly: Boolean = false

    override val expectedExampleDump: String = """kotlin.FILE [1:1..14:2)
  PACKAGE_DIRECTIVE `` [1:1..1)
  IMPORT_LIST `` [1:1..1)
  FUN [1:1..3:2)
    fun [1:1..4)
    WHITE_SPACE ` ` [1:4..5)
    IDENTIFIER `main` [1:5..9)
    VALUE_PARAMETER_LIST `()` [1:9..11)
      LPAR `(` [1:9..10)
      RPAR `)` [1:10..11)
    WHITE_SPACE ` ` [1:11..12)
    BLOCK [1:12..3:2)
      LBRACE `{` [1:12..13)
      WHITE_SPACE [1:13..2:5)
      CALL_EXPRESSION `println("Hello, World!")` [2:5..29)
        REFERENCE_EXPRESSION `println` [2:5..12)
          IDENTIFIER `println` [2:5..12)
        VALUE_ARGUMENT_LIST `("Hello, World!")` [2:12..29)
          LPAR `(` [2:12..13)
          VALUE_ARGUMENT `"Hello, World!"` [2:13..28)
            STRING_TEMPLATE `"Hello, World!"` [2:13..28)
              OPEN_QUOTE `"` [2:13..14)
              LITERAL_STRING_TEMPLATE_ENTRY `Hello, World!` [2:14..27)
                REGULAR_STRING_PART `Hello, World!` [2:14..27)
              CLOSING_QUOTE `"` [2:27..28)
          RPAR `)` [2:28..29)
      WHITE_SPACE [2:29..3:1)
      RBRACE `}` [3:1..2)
  WHITE_SPACE [3:2..5:1)
  CLASS `class C(val x: Int)` [5:1..20)
    class [5:1..6)
    WHITE_SPACE ` ` [5:6..7)
    IDENTIFIER `C` [5:7..8)
    PRIMARY_CONSTRUCTOR `(val x: Int)` [5:8..20)
      VALUE_PARAMETER_LIST `(val x: Int)` [5:8..20)
        LPAR `(` [5:8..9)
        VALUE_PARAMETER `val x: Int` [5:9..19)
          val [5:9..12)
          WHITE_SPACE ` ` [5:12..13)
          IDENTIFIER `x` [5:13..14)
          COLON `:` [5:14..15)
          WHITE_SPACE ` ` [5:15..16)
          TYPE_REFERENCE `Int` [5:16..19)
            USER_TYPE `Int` [5:16..19)
              REFERENCE_EXPRESSION `Int` [5:16..19)
                IDENTIFIER `Int` [5:16..19)
        RPAR `)` [5:19..20)
  WHITE_SPACE [5:20..7:1)
  FUN [7:1..14:2)
    KDoc [7:1..10:4)
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
      KDOC_END `*/` [10:2..4)
    WHITE_SPACE [10:4..11:1)
    fun [11:1..4)
    WHITE_SPACE ` ` [11:4..5)
    IDENTIFIER `test` [11:5..9)
    VALUE_PARAMETER_LIST `(p: String)` [11:9..20)
      LPAR `(` [11:9..10)
      VALUE_PARAMETER `p: String` [11:10..19)
        IDENTIFIER `p` [11:10..11)
        COLON `:` [11:11..12)
        WHITE_SPACE ` ` [11:12..13)
        TYPE_REFERENCE `String` [11:13..19)
          USER_TYPE `String` [11:13..19)
            REFERENCE_EXPRESSION `String` [11:13..19)
              IDENTIFIER `String` [11:13..19)
      RPAR `)` [11:19..20)
    WHITE_SPACE ` ` [11:20..21)
    BLOCK [11:21..14:2)
      LBRACE `{` [11:21..22)
      WHITE_SPACE [11:22..12:5)
      PROPERTY `val badCharacter =` [12:5..23)
        val [12:5..8)
        WHITE_SPACE ` ` [12:8..9)
        IDENTIFIER `badCharacter` [12:9..21)
        WHITE_SPACE ` ` [12:21..22)
        EQ `=` [12:22..23)
        ERROR_ELEMENT `` [12:23..23)
      WHITE_SPACE ` ` [12:23..24)
      ERROR_ELEMENT `^` [12:24..25)
        BAD_CHARACTER `^` [12:24..25)
      WHITE_SPACE [12:25..13:5)
      THROW `throw Exception()` [13:5..22)
        throw [13:5..10)
        WHITE_SPACE ` ` [13:10..11)
        CALL_EXPRESSION `Exception()` [13:11..22)
          REFERENCE_EXPRESSION `Exception` [13:11..20)
            IDENTIFIER `Exception` [13:11..20)
          VALUE_ARGUMENT_LIST `()` [13:20..22)
            LPAR `(` [13:20..21)
            RPAR `)` [13:21..22)
      WHITE_SPACE [13:22..14:1)
      RBRACE `}` [14:1..2)"""

    override val expectedExampleSyntaxElementsNumber: Long = 40

    @Test
    @Disabled("TODO: implement KT-77144")
    override fun testSimple() {
    }

    @Test
    @Disabled("TODO: implement KT-77144")
    override fun testEmpty() {
    }

    @Test
    @Disabled("TODO: implement KT-77144")
    override fun testOnTestData() {
    }
}