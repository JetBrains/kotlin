/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.kmp.infra.LightTreeTestParser
import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.jetbrains.kotlin.kmp.infra.TestParseNode

class FullParserTestsWithLightTree : AbstractParserTests<LighterASTNode>() {
    init {
        // Make sure the static declarations are initialized before time measurements to get more refined results
        LightTreeTestParser.environment
    }

    override val parseMode: ParseMode = ParseMode.NoKDoc

    override fun recognizeOldSyntaxElement(fileName: String, text: String): TestParseNode<LighterASTNode> =
        LightTreeTestParser().parse(fileName, text)

    override val oldRecognizerSuffix: String = " (LightTree)"

    // No KDoc expanding
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

    override val expectedExampleSyntaxElementsNumber: Long = 91

    override val expectedEmptySyntaxElementsNumber: Long = 3

    override val expectedDumpOnWindowsNewLine: String = """kotlin.FILE [1:1..2:1)
  PACKAGE_DIRECTIVE `` [1:1..1)
  IMPORT_LIST `` [1:1..1)
  ERROR_ELEMENT [1:1..2)
    BAD_CHARACTER [1:1..2)
  WHITE_SPACE [1:2..2:1)"""
}