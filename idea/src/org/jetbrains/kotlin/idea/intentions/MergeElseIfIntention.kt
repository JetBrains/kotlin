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
        return nestedIf.`else` == null
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        val nestedIf = element.`else`?.nestedIf() ?: return
        val condition = nestedIf.condition ?: return
        val nestedBody = nestedIf.then ?: return

        val factory = KtPsiFactory(element)
        element.`else`?.replace(
            factory.createExpressionByPattern("if ($0) $1", condition, nestedBody)
        )
    }

    companion object {
        private fun KtExpression.nestedIf() =
            if (this is KtBlockExpression) {
                this.statements.singleOrNull() as? KtIfExpression
            } else {
                null
            }
    }
}