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

import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.psi.JetPsiFactory
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences

public class MakeTypeExplicitInLambdaIntention : JetSelfTargetingIntention<JetFunctionLiteralExpression>(
        "make.type.explicit.in.lambda", javaClass()) {

    override fun isApplicableTo(element: JetFunctionLiteralExpression): Boolean {
        throw IllegalStateException("isApplicableTo(JetExpressionImpl, Editor) should be called instead")
    }

    override fun isApplicableTo(element: JetFunctionLiteralExpression, editor: Editor): Boolean {
        val openBraceOffset = element.getLeftCurlyBrace().getStartOffset()
        val closeBraceOffset = element.getRightCurlyBrace()?.getStartOffset()
        val caretLocation = editor.getCaretModel().getOffset()
        val arrow = element.getFunctionLiteral().getArrowNode()
        if (arrow != null && !(openBraceOffset < caretLocation && caretLocation < arrow.getStartOffset() + 2) &&
            caretLocation != closeBraceOffset) return false
        else if (arrow == null && caretLocation != openBraceOffset + 1 && caretLocation != closeBraceOffset) return false

        val context = AnalyzerFacadeWithCache.getContextForElement(element)
        val func = context[BindingContext.FUNCTION, element.getFunctionLiteral()]
        if (func == null || ErrorUtils.containsErrorType(func)) return false

        if (hasImplicitReturnType(element) && func.getReturnType() != null) return true
        if (hasImplicitReceiverType(element) && func.getReceiverParameter()?.getType() != null) return true

        val params = element.getValueParameters()
        return params.any { it.getTypeReference() == null }
    }

    override fun applyTo(element: JetFunctionLiteralExpression, editor: Editor) {
        val functionLiteral = element.getFunctionLiteral()
        val context = AnalyzerFacadeWithCache.getContextForElement(element)
        val func = context[BindingContext.FUNCTION, functionLiteral]!!

        // Step 1: make the parameters types explicit
        val valueParameters = func.getValueParameters()
        val parameterString = valueParameters.map({descriptor -> "" + descriptor.getName() +
                                                      ": " + DescriptorRenderer.SOURCE_CODE.renderType(descriptor.getType())
                                                  }).makeString(", ", "(", ")")
        val newParameterList = JetPsiFactory.createParameterList(element.getProject(), parameterString)
        val oldParameterList = functionLiteral.getValueParameterList()
        if (oldParameterList != null) {
            oldParameterList.replace(newParameterList)
        }
        else {
            val openBraceElement = functionLiteral.getOpenBraceNode().getPsi()
            val nextSibling = openBraceElement?.getNextSibling()
            val whitespaceToAdd = (if (nextSibling is PsiWhiteSpace && nextSibling.getText().contains("\n"))
                nextSibling.copy()
            else
                null)
            val whitespaceAndArrow = JetPsiFactory.createWhitespaceAndArrow(element.getProject())
            functionLiteral.addRangeAfter(whitespaceAndArrow.first, whitespaceAndArrow.second, openBraceElement)
            functionLiteral.addAfter(newParameterList, openBraceElement)
            if (whitespaceToAdd != null) {
                functionLiteral.addAfter(whitespaceToAdd, openBraceElement)
            }
        }
        ShortenReferences.process(element.getValueParameters())

        // Step 2: make the return type explicit
        val expectedReturnType = func.getReturnType()
        if (hasImplicitReturnType(element) && expectedReturnType != null) {
            val paramList = functionLiteral.getValueParameterList()
            val returnTypeColon = JetPsiFactory.createColon(element.getProject())
            val returnTypeExpr = JetPsiFactory.createType(element.getProject(), DescriptorRenderer.SOURCE_CODE.renderType(expectedReturnType))
            ShortenReferences.process(returnTypeExpr)
            functionLiteral.addAfter(returnTypeExpr, paramList)
            functionLiteral.addAfter(returnTypeColon, paramList)
        }

        // Step 3: make the receiver type explicit
        val expectedReceiverType = func.getReceiverParameter()?.getType()
        if (hasImplicitReceiverType(element) && expectedReceiverType != null) {
            val receiverTypeString = DescriptorRenderer.SOURCE_CODE.renderType(expectedReceiverType)
            val paramListString = functionLiteral.getValueParameterList()?.getText()
            val paramListWithReceiver = JetPsiFactory.createExpression(element.getProject(), receiverTypeString + "." + paramListString)
            ShortenReferences.process(paramListWithReceiver)
            functionLiteral.getValueParameterList()?.replace(paramListWithReceiver)
        }
    }

    private fun hasImplicitReturnType(element: JetFunctionLiteralExpression): Boolean {
        return !element.hasDeclaredReturnType()
    }

    private fun hasImplicitReceiverType(element: JetFunctionLiteralExpression): Boolean {
        return element.getFunctionLiteral().getReceiverTypeRef() == null
    }
}