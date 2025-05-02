/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import fleet.com.intellij.platform.syntax.SyntaxElementType
import fleet.com.intellij.platform.syntax.util.lexer.LexerBase
import org.jetbrains.kotlin.kmp.lexer.KDocLexer
import org.jetbrains.kotlin.kmp.lexer.KDocTokens
import org.jetbrains.kotlin.kmp.lexer.KotlinLexer
import org.jetbrains.kotlin.kmp.lexer.KtTokens

class NewLexer : AbstractLexer<SyntaxElementType>() {
    override fun tokenize(text: String): List<Token<SyntaxElementType>> {
        return tokenizeSubsequence(text, 0, KotlinLexer())
    }

    private fun tokenizeSubsequence(charSequence: CharSequence, start: Int, lexer: LexerBase): List<Token<SyntaxElementType>> {
        return buildList {
            lexer.start(charSequence)
            var tokenType = lexer.getTokenType()
            while (tokenType != null) {
                val mainTokenStart = lexer.getTokenStart() + start
                val mainTokenEnd = lexer.getTokenEnd() + start

                val token = when (tokenType) {
                    KtTokens.DOC_COMMENT,
                    KDocTokens.MARKDOWN_LINK
                        -> {
                        val subLexer = if (tokenType == KtTokens.DOC_COMMENT) KDocLexer() else KotlinLexer()
                        val children =
                            tokenizeSubsequence(
                                charSequence.subSequence(lexer.getTokenStart(), lexer.getTokenEnd()),
                                mainTokenStart,
                                subLexer
                            )
                        MultiToken(tokenType.toString(), mainTokenStart, mainTokenEnd, tokenType, children)
                    }
                    else -> {
                        SingleToken(tokenType.toString(), mainTokenStart, mainTokenEnd, tokenType)
                    }
                }
                add(token)

                lexer.advance()
                tokenType = lexer.getTokenType()
            }
        }
    }
}