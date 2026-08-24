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
 * This is the common base for the concrete node types [KtCallableReferenceExpression] (`foo::bar`) and [KtClassLiteralExpression]
 * (`foo::class`). The part before `::` is the optional left-hand side, which may denote an expression receiver or a type.
 *
 * ### Example:
 *
 * ```kotlin
 * val ref = String::length
 * //        ^____________^
 * // A double-colon expression (a callable reference)
 * ```
 */
interface KtDoubleColonExpression : KtExpression, KtResolvable {
    /**
     * The first child when it is a [KtExpression], or `null` otherwise. Syntactic type receivers such as `String` in `String::length` are
     * also represented as expressions. An empty left-hand side, as in `::foo`, has no receiver expression.
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
     * The PSI sibling immediately before `::`, or `null` if the token is the first child. This may be whitespace or a comment rather than
     * the receiver itself; use [receiverExpression] to obtain the receiver.
     */
    val lhs: PsiElement?
        get() = doubleColonTokenReference.prevSibling

    @Deprecated(
        message = "Use setDoubleColonReceiverExpression(newReceiverExpression) instead",
        replaceWith = ReplaceWith(
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
