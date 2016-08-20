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
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression

class ReplaceSubstringWithSubstringAfterIntention : ReplaceSubstringIntention("Replace 'substring' call with 'substringAfter' call") {
    override fun applicabilityRangeInner(element: KtDotQualifiedExpression): TextRange? {
        val arguments = element.callExpression?.valueArguments ?: return null

        if (arguments.size == 1 && isIndexOfCall(arguments[0].getArgumentExpression(), element.receiverExpression)) {
            return getTextRange(element)
        }

        return null
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        element.replaceWith(
                "$0.substringAfter($1)",
                (element.getArgumentExpression(0) as KtDotQualifiedExpression).getArgumentExpression(0))
    }
}

class ReplaceSubstringWithSubstringBeforeIntention : ReplaceSubstringIntention("Replace 'substring' call with 'substringBefore' call") {
    override fun applicabilityRangeInner(element: KtDotQualifiedExpression): TextRange? {
        val arguments = element.callExpression?.valueArguments ?: return null

        if (arguments.size == 2
            && element.isFirstArgumentZero()
            && isIndexOfCall(arguments[1].getArgumentExpression(), element.receiverExpression)) {
            return getTextRange(element)
        }

        return null
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        element.replaceWith(
                "$0.substringBefore($1)",
                (element.getArgumentExpression(1) as KtDotQualifiedExpression).getArgumentExpression(0))
    }
}

private fun KtDotQualifiedExpression.getArgumentExpression(index: Int): KtExpression {
    return callExpression!!.valueArguments[index].getArgumentExpression()!!
}
