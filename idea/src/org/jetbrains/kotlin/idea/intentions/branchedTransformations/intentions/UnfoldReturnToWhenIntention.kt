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
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class UnfoldReturnToWhenIntention : LowPriorityAction, SelfTargetingRangeIntention<KtReturnExpression>(
    KtReturnExpression::class.java, KotlinBundle.lazyMessage("replace.return.with.when.expression")
) {
    override fun applicabilityRange(element: KtReturnExpression): TextRange? {
        val whenExpr = element.returnedExpression as? KtWhenExpression ?: return null
        if (!KtPsiUtil.checkWhenExpressionHasSingleElse(whenExpr)) return null
        if (whenExpr.entries.any { it.expression == null }) return null
        return TextRange(element.startOffset, whenExpr.whenKeyword.endOffset)
    }

    override fun applyTo(element: KtReturnExpression, editor: Editor?) {
        val psiFactory = KtPsiFactory(element)
        val context = element.analyze()

        val whenExpression = element.returnedExpression as KtWhenExpression
        val newWhenExpression = whenExpression.copied()
        val labelName = element.getLabelName()
        whenExpression.entries.zip(newWhenExpression.entries).forEach { (entry, newEntry) ->
            val expr = entry.expression!!.lastBlockStatementOrThis()
            val newExpr = newEntry.expression!!.lastBlockStatementOrThis()
            newExpr.replace(UnfoldReturnToIfIntention.createReturnExpression(expr, labelName, psiFactory, context))
        }

        element.replace(newWhenExpression)
    }
}
