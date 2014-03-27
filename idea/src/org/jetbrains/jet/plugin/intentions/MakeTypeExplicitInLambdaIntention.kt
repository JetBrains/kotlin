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
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.psi.JetPsiFactory
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences

public class MakeTypeExplicitInLambdaIntention : JetSelfTargetingIntention<JetFunctionLiteralExpression>(
        "make.type.explicit.in.lambda", javaClass()) {
    override fun isApplicableTo(element: JetFunctionLiteralExpression): Boolean {
        val params = element.getValueParameters()
        val hasDeclaredParamsType = params.any { it.getTypeReference() != null }
        val hasDeclaredReturnType = element.hasDeclaredReturnType()
        val hasDeclaredReceiverType = element.getFunctionLiteral().getReceiverTypeRef() != null

        val context = AnalyzerFacadeWithCache.analyzeFileWithCache(element.getContainingFile() as JetFile).getBindingContext()
        val func = context.get(BindingContext.FUNCTION, element.getFunctionLiteral())
        if (func == null || ErrorUtils.containsErrorType(func)) return false

        val expectedReturnType = func!!.getReturnType()
        val expectedReceiverType = func!!.getReceiverParameter()?.getType()

        if (!hasDeclaredReturnType && expectedReturnType != null) return true
        if (!hasDeclaredReceiverType && expectedReceiverType != null) return true
        if (!hasDeclaredParamsType && func!!.getValueParameters() != null) return true

        return false
    }

    override fun applyTo(element: JetFunctionLiteralExpression, editor: Editor) {
        val params = element.getFunctionLiteral().getValueParameterList()
        val context = AnalyzerFacadeWithCache.analyzeFileWithCache(element.getContainingFile() as JetFile).getBindingContext()
        val func = context.get(BindingContext.FUNCTION, element.getFunctionLiteral())!!
        val valueParameters = func!!.getValueParameters()
        val expectedReturnType = func!!.getReturnType()
        val expectedReceiverType = func!!.getReceiverParameter()?.getType()
        val parameterString = StringUtil.join(valueParameters,
                                              {(descriptor: ValueParameterDescriptor?): String ->
                                                  "" + descriptor!!.getName() +
                                                  ": " + DescriptorRenderer.TEXT.renderType(descriptor!!.getType())
                                              },
                                              ", ");

        // Step 1: make the parameters types explicit
        val newParameterList = JetPsiFactory.createParameterList(element.getProject(), "(" + parameterString + ")")
        if (params != null) {
            params.replace(newParameterList)
        }
        else {
            val openBraceElement = element.getFunctionLiteral().getOpenBraceNode().getPsi()
            val nextSibling = openBraceElement?.getNextSibling()
            val whitespaceToAdd = (if (nextSibling is PsiWhiteSpace && nextSibling?.getText()?.contains("\n")?: false)
                nextSibling?.copy()
            else
                null)
            val whitespaceAndArrow = JetPsiFactory.createWhitespaceAndArrow(element.getProject())
            element.getFunctionLiteral().addRangeAfter(whitespaceAndArrow.first, whitespaceAndArrow.second, openBraceElement)
            element.getFunctionLiteral().addAfter(newParameterList, openBraceElement)
            if (whitespaceToAdd != null) {
                element.getFunctionLiteral().addAfter(whitespaceToAdd, openBraceElement)
            }
        }
        ShortenReferences.process(element.getValueParameters())

        // Step 2: make the return type explicit
        if (expectedReturnType != null) {
            val paramList = element.getFunctionLiteral().getValueParameterList()
            val returnTypeColon = JetPsiFactory.createColon(element.getProject())
            val returnTypeExpr = JetPsiFactory.createType(element.getProject(), DescriptorRenderer.TEXT.renderType(expectedReturnType))
            ShortenReferences.process(returnTypeExpr)
            element.getFunctionLiteral().addAfter(returnTypeExpr, paramList)
            element.getFunctionLiteral().addAfter(returnTypeColon, paramList)
        }

        // Step 3: make the receiver type explicit
        if (expectedReceiverType != null) {
            val receiverTypeString = DescriptorRenderer.TEXT.renderType(expectedReceiverType)
            val paramListString = element.getFunctionLiteral().getValueParameterList()?.getText()
            val paramListWithReceiver = JetPsiFactory.createExpression(element.getProject(), receiverTypeString + "." + paramListString)
            ShortenReferences.process(paramListWithReceiver)
            element.getFunctionLiteral().getValueParameterList()?.replace(paramListWithReceiver)
        }
    }
}