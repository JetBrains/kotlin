/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class ConvertRangeCheckToTwoComparisonsIntention : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(
    KtBinaryExpression::class.java,
    "Convert to comparisons"
) {
    private fun KtExpression?.isSimple() = this is KtConstantExpression || this is KtNameReferenceExpression

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        if (element.operationToken != KtTokens.IN_KEYWORD) return
        val rangeExpression = element.right as? KtBinaryExpression ?: return
        val min = rangeExpression.left ?: return
        val arg = element.left ?: return
        val max = rangeExpression.right ?: return
        val comparisonsExpression = KtPsiFactory(element).createExpressionByPattern("$0 <= $1 && $1 <= $2", min, arg, max)
        element.replace(comparisonsExpression)
    }

    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        if (element.operationToken != KtTokens.IN_KEYWORD) return false

        // ignore for-loop. for(x in 1..2) should not be convert to for(1<=x && x<=2)
        if (element.parent is KtForExpression) return false

        val rangeExpression = element.right as? KtBinaryExpression ?: return false
        if (rangeExpression.operationToken != KtTokens.RANGE) return false

        return element.left.isSimple() && rangeExpression.left.isSimple() && rangeExpression.right.isSimple()
    }
}