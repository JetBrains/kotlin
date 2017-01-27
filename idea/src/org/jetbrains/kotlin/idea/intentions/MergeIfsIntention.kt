/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.*

class MergeIfsIntention : SelfTargetingIntention<KtIfExpression>(KtIfExpression::class.java, "Merge 'if's") {

    override fun isApplicableTo(element: KtIfExpression, caretOffset: Int): Boolean {
        if (element.`else` != null) return false
        val then = element.then ?: return false

        val nestedIf = then.nestedIf() ?: return false
        if (nestedIf.`else` != null) return false

        return true
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        applyTo(element)
    }

    companion object {
        fun applyTo(element: KtIfExpression): Int {
            val nestedIf = element.then?.nestedIf() ?: return -1
            val condition = element.condition ?: return -1
            val secondCondition = nestedIf.condition ?: return -1
            val nestedBody = nestedIf.then ?: return -1

            val factory = KtPsiFactory(element)

            condition.replace(factory.createExpressionByPattern("$0 && $1", condition, secondCondition))
            val newBody = element.then!!.replace(nestedBody)

            return newBody.textRange!!.startOffset
        }

        private fun KtExpression.nestedIf() = when (this) {
            is KtBlockExpression -> this.statements.singleOrNull() as? KtIfExpression
            is KtIfExpression -> this
            else -> null
        }
    }
}