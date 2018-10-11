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
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isError

class SpecifyExplicitLambdaSignatureIntention : SelfTargetingOffsetIndependentIntention<KtLambdaExpression>(
    KtLambdaExpression::class.java, "Specify explicit lambda signature"
), LowPriorityAction {

    override fun isApplicableTo(element: KtLambdaExpression): Boolean {
        if (element.functionLiteral.arrow != null && element.valueParameters.all { it.typeReference != null }) return false
        val functionDescriptor = element.analyze(BodyResolveMode.PARTIAL)[BindingContext.FUNCTION, element.functionLiteral] ?: return false
        return functionDescriptor.valueParameters.none { it.type.isError }
    }

    private fun ValueParameterDescriptor.render(psiName: String?): String = IdeDescriptorRenderers.SOURCE_CODE.let {
        "${psiName ?: it.renderName(name, true)}: ${it.renderType(type)}"
    }

    override fun applyTo(element: KtLambdaExpression, editor: Editor?) {
        val functionLiteral = element.functionLiteral
        val functionDescriptor = element.analyze(BodyResolveMode.PARTIAL)[BindingContext.FUNCTION, functionLiteral]!!

        val parameterString = functionDescriptor.valueParameters
            .asSequence()
            .mapIndexed { index, parameterDescriptor ->
                parameterDescriptor.render(psiName = functionLiteral.valueParameters.getOrNull(index)?.let {
                    it.name ?: it.destructuringDeclaration?.text
                })
            }
            .joinToString()
        applyWithParameters(element, parameterString)
    }

    companion object {

        fun KtFunctionLiteral.setParameterListIfAny(psiFactory: KtPsiFactory, newParameterList: KtParameterList?) {
            val oldParameterList = valueParameterList
            if (oldParameterList != null && newParameterList != null) {
                oldParameterList.replace(newParameterList)
            } else {
                val openBraceElement = lBrace
                val nextSibling = openBraceElement.nextSibling
                val addNewline = nextSibling is PsiWhiteSpace && nextSibling.text?.contains("\n") ?: false
                val (whitespace, arrow) = psiFactory.createWhitespaceAndArrow()
                addRangeAfter(whitespace, arrow, openBraceElement)
                if (newParameterList != null) {
                    addAfter(newParameterList, openBraceElement)
                }
                if (addNewline) {
                    addAfter(psiFactory.createNewLine(), openBraceElement)
                }
            }
        }

        fun applyWithParameters(element: KtLambdaExpression, parameterString: String) {
            val psiFactory = KtPsiFactory(element)
            val functionLiteral = element.functionLiteral
            val newParameterList = psiFactory.createLambdaParameterListIfAny(parameterString)
            functionLiteral.setParameterListIfAny(psiFactory, newParameterList)
            ShortenReferences.DEFAULT.process(element.valueParameters)
        }
    }
}
