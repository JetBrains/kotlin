/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.util.lexer.LexerBase
import org.jetbrains.kotlin.kmp.lexer.KDocLexer
import org.jetbrains.kotlin.kmp.lexer.KDocTokens
import org.jetbrains.kotlin.kmp.lexer.KotlinLexer
import org.jetbrains.kotlin.kmp.lexer.KtTokens

class NewTestLexer : AbstractTestLexer<SyntaxElementType>() {
    override fun tokenize(text: String): TestToken<SyntaxElementType> {
        return tokenizeSubsequence(text, 0, KotlinLexer()).wrap(text.length)
    }

    private fun tokenizeSubsequence(charSequence: CharSequence, start: Int, lexer: LexerBase): List<TestToken<SyntaxElementType>> {
        return buildList {
            lexer.start(charSequence)
            var tokenType = lexer.getTokenType()
            while (tokenType != null) {
                val mainTokenStart = lexer.getTokenStart() + start
                val mainTokenEnd = lexer.getTokenEnd() + start

                val children = when (tokenType) {
                    KtTokens.DOC_COMMENT,
                    KDocTokens.MARKDOWN_LINK
                        -> {
                        val subLexer = if (tokenType == KtTokens.DOC_COMMENT) KDocLexer() else KotlinLexer()
                        val subSequence = charSequence.subSequence(lexer.getTokenStart(), lexer.getTokenEnd())
                        tokenizeSubsequence(subSequence, mainTokenStart, subLexer)
                    }
                    else -> {
                        emptyList()
                    }
                }
                add(TestToken(tokenType.toString(), mainTokenStart, mainTokenEnd, tokenType, children))

                lexer.advance()
                tokenType = lexer.getTokenType()
            }
        }
    }
}