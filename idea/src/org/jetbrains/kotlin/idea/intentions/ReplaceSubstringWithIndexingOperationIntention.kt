/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
        if (arguments.size != 2) return null

        val arg1 = element.getValueArgument(0) ?: return null
        val arg2 = element.getValueArgument(1) ?: return null
        if (arg1 + 1 != arg2) return null

        return getTextRange(element)
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        val expression = element.callExpression?.valueArguments?.firstOrNull()?.getArgumentExpression() ?: return
        element.replaceWith("$0[$1]", expression)
    }

    private fun KtDotQualifiedExpression.isSubstringMethod(): Boolean {
        val resolvedCall = toResolvedCall(BodyResolveMode.PARTIAL) ?: return false
        return (resolvedCall.resultingDescriptor.fqNameUnsafe.asString() == "kotlin.text.substring")
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