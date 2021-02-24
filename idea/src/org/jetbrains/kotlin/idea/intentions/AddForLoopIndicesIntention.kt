/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class AddForLoopIndicesIntention : SelfTargetingRangeIntention<KtForExpression>(
    KtForExpression::class.java,
    KotlinBundle.lazyMessage("add.indices.to.for.loop"),
), LowPriorityAction {
    private val WITH_INDEX_NAME = "withIndex"
    private val WITH_INDEX_FQ_NAMES: Set<String> by lazy {
        sequenceOf("collections", "sequences", "text", "ranges").map { "kotlin.$it.$WITH_INDEX_NAME" }.toSet()
    }

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

        when (val body = forExpression.body) {
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

    private fun createWithIndexExpression(originalExpression: KtExpression, reformat: Boolean): KtExpression =
        KtPsiFactory(originalExpression).createExpressionByPattern(
            "$0.$WITH_INDEX_NAME()", originalExpression,
            reformat = reformat
        )
}
