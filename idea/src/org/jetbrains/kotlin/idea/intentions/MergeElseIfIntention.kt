/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.*


class MergeElseIfIntention : SelfTargetingIntention<KtIfExpression>(KtIfExpression::class.java, "Merge 'else if'") {
    override fun isApplicableTo(element: KtIfExpression, caretOffset: Int): Boolean {
        val elseBody = element.`else` ?: return false
        val nestedIf = elseBody.nestedIf() ?: return false
        if (nestedIf.`else` != null) {
            return false
        }
        return true
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        applyTo(element)
    }

    companion object {
        fun applyTo(element: KtIfExpression): Int {
            val nestedIf = element.`else`?.nestedIf() ?: return -1
            val condition = nestedIf.condition ?: return -1
            val nestedBody = nestedIf.then ?: return -1

            val factory = KtPsiFactory(element)
            val newBody = element.`else`?.replace(
                factory.createExpressionByPattern("if ($0) $1", condition, nestedBody)
            ) ?: return -1
            return newBody.textRange!!.startOffset
        }

        private fun KtExpression.nestedIf() = when (this) {
            is KtBlockExpression -> this.statements.singleOrNull() as? KtIfExpression
            is KtIfExpression -> this
            else -> null
        }
    }
}