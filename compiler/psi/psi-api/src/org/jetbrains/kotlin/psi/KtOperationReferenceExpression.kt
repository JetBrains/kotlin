/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lang.BinaryOperationPrecedence
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets
import org.jetbrains.kotlin.types.expressions.OperatorConventions

/**
 * Represents an operator symbol in an expression.
 *
 * ### Example:
 *
 * ```kotlin
 * val sum = a + b
 * //          ^
 * ```
 */
class KtOperationReferenceExpression(node: ASTNode) : KtSimpleNameExpressionImpl(node) {
    private companion object {
        private val OPERATION_TOKENS: TokenSet = TokenSet.create(*buildList {
            addAll(KtTokenSets.POSTFIX_OPERATIONS.types)
            addAll(KtTokenSets.PREFIX_OPERATIONS.types)
            for (precedence in BinaryOperationPrecedence.entries) {
                addAll(precedence.tokens)
            }
        }.toTypedArray())
    }

    override fun getReferencedNameElement() = findChildByType<PsiElement?>(OPERATION_TOKENS) ?: this

    val operationSignTokenType: KtSingleValueToken?
        get() = (firstChild as? TreeElement)?.elementType as? KtSingleValueToken

    fun isConventionOperator(): Boolean {
        val tokenType = operationSignTokenType ?: return false
        return OperatorConventions.getNameForOperationSymbol(tokenType) != null
    }
}
