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
import org.jetbrains.kotlin.psi.KtFunctionLiteralExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.endOffset

public class RemoveExplicitLambdaParameterTypesIntention : JetSelfTargetingIntention<KtFunctionLiteralExpression>(javaClass(), "Remove explicit lambda parameter types (may break code)") {
    override fun isApplicableTo(element: KtFunctionLiteralExpression, caretOffset: Int): Boolean {
        if (element.getValueParameters().none { it.getTypeReference() != null }) return false
        val arrow = element.getFunctionLiteral().getArrow() ?: return false
        return caretOffset <= arrow.endOffset
    }

    override fun applyTo(element: KtFunctionLiteralExpression, editor: Editor) {
        val oldParameterList = element.getFunctionLiteral().getValueParameterList()!!

        val parameterString = oldParameterList.getParameters().map { it.getName() }.joinToString(", ")
        val newParameterList = KtPsiFactory(element).createFunctionLiteralParameterList(parameterString)
        oldParameterList.replace(newParameterList)
    }
}
