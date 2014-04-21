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

import org.jetbrains.jet.lang.psi.JetCallExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetValueArgumentList
import org.jetbrains.jet.lang.psi.JetValueArgument
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression
import org.jetbrains.jet.lang.psi.JetBinaryExpressionWithTypeRHS
import org.jetbrains.jet.lang.psi.JetPsiUnparsingUtils
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import javax.naming.Binding
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetFile

public class ReplaceWithInfixFunctionCallIntention : JetSelfTargetingIntention<JetCallExpression>("replace.with.infix.function.call.intention", javaClass()) {
    override fun isApplicableTo(element: JetCallExpression): Boolean {
        throw IllegalStateException("isApplicableTo(JetExpressionImpl, Editor) should be called instead")
    }

    override fun isApplicableTo(element: JetCallExpression, editor: Editor): Boolean {
        val caretLocation = editor.getCaretModel().getOffset()

        val calleeExpr = element.getCalleeExpression()
        if (calleeExpr == null) return false

        val textRange = calleeExpr.getTextRange()
        if (textRange == null) return false

        if (caretLocation !in textRange) return false

        val parent = element.getParent()

        if (parent is JetDotQualifiedExpression) {
            val callee = element.getCalleeExpression()
            val typeArguments = element.getTypeArgumentList()
            val valueArguments = element.getValueArgumentList()
            val functionLiteralArguments = element.getFunctionLiteralArguments()
            val numOfTotalValueArguments = (valueArguments?.getArguments()?.size() ?: 0) + functionLiteralArguments.size()

            if (typeArguments?.getArguments()?.size() ?: 0 == 0 &&
                numOfTotalValueArguments == 1 &&
                callee != null) {
                if (valueArguments?.getArguments()?.size() == 1 && valueArguments?.getArguments()?.first()?.isNamed() ?: false) {
                    val file: JetFile = element.getContainingJetFile()
                    val bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache(file).getBindingContext()
                    val descriptor = bindingContext.get(BindingContext.RESOLVED_CALL, callee)
                    val valueArgumentsMap = descriptor?.getValueArguments()
                    val firstArgument = valueArguments?.getArguments()?.first()

                    return valueArgumentsMap?.keySet()?.any { it.getName().asString() == firstArgument?.getArgumentName()?.getText() && it.getIndex() == 0 } ?: false
                } else {
                    return true
                }
            } else {
                return false
            }
        } else {
            return false
        }
    }

    override fun applyTo(element: JetCallExpression, editor: Editor) {
        val parent = element.getParent() as JetDotQualifiedExpression
        val leftHandText = parent.getReceiverExpression().getText()
        val rightHandTextStringBuilder = StringBuilder()
        val operatorText = element.getCalleeExpression()!!.getText()
        val valueArguments = element.getValueArgumentList()?.getArguments() ?: listOf<JetValueArgument>()
        val functionLiteralArguments = element.getFunctionLiteralArguments()

        rightHandTextStringBuilder.append(
                if (valueArguments.size() > 0)
                    JetPsiUnparsingUtils.parenthesizeIfNeeded(valueArguments.first().getArgumentExpression())
                else
                    functionLiteralArguments.first().getText()
        )

        val replacement = JetPsiFactory.createExpression(element.getProject(), "$leftHandText $operatorText ${rightHandTextStringBuilder.toString()}")

        parent.replace(replacement)
    }
}

