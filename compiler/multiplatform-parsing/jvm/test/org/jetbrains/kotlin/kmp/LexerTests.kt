/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp

import com.intellij.psi.tree.IElementType
import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.kotlin.kmp.infra.NewTestLexer
import org.jetbrains.kotlin.kmp.infra.OldTestLexer
import org.jetbrains.kotlin.kmp.infra.TestToken

class LexerTests : AbstractRecognizerTests<IElementType, SyntaxElementType, TestToken<IElementType>, TestToken<SyntaxElementType>>() {
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

    override fun recognizeOldSyntaxElement(fileName: String, text: String): TestToken<IElementType> = OldTestLexer().tokenize(text)
    override fun recognizeNewSyntaxElement(fileName: String, text: String): TestToken<SyntaxElementType> = NewTestLexer().tokenize(text)

    override val recognizerName: String = "lexer"
    override val recognizerSyntaxElementName: String = "token"
}