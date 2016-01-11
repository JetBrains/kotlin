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

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.moveInsideParentheses
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.psiUtil.containsInside

class MoveLambdaInsideParenthesesIntention : SelfTargetingIntention<KtLambdaArgument>(KtLambdaArgument::class.java, "Move lambda argument into parentheses"), LowPriorityAction {
    override fun isApplicableTo(element: KtLambdaArgument, caretOffset: Int): Boolean {
        val body = element.getLambdaExpression().bodyExpression ?: return true
        return !body.textRange.containsInside(caretOffset)
    }

    override fun applyTo(element: KtLambdaArgument, editor: Editor?) {
        element.moveInsideParentheses(element.analyze())
    }
}

