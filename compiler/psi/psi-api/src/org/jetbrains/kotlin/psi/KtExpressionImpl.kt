/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

/**
 * Base implementation of [KtExpression] backed directly by the AST tree.
 *
 * This is an internal implementation base class of the Kotlin PSI, not intended for direct use or subclassing outside of the PSI
 * implementation. For expressions that may also be backed by a stub, see [KtExpressionImplStub].
 */
@SubclassOptInRequired(KtImplementationDetail::class)
abstract class KtExpressionImpl : KtElementImpl, KtExpression {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitExpression(this, data)

    protected fun findExpressionUnder(type: IElementType): KtExpression? {
        val containerNode = findChildByType<KtContainerNode>(type) ?: return null
        return containerNode.findChildByClass<KtExpression>(KtExpression::class.java)
    }

    /**
     * Replaces this expression with [newElement].
     *
     * When [KtPsiMutationService] is registered, as in the IntelliJ Kotlin plugin, the replacement may also adjust the new expression to
     * its place, e.g., wrap it in parentheses to keep the operator precedence, or turn a `$name` string template entry into `${...}`.
     * Without the service, it performs only the plain platform replacement, so, e.g., replacing `a` in `a * b` with `x + y` results in
     * `x + y * b`.
     */
    @OptIn(KtIdeApi::class)
    override fun replace(newElement: PsiElement): PsiElement {
        val mutationService = KtPsiMutationService.getInstanceOrNull() ?: return super.replace(newElement)
        return mutationService.replaceExpression(this, newElement, true) { super.replace(it) }
    }

    companion object {
        @Deprecated(
            message = "Use expression.replaceExpression(newElement, reformat, rawReplaceHandler) instead",
            replaceWith = ReplaceWith(
                "expression.replaceExpression(newElement, reformat, rawReplaceHandler)",
                "org.jetbrains.kotlin.idea.base.psi.replaceExpression",
            ),
            level = DeprecationLevel.ERROR,
        )
        @OptIn(KtIdeApi::class)
        fun replaceExpression(
            expression: KtExpression,
            newElement: PsiElement,
            reformat: Boolean = true,
            rawReplaceHandler: (PsiElement) -> PsiElement,
        ): PsiElement = KtPsiMutationService.getInstance().replaceExpression(expression, newElement, reformat, rawReplaceHandler)
    }
}
