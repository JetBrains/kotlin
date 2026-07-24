/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.psiUtil.parentSubstitute

abstract class KtExpressionImpl(node: ASTNode) : KtElementImpl(node), KtExpression {

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitExpression(this, data)

    protected fun findExpressionUnder(type: IElementType): KtExpression? {
        val containerNode = findChildByType<KtContainerNode>(type) ?: return null
        return containerNode.findChildByClass<KtExpression>(KtExpression::class.java)
    }

    @OptIn(KtNonPublicApi::class)
    override fun replace(newElement: PsiElement): PsiElement =
        KtPsiMutationService.getInstance().replaceExpression(this, newElement, true) { super.replace(it) }

    // HasPlatformType is used to preserve the flexible type to not break source compatibility
    @Suppress("DEPRECATION", "HasPlatformType")
    override fun getParent() = parentSubstitute ?: super.getParent()

    companion object {
        @Deprecated(
            "Use expression.replaceExpression(newElement, reformat, rawReplaceHandler) instead",
            ReplaceWith(
                "expression.replaceExpression(newElement, reformat, rawReplaceHandler)",
                "org.jetbrains.kotlin.idea.base.psi.replaceExpression",
            ),
        )
        @OptIn(KtNonPublicApi::class)
        fun replaceExpression(
            expression: KtExpression,
            newElement: PsiElement,
            reformat: Boolean = true,
            rawReplaceHandler: (PsiElement) -> PsiElement,
        ): PsiElement = KtPsiMutationService.getInstance().replaceExpression(expression, newElement, reformat, rawReplaceHandler)
    }
}
