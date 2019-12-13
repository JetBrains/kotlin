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
import org.jetbrains.kotlin.idea.intentions.splitPropertyDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class UnfoldPropertyToWhenIntention :
    SelfTargetingRangeIntention<KtProperty>(KtProperty::class.java, "Replace property initializer with 'when' expression"),
    LowPriorityAction {
    override fun applicabilityRange(element: KtProperty): TextRange? {
        if (!element.isLocal) return null
        val initializer = element.initializer as? KtWhenExpression ?: return null
        if (!KtPsiUtil.checkWhenExpressionHasSingleElse(initializer)) return null
        if (initializer.entries.any { it.expression == null }) return null
        return TextRange(element.startOffset, initializer.whenKeyword.endOffset)
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val assignment = splitPropertyDeclaration(element) ?: return
        BranchedUnfoldingUtils.unfoldAssignmentToWhen(assignment, editor)
    }
}