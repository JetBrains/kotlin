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
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class UnfoldPropertyToIfIntention :
    SelfTargetingRangeIntention<KtProperty>(KtProperty::class.java, "Replace property initializer with 'if' expression"),
    LowPriorityAction {
    override fun applicabilityRange(element: KtProperty): TextRange? {
        if (!element.isLocal) return null
        val initializer = element.initializer as? KtIfExpression ?: return null
        return TextRange(element.startOffset, initializer.ifKeyword.endOffset)
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val assignment = splitPropertyDeclaration(element)
            ?: return // if element initializer is null the apply should not be invoked. If suddenly invoked splitPropertyDeclaration will return null
        BranchedUnfoldingUtils.unfoldAssignmentToIf(assignment, editor)
    }
}