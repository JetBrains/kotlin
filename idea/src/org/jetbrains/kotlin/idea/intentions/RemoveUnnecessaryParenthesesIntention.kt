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
import org.jetbrains.kotlin.psi.JetParenthesizedExpression
import org.jetbrains.kotlin.psi.JetPsiUtil

public class RemoveUnnecessaryParenthesesIntention : JetSelfTargetingIntention<JetParenthesizedExpression>(
        "remove.unnecessary.parentheses", javaClass()
) {
    override fun isApplicableTo(element: JetParenthesizedExpression): Boolean = JetPsiUtil.areParenthesesUseless(element)

    override fun applyTo(element: JetParenthesizedExpression, editor: Editor) {
        with (element.getExpression()) {
            assert (this != null, "parenthesizedExpression.getExpression() == null despite @IfNotParsed annotation")
            element.replace(this!!)
        }
    }
}
