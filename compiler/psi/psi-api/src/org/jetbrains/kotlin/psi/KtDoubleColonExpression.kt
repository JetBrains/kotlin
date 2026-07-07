/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolution.KtResolvable

/**
 * Represents an expression that uses the `::` token, namely a callable reference or a class literal.
 *
 * This is the common base for the concrete node types [KtCallableReferenceExpression] (`foo::bar`) and
 * [KtClassLiteralExpression] (`foo::class`). The part before `::` is the optional left-hand side, which may be an
 * expression receiver, a type, or empty.
 *
 * ### Example:
 *
 * ```kotlin
 * val ref = String::length
 * //        ^____________^
 * // A double-colon expression (a callable reference)
 * ```
 */
@OptIn(KtExperimentalApi::class)
interface KtDoubleColonExpression : KtExpression, KtResolvable {
    /**
     * The receiver expression on the left-hand side of `::`, or `null` if the left-hand side is empty or is a pure
     * type reference rather than an expression.
     */
    val receiverExpression: KtExpression?
        get() = node.firstChildNode.psi as? KtExpression

    /**
     * `true` if the left-hand side is a nullable type, that is, a `?` appears before `::` (as in `String?::class`).
     */
    val hasQuestionMarks: Boolean
        get() {
            for (element in generateSequence(node.firstChildNode, ASTNode::getTreeNext)) {
                when (element.elementType) {
                    KtTokens.QUEST -> return true
                    KtTokens.COLONCOLON -> return false
                }
            }
            error("Double colon expression must have '::': $text")
        }

    /**
     * Returns the `::` token, or `null` if it is absent in incomplete code.
     */
    fun findColonColon(): PsiElement?

    /**
     * The `::` token. Throws if it is absent; use [findColonColon] to handle incomplete code.
     */
    val doubleColonTokenReference: PsiElement
        get() = findColonColon()!!

    /**
     * The element on the left-hand side of `::` (an expression or a type reference), or `null` if the left-hand side
     * is empty.
     */
    val lhs: PsiElement?
        get() = doubleColonTokenReference.prevSibling

    @Deprecated(
        "Use setDoubleColonReceiverExpression(newReceiverExpression) instead",
        ReplaceWith(
            "this.setDoubleColonReceiverExpression(newReceiverExpression)",
            "org.jetbrains.kotlin.idea.base.psi.setDoubleColonReceiverExpression",
        ),
    )
    @OptIn(KtNonPublicApi::class)
    fun setReceiverExpression(newReceiverExpression: KtExpression) {
        KtPsiMutationService.getInstance().setDoubleColonReceiverExpression(this, newReceiverExpression)
    }

    /**
     * `true` if the left-hand side is empty, as in `::foo` where the receiver is inferred from the context.
     */
    val isEmptyLHS: Boolean
        get() = lhs == null

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitDoubleColonExpression(this, data)
    }
}
