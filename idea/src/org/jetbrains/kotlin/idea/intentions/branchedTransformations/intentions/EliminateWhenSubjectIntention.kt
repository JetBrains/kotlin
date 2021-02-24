/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.SelfTargetingIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.toExpression
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.buildExpression
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class EliminateWhenSubjectIntention :
    SelfTargetingIntention<KtWhenExpression>(KtWhenExpression::class.java, KotlinBundle.lazyMessage("eliminate.argument.of.when")),
    LowPriorityAction {
    override fun isApplicableTo(element: KtWhenExpression, caretOffset: Int): Boolean {
        if (element.subjectExpression !is KtNameReferenceExpression) return false
        val lBrace = element.openBrace ?: return false
        if (caretOffset > lBrace.startOffset) return false
        val lastEntry = element.entries.lastOrNull()
        if (lastEntry?.isElse != true && element.isUsedAsExpression(element.analyze(BodyResolveMode.PARTIAL_WITH_CFA))) return false
        return true
    }

    override fun applyTo(element: KtWhenExpression, editor: Editor?) {
        val subject = element.subjectExpression ?: return

        val commentSaver = CommentSaver(element, saveLineBreaks = true)

        val whenExpression = KtPsiFactory(element).buildExpression {
            appendFixedText("when {\n")

            for (entry in element.entries) {
                val branchExpression = entry.expression

                if (entry.isElse) {
                    appendFixedText("else")
                } else {
                    appendExpressions(entry.conditions.map { it.toExpression(subject) }, separator = "||")
                }
                appendFixedText("->")

                appendExpression(branchExpression)
                appendFixedText("\n")
            }

            appendFixedText("}")
        }

        val result = element.replace(whenExpression)
        commentSaver.restore(result)
    }
}
