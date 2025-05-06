/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.kdoc.lexer.KDocLexer
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens

class OldTestLexer : AbstractTestLexer<IElementType>() {
    override fun tokenize(text: String): TestToken<IElementType> {
        return tokenizeSubsequence(text, 0, KotlinLexer()).wrap(text.length)
    }

    private fun tokenizeSubsequence(charSequence: CharSequence, start: Int, lexer: LexerBase): List<TestToken<IElementType>> {
        return buildList {
            lexer.start(charSequence)
            var tokenType = lexer.tokenType
            while (tokenType != null) {
                val mainTokenStart = lexer.tokenStart + start
                val mainTokenEnd = lexer.tokenEnd + start

                val children = when (tokenType) {
                    KtTokens.DOC_COMMENT,
                    KDocTokens.MARKDOWN_LINK
                        -> {
                        val subLexer = if (tokenType == KtTokens.DOC_COMMENT) KDocLexer() else KotlinLexer()
                        val subSequence = charSequence.subSequence(lexer.tokenStart, lexer.tokenEnd)
                        tokenizeSubsequence(subSequence, mainTokenStart, subLexer)
                    }
                    else -> {
                        emptyList()
                    }
                }
                add(TestToken(tokenType.toString(), mainTokenStart, mainTokenEnd, tokenType, children))

                lexer.advance()
                tokenType = lexer.tokenType
            }
        }
    }
}