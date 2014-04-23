/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetPrefixExpression
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import org.jetbrains.jet.lang.psi.JetCallableReferenceExpression
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import kotlin.properties.Delegates
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.plugin.intentions.branchedTransformations.extractExpressionIfSingle
import org.jetbrains.jet.lang.psi.JetThrowExpression
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetUserType

public class ConvertIfWithThrowToAssertIntention : JetSelfTargetingIntention<JetIfExpression>(
        "convert.if.with.throw.to.assert", javaClass()) {

    private var param: ResolvedValueArgument by Delegates.notNull()

    override fun isApplicableTo(element: JetIfExpression): Boolean {
        if (element.getElse() != null) return false

        val thenExpr = element.getThen()?.extractExpressionIfSingle()
        if (thenExpr !is JetThrowExpression) return false

        val thrownExpr = getSelector(thenExpr.getThrownExpression())
        if (thrownExpr !is JetCallExpression) return false

        if (thrownExpr.getCalleeExpression()?.getText() != "AssertionError") return false

        val context = AnalyzerFacadeWithCache.getContextForElement(thrownExpr)
        val resolvedCall = context[BindingContext.RESOLVED_CALL, thrownExpr.getCalleeExpression()]
        if (resolvedCall == null) return false

        val paramAmount = resolvedCall.getValueArguments().size
        if (paramAmount > 1) return false

        return DescriptorUtils.getFqName(resolvedCall.getResultingDescriptor()).toString() == "java.lang.AssertionError.<init>"
    }

    override fun applyTo(element: JetIfExpression, editor: Editor) {
        val condition = element.getCondition()
        if (condition == null) return

        val thenExpr = element.getThen()?.extractExpressionIfSingle() as JetThrowExpression
        val thrownExpr = getSelector(thenExpr.getThrownExpression()) as JetCallExpression
        val context = AnalyzerFacadeWithCache.getContextForElement(thrownExpr)
        val resolvedCall = context[BindingContext.RESOLVED_CALL, thrownExpr.getCalleeExpression()]!!

        val paramAmount = resolvedCall.getValueArguments().size
        val param =
            if (paramAmount == 1) {
                val paramDescriptor = resolvedCall.getResultingDescriptor().getValueParameters()[0]
                val paramText = resolvedCall.getValueArguments()[paramDescriptor]!!.toString()
                if (paramText == "null") "" else ", $paramText"
            } else {
                ""
            }

        val negatedCondition = JetPsiFactory.createExpression(element.getProject(), "!true") as JetPrefixExpression
        negatedCondition.getBaseExpression()!!.replace(condition)
        condition.replace(negatedCondition)

        val newCondition = element.getCondition() as JetPrefixExpression
        val simplifier = SimplifyNegatedBinaryExpressionIntention()
        if (simplifier.isApplicableTo(newCondition)) {
            simplifier.applyTo(newCondition, editor)
        }

        val assertText = "kotlin.assert(${element.getCondition()?.getText()} $param)"
        val assertExpr = JetPsiFactory.createExpression(element.getProject(), assertText)

        val newExpr = element.replace(assertExpr) as JetExpression
        ShortenReferences.process(newExpr)
    }


    private fun getSelector(element: JetExpression?): JetExpression? {
        if (element is JetDotQualifiedExpression) {
            return element.getSelectorExpression()
        }
        return element
    }
}
