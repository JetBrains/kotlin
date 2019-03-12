/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.psi.tree.IErrorCounterReparseableElementType
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens

object ElementTypeUtils {
    @JvmStatic
    fun getKotlinBlockImbalanceCount(seq: CharSequence): Int {
        val lexer = KotlinLexer()

        lexer.start(seq)
        if (lexer.tokenType !== KtTokens.LBRACE) return IErrorCounterReparseableElementType.FATAL_ERROR
        lexer.advance()
        var balance = 1
        while (lexer.tokenType != KtTokens.EOF) {
            val type = lexer.tokenType ?: break
            if (balance == 0) {
                return IErrorCounterReparseableElementType.FATAL_ERROR
            }
            if (type === KtTokens.LBRACE) {
                balance++
            } else if (type === KtTokens.RBRACE) {
                balance--
            }
            lexer.advance()
        }
        return balance
    }
}