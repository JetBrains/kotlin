/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.typeUtil.isNothing

class UnfoldReturnToIfIntention : LowPriorityAction, SelfTargetingRangeIntention<KtReturnExpression>(
    KtReturnExpression::class.java, KotlinBundle.lazyMessage("replace.return.with.if.expression")
) {
    override fun applicabilityRange(element: KtReturnExpression): TextRange? {
        val ifExpression = element.returnedExpression as? KtIfExpression ?: return null
        return TextRange(element.startOffset, ifExpression.ifKeyword.endOffset)
    }

    override fun applyTo(element: KtReturnExpression, editor: Editor?) {
        val ifExpression = element.returnedExpression as KtIfExpression
        val thenExpr = ifExpression.then!!.lastBlockStatementOrThis()
        val elseExpr = ifExpression.`else`!!.lastBlockStatementOrThis()

        val newIfExpression = ifExpression.copied()
        val newThenExpr = newIfExpression.then!!.lastBlockStatementOrThis()
        val newElseExpr = newIfExpression.`else`!!.lastBlockStatementOrThis()

        val psiFactory = KtPsiFactory(element)
        val context = element.analyze()

        val labelName = element.getLabelName()
        newThenExpr.replace(createReturnExpression(thenExpr, labelName, psiFactory, context))
        newElseExpr.replace(createReturnExpression(elseExpr, labelName, psiFactory, context))
        element.replace(newIfExpression)
    }

    companion object {
        fun createReturnExpression(
            expr: KtExpression,
            labelName: String?,
            psiFactory: KtPsiFactory,
            context: BindingContext
        ): KtExpression {
            val label = labelName?.let { "@$it" } ?: ""
            val returnText = when (expr) {
                is KtBreakExpression, is KtContinueExpression, is KtReturnExpression, is KtThrowExpression -> ""
                else -> if (expr.getResolvedCall(context)?.resultingDescriptor?.returnType?.isNothing() == true) "" else "return$label "
            }

            return psiFactory.createExpressionByPattern("$returnText$0", expr)
        }
    }

}