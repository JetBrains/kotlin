/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedFoldingUtils
import org.jetbrains.kotlin.psi.*

class FoldIfToReturnAsymmetricallyIntention :
    SelfTargetingRangeIntention<KtIfExpression>(KtIfExpression::class.java, "Replace 'if' expression with return") {
    override fun applicabilityRange(element: KtIfExpression): TextRange? {
        if (BranchedFoldingUtils.getFoldableBranchedReturn(element.then) == null || element.`else` != null) {
            return null
        }

        val nextElement = KtPsiUtil.skipTrailingWhitespacesAndComments(element) as? KtReturnExpression
        if (nextElement?.returnedExpression == null) return null
        return element.ifKeyword.textRange
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val condition = element.condition!!
        val thenBranch = element.then!!
        val elseBranch = KtPsiUtil.skipTrailingWhitespacesAndComments(element) as KtReturnExpression

        val psiFactory = KtPsiFactory(element)
        val newIfExpression = psiFactory.createIf(condition, thenBranch, elseBranch)

        val thenReturn = BranchedFoldingUtils.getFoldableBranchedReturn(newIfExpression.then!!)!!
        val elseReturn = BranchedFoldingUtils.getFoldableBranchedReturn(newIfExpression.`else`!!)!!

        thenReturn.replace(thenReturn.returnedExpression!!)
        elseReturn.replace(elseReturn.returnedExpression!!)

        element.replace(psiFactory.createExpressionByPattern("return $0", newIfExpression))
        elseBranch.delete()
    }
}