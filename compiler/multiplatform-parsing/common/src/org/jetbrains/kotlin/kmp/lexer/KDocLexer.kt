/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.lexer

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import com.intellij.platform.syntax.lexer.Lexer
import com.intellij.platform.syntax.syntaxElementTypeSetOf
import com.intellij.platform.syntax.util.lexer.FlexAdapter
import com.intellij.platform.syntax.util.lexer.MergingLexerAdapter
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
class KDocLexer : MergingLexerAdapter(
    FlexAdapter(KDocFlexLexer()),
    KDOC_TOKENS
) {
    companion object {
        val KDOC_TOKENS = syntaxElementTypeSetOf(KDocTokens.TEXT, KDocTokens.CODE_BLOCK_TEXT, SyntaxTokenTypes.WHITE_SPACE)
    }

    override fun merge(tokenType: SyntaxElementType, lexer: Lexer): SyntaxElementType {
        val nextTokenType = lexer.getTokenType()
        val nextTokenText = lexer.getTokenText()
        if (tokenType == KDocTokens.CODE_BLOCK_TEXT && nextTokenType == KDocTokens.TEXT && nextTokenText.isValidCodeFence()) {
            lexer.advance()
            return KDocTokens.TEXT // Don't treat the trailing line as a part of a code block
        } else if (tokenType == KDocTokens.CODE_BLOCK_TEXT || tokenType == KDocTokens.CODE_SPAN_TEXT || tokenType == KDocTokens.TEXT || tokenType == KtTokens.WHITE_SPACE) {
            while (tokenType == lexer.getTokenType()) {
                lexer.advance()
            }
        }

        return tokenType
    }

    private fun String.isValidCodeFence(): Boolean =
        this.length >= 3 && (this[0] == '`' || this[0] == '~') && this.all { it == this[0] }

}