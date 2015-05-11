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

import org.jetbrains.kotlin.psi.JetFunctionLiteralExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.psi.JetPsiFactory
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.psiUtil.endOffset

public class SpecifyExplicitLambdaSignatureIntention : JetSelfTargetingIntention<JetFunctionLiteralExpression>(javaClass(), "Specify explicit lambda signature") {

    override fun isApplicableTo(element: JetFunctionLiteralExpression, caretOffset: Int): Boolean {
        val arrow = element.getFunctionLiteral().getArrow()
        if (arrow != null) {
            if (caretOffset > arrow.endOffset) return false
            if (element.getValueParameters().all { it.getTypeReference() != null }) return false
        }
        else {
            if (!element.getLeftCurlyBrace().getTextRange().containsOffset(caretOffset)) return false
        }

        val functionDescriptor = element.analyze()[BindingContext.FUNCTION, element.getFunctionLiteral()] ?: return false
        return functionDescriptor.getValueParameters().none { it.getType().isError() }
    }

    override fun applyTo(element: JetFunctionLiteralExpression, editor: Editor) {
        val psiFactory = JetPsiFactory(element)
        val functionLiteral = element.getFunctionLiteral()
        val functionDescriptor = element.analyze()[BindingContext.FUNCTION, functionLiteral]!!

        val parameterString = functionDescriptor.getValueParameters()
                .map { "${it.getName()}: ${IdeDescriptorRenderers.SOURCE_CODE.renderType(it.getType())}" }
                .joinToString(", ")

        val newParameterList = psiFactory.createFunctionLiteralParameterList(parameterString)
        val oldParameterList = functionLiteral.getValueParameterList()
        if (oldParameterList != null) {
            oldParameterList.replace(newParameterList)
        }
        else {
            val openBraceElement = functionLiteral.getLBrace()
            val nextSibling = openBraceElement.getNextSibling()
            val addNewline = nextSibling is PsiWhiteSpace && nextSibling.getText()?.contains("\n") ?: false
            val (whitespace, arrow) = psiFactory.createWhitespaceAndArrow()
            functionLiteral.addRangeAfter(whitespace, arrow, openBraceElement)
            functionLiteral.addAfter(newParameterList, openBraceElement)
            if (addNewline) {
                functionLiteral.addAfter(psiFactory.createNewLine(), openBraceElement)
            }
        }
        ShortenReferences.DEFAULT.process(element.getValueParameters())
    }
}
