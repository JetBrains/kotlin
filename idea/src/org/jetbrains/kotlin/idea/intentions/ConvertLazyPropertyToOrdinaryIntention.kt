/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiComment
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren

class ConvertLazyPropertyToOrdinaryIntention : SelfTargetingIntention<KtProperty>(
    KtProperty::class.java, "Convert to ordinary property"
) {
    override fun isApplicableTo(element: KtProperty, caretOffset: Int): Boolean {
        val delegateExpression = element.delegate?.expression as? KtCallExpression ?: return false
        val lambdaBody = delegateExpression.functionLiteral()?.bodyExpression ?: return false
        if (lambdaBody.statements.isEmpty()) return false
        return delegateExpression.isCalling(FqName("kotlin.lazy"))
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val delegate = element.delegate ?: return
        val delegateExpression = delegate.expression as? KtCallExpression ?: return
        val functionLiteral = delegateExpression.functionLiteral() ?: return
        element.initializer = functionLiteral.singleStatement() ?: KtPsiFactory(element).createExpression("run ${functionLiteral.text}")
        delegate.delete()
    }

    private fun KtCallExpression.functionLiteral(): KtFunctionLiteral? {
        return lambdaArguments.singleOrNull()?.getLambdaExpression()?.functionLiteral
    }

    private fun KtFunctionLiteral.singleStatement(): KtExpression? {
        val body = this.bodyExpression ?: return null
        if (body.allChildren.any { it is PsiComment }) return null
        return body.statements.singleOrNull()
    }
}
