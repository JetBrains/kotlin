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

class ReplaceSubstringWithTakeIntention : ReplaceSubstringIntention("Replace 'substring' call with 'take' call") {
    override fun applicabilityRangeInner(element: KtDotQualifiedExpression): TextRange? {
        val arguments = element.callExpression?.valueArguments ?: return null
        if (arguments.size == 2 && element.isFirstArgumentZero()) {
            return getTextRange(element)
        }
        return null
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        val argument = element.callExpression!!.valueArguments[1].getArgumentExpression()!!
        element.replaceWith("$0.take($1)", argument)
    }
}
