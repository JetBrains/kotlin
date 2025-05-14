/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.kmp.LexerTests.Companion.initializeLexers
import org.jetbrains.kotlin.kmp.infra.NewParserTestNode
import org.jetbrains.kotlin.kmp.infra.NewTestParser
import org.jetbrains.kotlin.kmp.infra.ParseMode
import org.jetbrains.kotlin.kmp.infra.PsiTestParser
import org.jetbrains.kotlin.kmp.infra.TestParseNode
import org.junit.jupiter.api.Test

abstract class AbstractParserTests<OldParseElement> : AbstractRecognizerTests<
        OldParseElement,
        NewParserTestNode,
        TestParseNode<out OldParseElement>,
        TestParseNode<out NewParserTestNode>
>() {
    abstract val parseMode: ParseMode

    override val recognizerName: String = "parser"

    override val recognizerSyntaxElementName: String = "parse node"

    override fun recognizeNewSyntaxElement(fileName: String, text: String): TestParseNode<out NewParserTestNode> =
        NewTestParser(parseMode).parse(fileName, text)

    @Test
    fun testBlockInsideBlock() {
        checkOnKotlinCode("""fun test() {
    try {
        println("Block inside block")
    } catch (e: Exception) {
    }
}""")
    }

    @Test
    fun testCollapsedEnumModifierToken() {
        checkOnKotlinCode("""enum class Direction {}""")
    }

    @Test
    fun testLambda() {
        checkOnKotlinCode("""val lambda: (Int) -> Unit = { i -> println(i) }""")
    }

    @Test
    fun testLeftBoundElementOnSecondaryConstructor() {
        checkOnKotlinCode("""class A { constructor() { } }""")
    }

    @Test
    fun testAllModifier() {
        checkOnKotlinCode("""annotation class Simple
@all:Simple val x: Int = 42
""")
    }

    @Test
    fun testIncorrectCodeInBlock() {
        checkOnKotlinCode("""fun f1() { f2(= return) }""")
    }

    @Test
    fun testNoErrorElementDuplication() {
        checkOnKotlinCode("""public interface Func<T1, T2 extends CharSequence> { void bar(T1 t1, T2 t2); }""")
    }

    @Test
    fun testDanglingNewline() {
        checkOnKotlinCode("""fun foo() {
    "\
}""")
    }

    @Test
    open fun testDifferentParsingOnLazyBlock() {
        checkOnKotlinCode("""{
try { }
finally

""")
    }

    @Test
    fun testBinaryOperationPrecedences() {
        checkOnKotlinCode("""val x = 1 + 2 * 3""", """kotlin.FILE `val x = 1 + 2 * 3` [1:1..18)
  PACKAGE_DIRECTIVE `` [1:1..1)
  IMPORT_LIST `` [1:1..1)
  PROPERTY `val x = 1 + 2 * 3` [1:1..18)
    val [1:1..4)
    WHITE_SPACE ` ` [1:4..5)
    IDENTIFIER `x` [1:5..6)
    WHITE_SPACE ` ` [1:6..7)
    EQ `=` [1:7..8)
    WHITE_SPACE ` ` [1:8..9)
    BINARY_EXPRESSION `1 + 2 * 3` [1:9..18)
      INTEGER_CONSTANT `1` [1:9..10)
        INTEGER_LITERAL `1` [1:9..10)
      WHITE_SPACE ` ` [1:10..11)
      OPERATION_REFERENCE `+` [1:11..12)
        PLUS `+` [1:11..12)
      WHITE_SPACE ` ` [1:12..13)
      BINARY_EXPRESSION `2 * 3` [1:13..18)
        INTEGER_CONSTANT `2` [1:13..14)
          INTEGER_LITERAL `2` [1:13..14)
        WHITE_SPACE ` ` [1:14..15)
        OPERATION_REFERENCE `*` [1:15..16)
          MUL `*` [1:15..16)
        WHITE_SPACE ` ` [1:16..17)
        INTEGER_CONSTANT `3` [1:17..18)
          INTEGER_LITERAL `3` [1:17..18)""")

        checkOnKotlinCode("""val x = 1 * 2 + 3 * 4""")
        checkOnKotlinCode("""val y = 1 + 2 * 3 + 4""")
    }

    @Test
    fun testNoHigherPrecedenceAfterIs() {
        // Incorrect code, but the `value2` should not be treated as infix function
        checkOnKotlinCode("""when { value is Int value2 is String -> return "" }""")
    }

    @Test
    fun testIsExpressions() {
        checkOnKotlinCode(
            """val y = x is Int <= true""", // Correct syntax: `<=` (COMPARISON) precedence is lower than `is`

            """kotlin.FILE `val y = x is Int <= true` [1:1..25)
  PACKAGE_DIRECTIVE `` [1:1..1)
  IMPORT_LIST `` [1:1..1)
  PROPERTY `val y = x is Int <= true` [1:1..25)
    val [1:1..4)
    WHITE_SPACE ` ` [1:4..5)
    IDENTIFIER `y` [1:5..6)
    WHITE_SPACE ` ` [1:6..7)
    EQ `=` [1:7..8)
    WHITE_SPACE ` ` [1:8..9)
    BINARY_EXPRESSION `x is Int <= true` [1:9..25)
      IS_EXPRESSION `x is Int` [1:9..17)
        REFERENCE_EXPRESSION `x` [1:9..10)
          IDENTIFIER `x` [1:9..10)
        WHITE_SPACE ` ` [1:10..11)
        OPERATION_REFERENCE `is` [1:11..13)
          is [1:11..13)
        WHITE_SPACE ` ` [1:13..14)
        TYPE_REFERENCE `Int` [1:14..17)
          USER_TYPE `Int` [1:14..17)
            REFERENCE_EXPRESSION `Int` [1:14..17)
              IDENTIFIER `Int` [1:14..17)
      WHITE_SPACE ` ` [1:17..18)
      OPERATION_REFERENCE `<=` [1:18..20)
        LTEQ `<=` [1:18..20)
      WHITE_SPACE ` ` [1:20..21)
      BOOLEAN_CONSTANT `true` [1:21..25)
        true [1:21..25)""")

        checkOnKotlinCode(
            """val y = x is Int is Boolean""", // Correct syntax: `is` precedence equals `is`

            """kotlin.FILE `val y = x is Int is Boolean` [1:1..28)
  PACKAGE_DIRECTIVE `` [1:1..1)
  IMPORT_LIST `` [1:1..1)
  PROPERTY `val y = x is Int is Boolean` [1:1..28)
    val [1:1..4)
    WHITE_SPACE ` ` [1:4..5)
    IDENTIFIER `y` [1:5..6)
    WHITE_SPACE ` ` [1:6..7)
    EQ `=` [1:7..8)
    WHITE_SPACE ` ` [1:8..9)
    IS_EXPRESSION `x is Int is Boolean` [1:9..28)
      IS_EXPRESSION `x is Int` [1:9..17)
        REFERENCE_EXPRESSION `x` [1:9..10)
          IDENTIFIER `x` [1:9..10)
        WHITE_SPACE ` ` [1:10..11)
        OPERATION_REFERENCE `is` [1:11..13)
          is [1:11..13)
        WHITE_SPACE ` ` [1:13..14)
        TYPE_REFERENCE `Int` [1:14..17)
          USER_TYPE `Int` [1:14..17)
            REFERENCE_EXPRESSION `Int` [1:14..17)
              IDENTIFIER `Int` [1:14..17)
      WHITE_SPACE ` ` [1:17..18)
      OPERATION_REFERENCE `is` [1:18..20)
        is [1:18..20)
      WHITE_SPACE ` ` [1:20..21)
      TYPE_REFERENCE `Boolean` [1:21..28)
        USER_TYPE `Boolean` [1:21..28)
          REFERENCE_EXPRESSION `Boolean` [1:21..28)
            IDENTIFIER `Boolean` [1:21..28)""")


        checkOnKotlinCode(
            """val y = x is Int .. true""", // Incorrect syntax: `..` (RANGE) precedence higher than `is`

            """kotlin.FILE `val y = x is Int .. true` [1:1..25)
  PACKAGE_DIRECTIVE `` [1:1..1)
  IMPORT_LIST `` [1:1..1)
  PROPERTY `val y = x is Int .. true` [1:1..25)
    val [1:1..4)
    WHITE_SPACE ` ` [1:4..5)
    IDENTIFIER `y` [1:5..6)
    WHITE_SPACE ` ` [1:6..7)
    EQ `=` [1:7..8)
    WHITE_SPACE ` ` [1:8..9)
    IS_EXPRESSION `x is Int` [1:9..17)
      REFERENCE_EXPRESSION `x` [1:9..10)
        IDENTIFIER `x` [1:9..10)
      WHITE_SPACE ` ` [1:10..11)
      OPERATION_REFERENCE `is` [1:11..13)
        is [1:11..13)
      WHITE_SPACE ` ` [1:13..14)
      TYPE_REFERENCE `Int` [1:14..17)
        USER_TYPE `Int` [1:14..17)
          REFERENCE_EXPRESSION `Int` [1:14..17)
            IDENTIFIER `Int` [1:14..17)
    WHITE_SPACE ` ` [1:17..18)
    ERROR_ELEMENT `.. true` [1:18..25)
      RANGE `..` [1:18..20)
      WHITE_SPACE ` ` [1:20..21)
      true [1:21..25)""")
    }

    @Test
    fun testSoftModifierAsInfixFunction() {
        // Incorrect code, but `annotation` should be parsed as infix function
        checkOnKotlinCode("""val x = @Ann() class C annotation y""")
    }

    @Test
    fun testElvis() {
        checkOnKotlinCode("val result = a ?: b ?: c",

            """kotlin.FILE `val result = a ?: b ?: c` [1:1..25)
  PACKAGE_DIRECTIVE `` [1:1..1)
  IMPORT_LIST `` [1:1..1)
  PROPERTY `val result = a ?: b ?: c` [1:1..25)
    val [1:1..4)
    WHITE_SPACE ` ` [1:4..5)
    IDENTIFIER `result` [1:5..11)
    WHITE_SPACE ` ` [1:11..12)
    EQ `=` [1:12..13)
    WHITE_SPACE ` ` [1:13..14)
    BINARY_EXPRESSION `a ?: b ?: c` [1:14..25)
      BINARY_EXPRESSION `a ?: b` [1:14..20)
        REFERENCE_EXPRESSION `a` [1:14..15)
          IDENTIFIER `a` [1:14..15)
        WHITE_SPACE ` ` [1:15..16)
        OPERATION_REFERENCE `?:` [1:16..18)
          ELVIS `?:` [1:16..18)
        WHITE_SPACE ` ` [1:18..19)
        REFERENCE_EXPRESSION `b` [1:19..20)
          IDENTIFIER `b` [1:19..20)
      WHITE_SPACE ` ` [1:20..21)
      OPERATION_REFERENCE `?:` [1:21..23)
        ELVIS `?:` [1:21..23)
      WHITE_SPACE ` ` [1:23..24)
      REFERENCE_EXPRESSION `c` [1:24..25)
        IDENTIFIER `c` [1:24..25)""")
    }

    @Test
    fun testErrorRecoveryOnComplexExampleWithIsExpression() {
        checkOnKotlinCode("""val x = if (z == y > a is A / 1)""")
    }
}

abstract class AbstractParserTestsWithPsi : AbstractParserTests<PsiElement>() {
    companion object {
        init {
            // Make sure the static declarations are initialized before time measurements to get more refined results
            initializeLexers()
            initializeParsers()
        }

        fun initializeParsers() {
            org.jetbrains.kotlin.kdoc.parser.KDocElementTypes.KDOC_SECTION
            org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes.CLASS
            org.jetbrains.kotlin.KtNodeTypes.KT_FILE

            org.jetbrains.kotlin.kmp.parser.KDocParseNodes.KDOC_SECTION
            org.jetbrains.kotlin.kmp.parser.KtNodeTypes.KT_FILE
        }
    }

    override fun recognizeOldSyntaxElement(fileName: String, text: String): TestParseNode<out PsiElement> =
        PsiTestParser(parseMode).parse(fileName, text)

    override val oldRecognizerSuffix: String = " (PSI)"
}