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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class AddForLoopIndicesIntention : SelfTargetingRangeIntention<KtForExpression>(KtForExpression::class.java, "Add indices to 'for' loop"),
    LowPriorityAction {
    private val WITH_INDEX_NAME = "withIndex"
    private val WITH_INDEX_FQ_NAMES = listOf("collections", "sequences", "text", "ranges").map { "kotlin.$it.$WITH_INDEX_NAME" }.toSet()

    override fun applicabilityRange(element: KtForExpression): TextRange? {
        if (element.loopParameter == null) return null
        if (element.loopParameter?.destructuringDeclaration != null) return null
        val loopRange = element.loopRange ?: return null

        val bindingContext = element.analyze(BodyResolveMode.PARTIAL_WITH_CFA)

        val resolvedCall = loopRange.getResolvedCall(bindingContext)
        if (resolvedCall?.resultingDescriptor?.fqNameUnsafe?.asString() in WITH_INDEX_FQ_NAMES) return null // already withIndex() call

        val potentialExpression = createWithIndexExpression(loopRange, reformat = false)

        val newBindingContext = potentialExpression.analyzeAsReplacement(loopRange, bindingContext)
        val newResolvedCall = potentialExpression.getResolvedCall(newBindingContext) ?: return null
        if (newResolvedCall.resultingDescriptor.fqNameUnsafe.asString() !in WITH_INDEX_FQ_NAMES) return null

        return TextRange(element.startOffset, element.body?.startOffset ?: element.endOffset)
    }

    override fun applyTo(element: KtForExpression, editor: Editor?) {
        if (editor == null) throw IllegalArgumentException("This intention requires an editor")
        val loopRange = element.loopRange!!
        val loopParameter = element.loopParameter!!
        val psiFactory = KtPsiFactory(element)

        loopRange.replace(createWithIndexExpression(loopRange, reformat = true))

        var multiParameter = (psiFactory.createExpressionByPattern(
            "for((index, $0) in x){}",
            loopParameter.text
        ) as KtForExpression).destructuringDeclaration!!

        multiParameter = loopParameter.replaced(multiParameter)

        val indexVariable = multiParameter.entries[0]
        editor.caretModel.moveToOffset(indexVariable.startOffset)

        runTemplate(editor, element, indexVariable)
    }

    private fun runTemplate(editor: Editor, forExpression: KtForExpression, indexVariable: KtDestructuringDeclarationEntry) {
        PsiDocumentManager.getInstance(forExpression.project).doPostponedOperationsAndUnblockDocument(editor.document)

        val templateBuilder = TemplateBuilderImpl(forExpression)
        templateBuilder.replaceElement(indexVariable, ChooseStringExpression(listOf("index", "i")))

        val body = forExpression.body
        when (body) {
            is KtBlockExpression -> {
                val statement = body.statements.firstOrNull()
                if (statement != null) {
                    templateBuilder.setEndVariableBefore(statement)
                } else {
                    templateBuilder.setEndVariableAfter(body.lBrace)
                }
            }

            null -> forExpression.rightParenthesis.let { templateBuilder.setEndVariableAfter(it) }

            else -> templateBuilder.setEndVariableBefore(body)
        }

        templateBuilder.run(editor, true)
    }

    private fun createWithIndexExpression(originalExpression: KtExpression, reformat: Boolean): KtExpression {
        return KtPsiFactory(originalExpression).createExpressionByPattern(
            "$0.$WITH_INDEX_NAME()", originalExpression,
            reformat = reformat
        )
    }
}
