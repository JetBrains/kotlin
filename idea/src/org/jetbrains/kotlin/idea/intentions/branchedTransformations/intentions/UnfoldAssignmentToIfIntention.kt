/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.BranchedUnfoldingUtils
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class UnfoldAssignmentToIfIntention :
    SelfTargetingRangeIntention<KtBinaryExpression>(KtBinaryExpression::class.java, "Replace assignment with 'if' expression"),
    LowPriorityAction {
    override fun applicabilityRange(element: KtBinaryExpression): TextRange? {
        if (element.operationToken !in KtTokens.ALL_ASSIGNMENTS) return null
        if (element.left == null) return null
        val right = element.right as? KtIfExpression ?: return null
        return TextRange(element.startOffset, right.ifKeyword.endOffset)
    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        BranchedUnfoldingUtils.unfoldAssignmentToIf(element, editor)
    }
}