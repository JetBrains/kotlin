/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

abstract class KtExpressionImpl(node: ASTNode) : KtElementImpl(node), KtExpression {

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitExpression(this, data)

    protected fun findExpressionUnder(type: IElementType): KtExpression? {
        val containerNode = findChildByType<KtContainerNode>(type) ?: return null
        return containerNode.findChildByClass<KtExpression>(KtExpression::class.java)
    }

    override fun replace(newElement: PsiElement): PsiElement {
        return replaceExpression(this, newElement) { super.replace(it) }
    }

    companion object {
        fun replaceExpression(
            expression: KtExpression,
            newElement: PsiElement,
            reformat: Boolean = true,
            rawReplaceHandler: (PsiElement) -> PsiElement
        ): PsiElement {
            val parent = expression.parent

            if (newElement is KtExpression) {
                when (parent) {
                    is KtExpression, is KtValueArgument -> {
                        if (KtPsiUtil.areParenthesesNecessary(newElement, expression, parent as KtElement)) {
                            return rawReplaceHandler(
                                KtPsiFactory(expression).createExpressionByPattern("($0)", newElement, reformat = reformat)
                            )
                        }
                    }
                    is KtSimpleNameStringTemplateEntry -> {
                        if (newElement !is KtSimpleNameExpression && !newElement.isThisWithoutLabel()) {
                            val newEntry =
                                parent.replace(KtPsiFactory(expression).createBlockStringTemplateEntry(newElement)) as KtBlockStringTemplateEntry
                            return newEntry.expression!!
                        }
                    }
                }
            }

            return rawReplaceHandler(newElement)
        }
    }
}

private fun PsiElement.isThisWithoutLabel() = this is KtThisExpression && getLabelName() == null
