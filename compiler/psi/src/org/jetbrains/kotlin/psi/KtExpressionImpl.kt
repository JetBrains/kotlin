/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
                        @Suppress("USELESS_CAST") // K2 warning suppression, TODO: KT-62472
                        if (KtPsiUtil.areParenthesesNecessary(newElement, expression, parent as KtElement)) {
                            val factory = KtPsiFactory(expression.project)
                            return rawReplaceHandler(factory.createExpressionByPattern("($0)", newElement, reformat = reformat))
                        }
                    }
                    is KtSimpleNameStringTemplateEntry -> {
                        if (newElement !is KtSimpleNameExpression && !newElement.isThisWithoutLabel()) {
                            val factory = KtPsiFactory(expression.project)
                            val newEntry = parent.replace(factory.createBlockStringTemplateEntry(newElement)) as KtBlockStringTemplateEntry
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
