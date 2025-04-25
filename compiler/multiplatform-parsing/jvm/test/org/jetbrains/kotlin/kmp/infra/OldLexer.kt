/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KotlinLexer

class OldLexer : AbstractLexer<IElementType>() {
    override fun tokenize(text: String): List<TokenInfo<IElementType>> {
        val lexer = KotlinLexer()
        lexer.start(text)

        return buildList {
            var currentToken = lexer.tokenType
            while (currentToken != null) {
                add(TokenInfo(currentToken.toString(), lexer.tokenStart, lexer.tokenEnd, currentToken))
                lexer.advance()
                currentToken = lexer.tokenType
            }
        }
    }
}