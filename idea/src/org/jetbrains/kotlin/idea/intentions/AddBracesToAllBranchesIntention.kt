/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.util.containers.addIfNotNull
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.*

class AddBracesToAllBranchesIntention : SelfTargetingIntention<KtExpression>(
    KtExpression::class.java,
    KotlinBundle.lazyMessage("add.braces.to.all.branches")
) {
    override fun isApplicableTo(element: KtExpression, caretOffset: Int): Boolean {
        if (!isCaretOnIfOrWhenKeyword(element, caretOffset)) return false
        if (element.targetBranchExpressions().isEmpty()) return false
        when (element) {
            is KtIfExpression -> setTextGetter(KotlinBundle.lazyMessage("add.braces.to.if.all.statements"))
            is KtWhenExpression -> setTextGetter(KotlinBundle.lazyMessage("add.braces.to.when.all.entries"))
        }
        return true
    }

    override fun applyTo(element: KtExpression, editor: Editor?) {
        element.targetBranchExpressions().forEach {
            AddBracesIntention.addBraces(element, it)
        }
    }

    private fun KtExpression.targetBranchExpressions(): List<KtExpression> {
        return allBranchExpressions().filter { it !is KtBlockExpression }
    }

    companion object {
        fun isCaretOnIfOrWhenKeyword(element: KtExpression, caretOffset: Int): Boolean = when (element) {
            is KtIfExpression ->
                element.ifKeyword.textRange.containsOffset(caretOffset) && element.parent !is KtContainerNodeForControlStructureBody
            is KtWhenExpression ->
                element.whenKeyword.textRange.containsOffset(caretOffset)
            else ->
                false
        }

        fun KtExpression.allBranchExpressions(): List<KtExpression> = when (this) {
            is KtIfExpression -> {
                val branchExpressions = mutableListOf<KtExpression>()
                fun collect(ifExpression: KtIfExpression) {
                    branchExpressions.addIfNotNull(ifExpression.then)
                    when (val elseExpression = ifExpression.`else`) {
                        is KtIfExpression -> collect(elseExpression)
                        else -> branchExpressions.addIfNotNull(elseExpression)
                    }
                }
                collect(this)
                branchExpressions
            }
            is KtWhenExpression -> entries.mapNotNull { it.expression }
            else -> emptyList()
        }
    }
}
