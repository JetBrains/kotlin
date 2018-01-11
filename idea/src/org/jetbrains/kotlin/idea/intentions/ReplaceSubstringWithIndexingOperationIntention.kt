/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ReplaceSubstringWithIndexingOperationIntention : ReplaceSubstringIntention("Replace 'substring' call with indexing operation call") {
    override fun applicabilityRange(element: KtDotQualifiedExpression): TextRange? {
        if (element.isSubstringMethod()) {
            return applicabilityRangeInner(element)
        }

        return null
    }

    override fun applicabilityRangeInner(element: KtDotQualifiedExpression): TextRange? {
        val arguments = element.callExpression?.valueArguments ?: return null
        if (arguments.size != 2 || !element.isFirstArgumentZero() || !element.isSecondArgumentOne()) return null
        return getTextRange(element)
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        element.replaceWith(
                "$0[$1]",
                element.getArgumentExpression(0))
    }

    private fun KtDotQualifiedExpression.isSubstringMethod(): Boolean {
        val resolvedCall = toResolvedCall(BodyResolveMode.PARTIAL) ?: return false
        return (resolvedCall.resultingDescriptor.fqNameUnsafe.asString() == "kotlin.text.substring")
    }

    private fun KtDotQualifiedExpression.isSecondArgumentOne(): Boolean {
        val bindingContext = analyze()
        val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return false
        val expression = resolvedCall.call.valueArguments[1].getArgumentExpression() as? KtConstantExpression ?: return false

        val constant = ConstantExpressionEvaluator.getConstant(expression, bindingContext) ?: return false
        val constantType = bindingContext.getType(expression) ?: return false
        return constant.getValue(constantType) == 1
    }

    private fun KtDotQualifiedExpression.getArgumentExpression(index: Int): KtExpression {
        return callExpression!!.valueArguments[index].getArgumentExpression()!!
    }
}