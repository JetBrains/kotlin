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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class SpecifyExplicitLambdaSignatureIntention : SelfTargetingIntention<KtLambdaExpression>(KtLambdaExpression::class.java, "Specify explicit lambda signature"), LowPriorityAction {

    override fun isApplicableTo(element: KtLambdaExpression, caretOffset: Int): Boolean {
        val arrow = element.functionLiteral.arrow
        if (arrow != null) {
            if (caretOffset > arrow.endOffset) return false
            if (element.valueParameters.all { it.typeReference != null }) return false
        }
        else {
            if (!element.leftCurlyBrace.textRange.containsOffset(caretOffset)) return false
        }

        val functionDescriptor = element.analyze(BodyResolveMode.PARTIAL)[BindingContext.FUNCTION, element.functionLiteral] ?: return false
        return functionDescriptor.valueParameters.none { it.type.isError }
    }

    override fun applyTo(element: KtLambdaExpression, editor: Editor?) {
        val psiFactory = KtPsiFactory(element)
        val functionLiteral = element.functionLiteral
        val functionDescriptor = element.analyze(BodyResolveMode.PARTIAL)[BindingContext.FUNCTION, functionLiteral]!!

        val parameterString = functionDescriptor.valueParameters
                .map { "${it.name}: ${IdeDescriptorRenderers.SOURCE_CODE.renderType(it.type)}" }
                .joinToString(", ")

        val newParameterList = psiFactory.createFunctionLiteralParameterList(parameterString)
        val oldParameterList = functionLiteral.valueParameterList
        if (oldParameterList != null) {
            oldParameterList.replace(newParameterList)
        }
        else {
            val openBraceElement = functionLiteral.lBrace
            val nextSibling = openBraceElement.nextSibling
            val addNewline = nextSibling is PsiWhiteSpace && nextSibling.text?.contains("\n") ?: false
            val (whitespace, arrow) = psiFactory.createWhitespaceAndArrow()
            functionLiteral.addRangeAfter(whitespace, arrow, openBraceElement)
            functionLiteral.addAfter(newParameterList, openBraceElement)
            if (addNewline) {
                functionLiteral.addAfter(psiFactory.createNewLine(), openBraceElement)
            }
        }
        ShortenReferences.DEFAULT.process(element.valueParameters)
    }
}
