/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.*

class RemoveUnnecessaryParenthesesIntention : SelfTargetingRangeIntention<KtParenthesizedExpression>(
    KtParenthesizedExpression::class.java, "Remove unnecessary parentheses"
) {
    override fun applicabilityRange(element: KtParenthesizedExpression): TextRange? {
        element.expression ?: return null
        if (!KtPsiUtil.areParenthesesUseless(element)) return null
        return element.textRange
    }

    override fun applyTo(element: KtParenthesizedExpression, editor: Editor?) {
        val commentSaver = CommentSaver(element)
        val innerExpression = element.expression ?: return
        val replaced = element.replace(innerExpression)
        if (innerExpression.firstChild is KtLambdaExpression) {
            KtPsiFactory(element).appendSemicolonBeforeLambdaContainingElement(replaced)
        }
        commentSaver.restore(replaced)
    }
}
