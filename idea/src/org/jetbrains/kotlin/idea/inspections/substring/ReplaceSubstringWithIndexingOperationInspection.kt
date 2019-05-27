/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.substring

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator

class ReplaceSubstringWithIndexingOperationInspection :
    ReplaceSubstringInspection() {
    override fun inspectionText(element: KtDotQualifiedExpression): String = "Replace 'substring' call with indexing operation call"
    override val defaultFixText: String = "Replace 'substring' call with indexing operation call"
    override val isAlwaysStable: Boolean = true

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        if (element !is KtDotQualifiedExpression) return
        val expression = element.callExpression?.valueArguments?.firstOrNull()?.getArgumentExpression() ?: return
        element.replaceWith("$0[$1]", expression)
    }

    override fun isApplicableInner(element: KtDotQualifiedExpression): Boolean {
        val arguments = element.callExpression?.valueArguments ?: return false
        if (arguments.size != 2) return false

        val arg1 = element.getValueArgument(0) ?: return false
        val arg2 = element.getValueArgument(1) ?: return false
        return arg1 + 1 == arg2
    }

    private fun KtDotQualifiedExpression.getValueArgument(index: Int): Int? {
        val bindingContext = analyze()
        val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return null
        val expression = resolvedCall.call.valueArguments[index].getArgumentExpression() as? KtConstantExpression ?: return null
        val constant = ConstantExpressionEvaluator.getConstant(expression, bindingContext) ?: return null
        val constantType = bindingContext.getType(expression) ?: return null
        return constant.getValue(constantType) as? Int
    }
}