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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.unwrapBlockOrParenthesis
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.utils.addToStdlib.constant

class ConvertIfWithThrowToAssertIntention : SelfTargetingOffsetIndependentIntention<KtIfExpression>(KtIfExpression::class.java, "Replace 'if' with 'assert' statement") {
    override fun isApplicableTo(element: KtIfExpression): Boolean {
        if (element.`else` != null) return false

        val throwExpr = element.then?.unwrapBlockOrParenthesis() as? KtThrowExpression
        val thrownExpr = getSelector(throwExpr?.thrownExpression)
        if (thrownExpr !is KtCallExpression) return false

        if (thrownExpr.valueArguments.size > 1) return false

        val resolvedCall = thrownExpr.getResolvedCall(thrownExpr.analyze()) ?: return false
        val targetFqName = DescriptorUtils.getFqName(resolvedCall.resultingDescriptor).asString()
        return targetFqName in constant { setOf("kotlin.AssertionError.<init>", "java.lang.AssertionError.<init>") }
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val condition = element.condition ?: return

        val thenExpr = element.then?.unwrapBlockOrParenthesis() as KtThrowExpression
        val thrownExpr = getSelector(thenExpr.thrownExpression) as KtCallExpression

        val psiFactory = KtPsiFactory(element)
        condition.replace(psiFactory.createExpressionByPattern("!$0", condition))

        var newCondition = element.condition!!
        val simplifier = SimplifyNegatedBinaryExpressionIntention()
        if (simplifier.isApplicableTo(newCondition as KtPrefixExpression)) {
            simplifier.applyTo(newCondition, editor)
            newCondition = element.condition!!
        }

        val arg = thrownExpr.valueArguments.singleOrNull()?.getArgumentExpression()
        val assertExpr = if (arg != null && !arg.isNullExpression())
            psiFactory.createExpressionByPattern("kotlin.assert($0) {$1}", newCondition, arg)
        else
            psiFactory.createExpressionByPattern("kotlin.assert($0)", newCondition)

        val newExpr = element.replaced(assertExpr)
        ShortenReferences.DEFAULT.process(newExpr)
    }

    private fun getSelector(element: KtExpression?): KtExpression? {
        if (element is KtDotQualifiedExpression) {
            return element.selectorExpression
        }
        return element
    }
}
