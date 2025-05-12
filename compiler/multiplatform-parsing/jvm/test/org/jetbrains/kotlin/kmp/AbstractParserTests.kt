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