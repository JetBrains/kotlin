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
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetPrefixExpression
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import org.jetbrains.jet.lang.psi.JetCallableReferenceExpression
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import kotlin.properties.Delegates
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getResolvedCall

public class ConvertAssertToIfWithThrowIntention : JetSelfTargetingIntention<JetCallExpression>(
        "convert.assert.to.if.with.throw", javaClass()) {

    private var messageIsAFunction : Boolean by Delegates.notNull()

    override fun isApplicableTo(element: JetCallExpression): Boolean {
        if (element.getCalleeExpression()?.getText() != "assert") return false

        val arguments = element.getValueArguments().size
        val lambdas = element.getFunctionLiteralArguments().size
        if (!(arguments == 1 && (lambdas == 1 || lambdas == 0)) && arguments != 2) return false

        val context = AnalyzerFacadeWithCache.getContextForElement(element)
        val resolvedCall = element.getResolvedCall(context)
        if (resolvedCall == null) return false

        val valParameters = resolvedCall.getResultingDescriptor().getValueParameters()
        if (valParameters.size > 1) {
            messageIsAFunction = (valParameters[1].getType() != KotlinBuiltIns.getInstance().getAnyType())
        } else {
            messageIsAFunction = false
        }

        return DescriptorUtils.getFqName(resolvedCall.getResultingDescriptor()).toString() == "kotlin.assert"
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val args = element.getValueArguments()
        val condition = args[0]?.getArgumentExpression()
        val lambdas = element.getFunctionLiteralArguments()

        val psiFactory = JetPsiFactory(element)
        val messageExpr =
                if (args.size == 2) {
                    args[1]?.getArgumentExpression()
                }
                else if (lambdas.isNotEmpty()) {
                    element.getFunctionLiteralArguments()[0]
                }
                else {
                    psiFactory.createExpression("\"Assertion failed\"")
                }

        if (condition == null || messageExpr == null) return

        val message =
                if (messageIsAFunction && messageExpr is JetCallableReferenceExpression) {
                    "${messageExpr.getCallableReference().getText()}()"
                }
                else if (messageIsAFunction) {
                    "${messageExpr.getText()}()"
                }
                else {
                    "${messageExpr.getText()}"
                }

        val assertTypeRef = psiFactory.createType("java.lang.AssertionError")
        ShortenReferences.process(assertTypeRef)

        val text = "if (!true) { throw ${assertTypeRef.getText()}(${message}) }"
        val ifExpression = psiFactory.createExpression(text) as JetIfExpression

        val ifCondition = ifExpression.getCondition() as JetPrefixExpression
        ifCondition.getBaseExpression()?.replace(condition)

        val simplifier = SimplifyNegatedBinaryExpressionIntention()
        if (simplifier.isApplicableTo(ifCondition)) {
            simplifier.applyTo(ifCondition, editor)
        }

        val parent = element.getParent()
        if (parent is JetDotQualifiedExpression) {
            parent.replace(ifExpression)
        } else {
            element.replace(ifExpression)
        }
    }
}
