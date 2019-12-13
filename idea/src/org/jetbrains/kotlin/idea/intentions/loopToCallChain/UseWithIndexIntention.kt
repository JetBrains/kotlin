/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.loopToCallChain

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern

@Suppress("DEPRECATION")
class UseWithIndexInspection : IntentionBasedInspection<KtForExpression>(UseWithIndexIntention::class)

class UseWithIndexIntention : SelfTargetingRangeIntention<KtForExpression>(
    KtForExpression::class.java,
    "Use withIndex() instead of manual index increment"
) {
    override fun applicabilityRange(element: KtForExpression): TextRange? {
        return if (matchIndexToIntroduce(element, reformat = false) != null) element.forKeyword.textRange else null
    }

    override fun applyTo(element: KtForExpression, editor: Editor?) {
        val (indexVariable, initializationStatement, incrementExpression) = matchIndexToIntroduce(element, reformat = true)!!

        val factory = KtPsiFactory(element)
        val loopRange = element.loopRange!!
        val loopParameter = element.loopParameter!!

        val newLoopRange = factory.createExpressionByPattern("$0.withIndex()", loopRange)
        loopRange.replace(newLoopRange)

        val multiParameter = (factory.createExpressionByPattern(
            "for(($0, $1) in x){}",
            indexVariable.nameAsSafeName,
            loopParameter.text
        ) as KtForExpression).loopParameter!!
        loopParameter.replace(multiParameter)

        initializationStatement.delete()
        if (incrementExpression.parent is KtBlockExpression) {
            incrementExpression.delete()
        } else {
            removePlusPlus(incrementExpression, true)
        }
    }
}