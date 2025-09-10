/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IErrorCounterReparseableElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.elements.KtClassLiteralExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtCollectionLiteralExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStringTemplateExpressionElementType
import org.jetbrains.kotlin.util.getSingleChildOrNull

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

    fun LighterASTNode.getOperationSymbol(tree: FlyweightCapableTreeStructure<LighterASTNode>): KtToken {
        assert(tokenType == OPERATION_REFERENCE)
        // Actually, all types are `KtSingleValueToken` except `IDENTIFIER` that is `KtToken`. The latter is used for infix functions.
        return getSingleChildOrNull(tree)!!.tokenType as KtToken
    }

    private val expressionSet = listOf(
        REFERENCE_EXPRESSION,
        CALL_EXPRESSION,
        DOT_QUALIFIED_EXPRESSION,
        LAMBDA_EXPRESSION,
        FUN,
        EMPTY_VALUE_ARGUMENT,
    )

    fun LighterASTNode.isExpression(): Boolean {
        return when (this.tokenType) {
            is KtNodeType,
            is KtConstantExpressionElementType,
            is KtStringTemplateExpressionElementType,
            is KtClassLiteralExpressionElementType,
            is KtCollectionLiteralExpressionElementType,
            in expressionSet,
                -> true

            else -> false
        }
    }
}
