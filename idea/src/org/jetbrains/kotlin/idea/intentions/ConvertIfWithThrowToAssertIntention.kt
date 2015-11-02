/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.unwrapBlockOrParenthesis
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.replaced

public class ConvertIfWithThrowToAssertIntention : SelfTargetingOffsetIndependentIntention<KtIfExpression>(javaClass(), "Replace 'if' with 'assert' statement") {

    override fun isApplicableTo(element: KtIfExpression): Boolean {
        if (element.getElse() != null) return false

        val throwExpr = element.getThen()?.unwrapBlockOrParenthesis() as? KtThrowExpression
        val thrownExpr = getSelector(throwExpr?.getThrownExpression())
        if (thrownExpr !is KtCallExpression) return false

        if (thrownExpr.getValueArguments().size() > 1) return false

        val resolvedCall = thrownExpr.getResolvedCall(thrownExpr.analyze()) ?: return false
        return DescriptorUtils.getFqName(resolvedCall.getResultingDescriptor()).toString() == "java.lang.AssertionError.<init>"
    }

    override fun applyTo(element: KtIfExpression, editor: Editor) {
        val condition = element.getCondition() ?: return

        val thenExpr = element.getThen()?.unwrapBlockOrParenthesis() as KtThrowExpression
        val thrownExpr = getSelector(thenExpr.getThrownExpression()) as KtCallExpression

        val psiFactory = KtPsiFactory(element)
        condition.replace(psiFactory.createExpressionByPattern("!$0", condition))

        var newCondition = element.getCondition()!!
        val simplifier = SimplifyNegatedBinaryExpressionIntention()
        if (simplifier.isApplicableTo(newCondition as KtPrefixExpression)) {
            simplifier.applyTo(newCondition, editor)
            newCondition = element.getCondition()!!
        }

        val arg = thrownExpr.getValueArguments().singleOrNull()?.getArgumentExpression()
        val assertExpr = if (arg != null && !arg.isNullExpression())
            psiFactory.createExpressionByPattern("kotlin.assert($0, $1)", newCondition, arg)
        else
            psiFactory.createExpressionByPattern("kotlin.assert($0)", newCondition)

        val newExpr = element.replaced(assertExpr)
        ShortenReferences.DEFAULT.process(newExpr)
    }

    private fun getSelector(element: KtExpression?): KtExpression? {
        if (element is KtDotQualifiedExpression) {
            return element.getSelectorExpression()
        }
        return element
    }
}
