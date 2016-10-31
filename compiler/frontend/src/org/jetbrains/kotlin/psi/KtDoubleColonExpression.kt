/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens

abstract class KtDoubleColonExpression(node: ASTNode) : KtExpressionImpl(node) {
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

    val doubleColonTokenReference: PsiElement
        get() = findChildByType(KtTokens.COLONCOLON)!!

    fun setReceiverExpression(newReceiverExpression: KtExpression) {
        val oldReceiverExpression = this.receiverExpression
        oldReceiverExpression?.replace(newReceiverExpression) ?: addBefore(newReceiverExpression, doubleColonTokenReference)
    }

    val isEmptyLHS: Boolean
        get() = doubleColonTokenReference.prevSibling == null

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitDoubleColonExpression(this, data)
    }
}
