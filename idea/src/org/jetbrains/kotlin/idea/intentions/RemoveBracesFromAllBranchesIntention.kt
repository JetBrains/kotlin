/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.intentions.AddBracesToAllBranchesIntention.Companion.allBranchExpressions
import org.jetbrains.kotlin.psi.*

class RemoveBracesFromAllBranchesIntention : SelfTargetingIntention<KtExpression>(
    KtExpression::class.java,
    KotlinBundle.lazyMessage("remove.braces.from.all.branches")
) {
    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        if (!AddBracesToAllBranchesIntention.isCaretOnIfOrWhenKeyword(element, caretOffset)) return false
        val targetBranchExpressions = element.targetBranchExpressions()
        if (targetBranchExpressions.isEmpty() || targetBranchExpressions.any { !RemoveBracesIntention.isApplicableTo(it) }) return false
        when (element) {
            is KtIfExpression -> setTextGetter(KotlinBundle.lazyMessage("remove.braces.from.if.all.statements"))
            is KtWhenExpression -> setTextGetter(KotlinBundle.lazyMessage("remove.braces.from.when.all.entries"))
        }
        return true
    }

    override fun applyTo(element: KtExpression, editor: Editor?) {
        element.targetBranchExpressions().forEach {
            RemoveBracesIntention.removeBraces(element, it)
        }
    }

    private fun KtExpression.targetBranchExpressions(): List<KtBlockExpression> {
        return allBranchExpressions().filterIsInstance<KtBlockExpression>()
    }
}