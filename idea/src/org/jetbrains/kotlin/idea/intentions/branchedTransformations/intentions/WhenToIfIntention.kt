/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.combineWhenConditions
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.buildExpression

class WhenToIfIntention : SelfTargetingRangeIntention<KtWhenExpression>(KtWhenExpression::class.java, "Replace 'when' with 'if'"),
    LowPriorityAction {
    override fun applicabilityRange(element: KtWhenExpression): TextRange? {
        val entries = element.entries
        if (entries.isEmpty()) return null
        val lastEntry = entries.last()
        if (entries.any { it != lastEntry && it.isElse }) return null
        if (entries.all { it.isElse }) return null // 'when' with only 'else' branch is not supported
        return element.whenKeyword.textRange
    }

    override fun applyTo(element: KtWhenExpression, editor: Editor?) {
        val commentSaver = CommentSaver(element)

        val factory = KtPsiFactory(element)
        val ifExpression = factory.buildExpression {
            val entries = element.entries
            for ((i, entry) in entries.withIndex()) {
                if (i > 0) {
                    appendFixedText("else ")
                }
                val branch = entry.expression
                if (entry.isElse) {
                    appendExpression(branch)
                } else {
                    val condition = factory.combineWhenConditions(entry.conditions, element.subjectExpression)
                    appendFixedText("if (")
                    appendExpression(condition)
                    appendFixedText(")")
                    if (branch is KtIfExpression) {
                        appendFixedText("{ ")
                    }
                    appendExpression(branch)
                    if (branch is KtIfExpression) {
                        appendFixedText(" }")
                    }
                }
                if (i != entries.lastIndex) {
                    appendFixedText("\n")
                }
            }
        }

        val result = element.replace(ifExpression)
        commentSaver.restore(result)
    }
}
