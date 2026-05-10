/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolution.KtResolvable

@OptIn(KtExperimentalApi::class)
interface KtDoubleColonExpression : KtExpression, KtResolvable {
    val receiverExpression: KtExpression?
        get() = node.firstChildNode.psi as? KtExpression

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

    fun findColonColon(): PsiElement?

    val doubleColonTokenReference: PsiElement
        get() = findColonColon()!!

    val lhs: PsiElement?
        get() = doubleColonTokenReference.prevSibling

    fun setReceiverExpression(newReceiverExpression: KtExpression) {
        val oldReceiverExpression = this.receiverExpression
        oldReceiverExpression?.replace(newReceiverExpression) ?: addBefore(newReceiverExpression, doubleColonTokenReference)
    }

    val isEmptyLHS: Boolean
        get() = lhs == null

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitDoubleColonExpression(this, data)
    }
}
