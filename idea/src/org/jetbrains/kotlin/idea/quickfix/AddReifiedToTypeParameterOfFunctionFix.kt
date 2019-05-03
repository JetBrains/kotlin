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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class AddReifiedToTypeParameterOfFunctionFix(
        typeParameter: KtTypeParameter,
        val function: KtNamedFunction
) : AddModifierFix(typeParameter, KtTokens.REIFIED_KEYWORD) {

    private val inlineFix = AddInlineToFunctionWithReifiedFix(function)

    override fun getText() = element?.let { "Make ${getElementName(it)} reified and ${getElementName(function)} inline" } ?: ""

    override fun invokeImpl(project: Project, editor: Editor?, file: PsiFile) {
        super.invokeImpl(project, editor, file)
        inlineFix.invoke(project, editor, file)
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = Errors.TYPE_PARAMETER_AS_REIFIED.cast(diagnostic)
            val function = element.psiElement.getStrictParentOfType<KtNamedFunction>()
            val parameter = function?.typeParameterList?.parameters?.getOrNull(element.a.index) ?: return null
            return AddReifiedToTypeParameterOfFunctionFix(parameter, function)
        }
    }
}