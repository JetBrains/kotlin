/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import fleet.com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.kotlin.kmp.lexer.KotlinLexer

class NewLexer : AbstractLexer<SyntaxElementType>() {
    override fun tokenize(text: String): List<TokenInfo<SyntaxElementType>> {
        val lexer = KotlinLexer()
        lexer.start(text)

        return buildList {
            var currentToken = lexer.getTokenType()
            while (currentToken != null) {
                add(TokenInfo(currentToken.toString(), lexer.getTokenStart(), lexer.getTokenEnd(), currentToken))
                lexer.advance()
                currentToken = lexer.getTokenType()
            }
        }
    }
}