/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern

class ReplaceWithOrdinaryAssignmentIntention : SelfTargetingIntention<KtBinaryExpression>(
    KtBinaryExpression::class.java,
    KotlinBundle.lazyMessage("replace.with.ordinary.assignment")
), LowPriorityAction {
    override fun isApplicableTo(element: KtBinaryExpression, caretOffset: Int): Boolean {
        if (element.operationToken !in KtTokens.AUGMENTED_ASSIGNMENTS) return false
        if (element.left !is KtNameReferenceExpression) return false
        if (element.right == null) return false
        return element.operationReference.textRange.containsOffset(caretOffset)
    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val left = element.left!!
        val right = element.right!!
        val factory = KtPsiFactory(element)

        val assignOpText = element.operationReference.text
        assert(assignOpText.endsWith("="))
        val operationText = assignOpText.substring(0, assignOpText.length - 1)

        element.replace(factory.createExpressionByPattern("$0 = $0 $operationText $1", left, right))
    }
}
