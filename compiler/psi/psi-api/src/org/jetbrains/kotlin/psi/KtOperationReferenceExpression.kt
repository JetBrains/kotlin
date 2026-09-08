/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lang.BinaryOperationPrecedence
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.KotlinOperationReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets
import org.jetbrains.kotlin.psi.utils.OperatorTokens
import org.jetbrains.kotlin.resolution.KtResolvableCall

/**
 * Represents an operator symbol in an expression.
 *
 * ### Example:
 *
 * ```kotlin
 * val sum = a + b
 * //          ^
 * ```
 *
 * ### Analysis API Resolver Notes:
 *
 * #### Note 1:
 *
 * The result of the call resolution is exactly the same as if it was called on the parent element.
 *
 * #### Note 2:
 *
 * **The resolver targets symbols contributed by the operation reference itself.**
 *
 * For compound cases, this includes the
 * symbols corresponding to the resulting update, but not the symbols used only for intermediate reads.
 *
 * For instance, in compound array assignments this includes the operator symbol (e.g., `plus`)
 * and the writing accessor (`set`), but not the reading accessor (`get`).
 *
 * #### Example
 *
 * ```kotlin
 * interface MyList {
 *     operator fun get(index: Int): String
 *     operator fun set(index: Int, value: String)
 * }
 *
 * fun test(list: MyList) {
 *     list[10] += "value"
 * }
 * ```
 *
 * `list[10] += "value"` desugars into something like
 *
 * ```kotlin
 * val oldValue = list.get(10)
 * val newValue = oldValue.plus("value")
 * list.set(10, newValue)
 * ```
 *
 * And the result will include symbols for the `plus` and `set` operators, but not for `get`.
 *
 * If the reading symbol is also needed, the API should be called on the parent expression
 * (e.g., [KtBinaryExpression] or [KtUnaryExpression]).
 *
 * @see KtBinaryExpression
 * @see KtUnaryExpression
 */
@OptIn(KtImplementationDetail::class)
class KtOperationReferenceExpression :
    KtExpressionImplStub<KotlinOperationReferenceExpressionStub>,
    KtSimpleNameExpression,
    KtResolvableCall {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    @KtImplementationDetail
    constructor(stub: KotlinOperationReferenceExpressionStub) : super(stub, KtNodeTypes.OPERATION_REFERENCE)

    private companion object {
        private val OPERATION_TOKENS: TokenSet = TokenSet.create(*buildList {
            addAll(KtTokenSets.POSTFIX_OPERATIONS.types)
            addAll(KtTokenSets.PREFIX_OPERATIONS.types)
            for (precedence in BinaryOperationPrecedence.entries) {
                addAll(precedence.tokens)
            }
        }.toTypedArray())
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitSimpleNameExpression(this, data)
    }

    override fun getReferencedName(): String {
        val stub = greenStub
        if (stub != null) {
            return stub.referencedName
        }

        return KtSimpleNameExpressionImpl.getReferencedNameImpl(this)
    }

    override fun getReferencedNameAsName(): Name {
        return KtSimpleNameExpressionImpl.getReferencedNameAsNameImpl(this)
    }

    override fun getReferencedNameElementType(): IElementType {
        val stub = greenStub
        if (stub != null) {
            return stub.operationToken
        }

        return KtSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)
    }

    override fun getIdentifier(): PsiElement? = findChildByType(KtTokens.IDENTIFIER)

    override fun getReferencedNameElement() = findChildByType<PsiElement?>(OPERATION_TOKENS) ?: this

    /**
     * The closest [KtExpression] sibling **before** this operation reference in the stub tree,
     * or `null` if the element is not stub-based or the operand is not stub-based itself.
     *
     * Not every expression is stub-based (e.g., [KtParenthesizedExpression]), so `null` doesn't mean the absence of the operand.
     * The caller has to fall back to the AST-based search in this case.
     *
     * @see KtPostfixExpression.getBaseExpression
     * @see KtBinaryExpression.getLeft
     */
    internal val stubBasedOperandBefore: KtExpression?
        get() = findStubBasedOperand(beforeOperation = true)

    /**
     * The closest [KtExpression] sibling **after** this operation reference in the stub tree,
     * or `null` if the element is not stub-based or the operand is not stub-based itself.
     *
     * @see stubBasedOperandBefore
     * @see KtPrefixExpression.getBaseExpression
     * @see KtBinaryExpression.getRight
     */
    internal val stubBasedOperandAfter: KtExpression?
        get() = findStubBasedOperand(beforeOperation = false)

    private fun findStubBasedOperand(beforeOperation: Boolean): KtExpression? {
        val siblings = stub?.parentStub?.childrenStubs ?: return null

        var lastExpressionBeforeOperation: KtExpression? = null
        var operationFound = false
        for (sibling in siblings) {
            when (val siblingPsi = sibling.psi) {
                is KtOperationReferenceExpression -> {
                    if (beforeOperation) {
                        return lastExpressionBeforeOperation
                    }

                    operationFound = true
                }

                is KtExpression -> when {
                    beforeOperation -> lastExpressionBeforeOperation = siblingPsi
                    operationFound -> return siblingPsi
                }
            }
        }

        return null
    }

    /**
     * The token type of the operation sign (for example, [KtTokens.PLUS][org.jetbrains.kotlin.lexer.KtTokens.PLUS] for `+`), or `null` if
     * the operation is spelled as an identifier (as with a named infix function).
     */
    val operationSignTokenType: KtSingleValueToken?
        get() {
            val stub = greenStub
            if (stub != null) {
                return stub.operationToken as? KtSingleValueToken
            }

            return (firstChild as? TreeElement)?.elementType as? KtSingleValueToken
        }

    /**
     * Returns `true` if this operation sign corresponds to a convention operator that maps to a named operator function (for example, `+`
     * maps to `plus`). Returns `false` for non-convention signs such as `&&`.
     */
    fun isConventionOperator(): Boolean {
        val tokenType = operationSignTokenType ?: return false
        return OperatorTokens.operationName(tokenType) != null
    }
}
