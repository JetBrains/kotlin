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
import org.jetbrains.jet.lang.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.lang.psi.JetThrowExpression
import org.jetbrains.jet.lang.psi.psiUtil.replaced

public class ConvertAssertToIfWithThrowIntention : JetSelfTargetingIntention<JetCallExpression>(
        "convert.assert.to.if.with.throw", javaClass()) {

    private var messageIsAFunction : Boolean by Delegates.notNull()

    override fun isApplicableTo(element: JetCallExpression): Boolean {
        if (element.getCalleeExpression()?.getText() != "assert") return false

        val argumentSize = element.getValueArguments().size
        if (argumentSize !in 1..2) return false
        if (element.getFunctionLiteralArguments().size == 1 && argumentSize == 1) return false

        val context = AnalyzerFacadeWithCache.getContextForElement(element)
        val resolvedCall = element.getResolvedCall(context)
        if (resolvedCall == null) return false

        val valParameters = resolvedCall.getResultingDescriptor().getValueParameters()
        if (valParameters.size > 1) {
            messageIsAFunction = (valParameters[1].getType() != KotlinBuiltIns.getInstance().getAnyType())
        } else {
            messageIsAFunction = false
        }

        return DescriptorUtils.getFqName(resolvedCall.getResultingDescriptor()).asString() == "kotlin.assert"
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val args = element.getValueArguments()
        val conditionText = args[0]?.getArgumentExpression()?.getText() ?: return
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

        if (messageExpr == null) return

        val replaced = replaceWithIfThenThrowExpression(element)

        ShortenReferences.process(replaced.getThen()!!)

        fun replaceMessage() {
            val thrownExpression = ((replaced.getThen() as JetBlockExpression).getStatements().first() as JetThrowExpression).getThrownExpression()
            val assertionErrorCall = if (thrownExpression is JetCallExpression) {
                thrownExpression: JetCallExpression
            }
            else {
                (thrownExpression as JetDotQualifiedExpression).getSelectorExpression() as JetCallExpression
            }

            val message = psiFactory.createExpression(
                    if (messageIsAFunction && messageExpr is JetCallableReferenceExpression) {
                        "${messageExpr.getCallableReference().getText()}()"
                    }
                    else if (messageIsAFunction) {
                        "${messageExpr.getText()}()"
                    }
                    else {
                        "${messageExpr.getText()}"
                    }
            )
            assertionErrorCall.getValueArguments().single()!!.getArgumentExpression()!!.replace(message)
        }

        fun replaceCondition() {
            val ifCondition = replaced.getCondition() as JetPrefixExpression
            ifCondition.getBaseExpression()!!.replace(psiFactory.createExpression(conditionText))
        }

        replaceCondition()
        replaceMessage()

        simplifyConditionIfPossible(editor, replaced)
    }

    private fun simplifyConditionIfPossible(editor: Editor, replaced: JetIfExpression) {
        val condition = replaced.getCondition() as JetPrefixExpression
        val simplifier = SimplifyNegatedBinaryExpressionIntention()
        if (simplifier.isApplicableTo(condition)) {
            simplifier.applyTo(condition, editor)
        }
    }

    private fun replaceWithIfThenThrowExpression(original: JetCallExpression): JetIfExpression {
        val replacement = JetPsiFactory(original).createExpression("if (!true) { throw java.lang.AssertionError(\"\") }") as JetIfExpression
        val parent = original.getParent()
        return if (parent is JetDotQualifiedExpression) {
            parent.replaced(replacement)
        }
        else {
            original.replaced(replacement)
        }
    }
}
