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
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import org.jetbrains.jet.lang.psi.JetCallableReferenceExpression
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import kotlin.properties.Delegates
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

public class ConvertAssertToIfWithThrowIntention : JetSelfTargetingIntention<JetCallExpression>(
        "convert.assert.to.if.with.throw", javaClass()) {

    private var messageIsAFunction : Boolean by Delegates.notNull()

    override fun isApplicableTo(element: JetCallExpression): Boolean {
        if (element.getCalleeExpression()?.getText() != "assert") return false

        val arguments = element.getValueArguments().size
        val lambdas = element.getFunctionLiteralArguments().size
        if (!(arguments == 1 && (lambdas == 1 || lambdas == 0)) && arguments != 2) return false

        val context = AnalyzerFacadeWithCache.getContextForElement(element)
        val resolvedCall = context[BindingContext.RESOLVED_CALL, element.getCalleeExpression()]
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

        val messageExpr: JetExpression?
        if (args.size == 2) {
            messageExpr = args[1]?.getArgumentExpression()
        } else if (lambdas.isNotEmpty()) {
            messageExpr = element.getFunctionLiteralArguments()[0]
        } else {
            messageExpr = JetPsiFactory.createExpression(element.getProject(), "\"Assertion failed\"")
        }

        if (condition == null || messageExpr == null) return

        val message: String
        if (messageIsAFunction && messageExpr is JetCallableReferenceExpression) {
            message = "${messageExpr.getCallableReference().getText()}()"
        } else if (messageIsAFunction) {
            message = "${messageExpr.getText()}()"
        } else {
            message = "${messageExpr.getText()}"
        }

        val negatedCondition = JetPsiFactory.createExpression(element.getProject(), "!true") as JetPrefixExpression
        negatedCondition.getBaseExpression()?.replace(condition)

        val simplifier = SimplifyNegatedBinaryExpressionIntention()
        if (simplifier.isApplicableTo(negatedCondition)) {
            simplifier.applyTo(negatedCondition, editor)
        }

        val assertTypeRef = JetPsiFactory.createType(element.getProject(), "java.lang.AssertionError")
        ShortenReferences.process(assertTypeRef)

        val text = "if (${negatedCondition.getText()}) { throw ${assertTypeRef.getText()}(${message}) }"
        element.replace(JetPsiFactory.createExpression(element.getProject(), text))
    }
}