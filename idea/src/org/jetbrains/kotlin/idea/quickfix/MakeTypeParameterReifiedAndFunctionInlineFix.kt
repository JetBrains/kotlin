/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class MakeTypeParameterReifiedAndFunctionInlineFix(
        typeReference: KtTypeReference,
        function: KtNamedFunction,
        private val typeParameter: KtTypeParameter
) : KotlinQuickFixAction<KtTypeReference>(typeReference) {

    private val inlineFix = AddInlineToFunctionWithReifiedFix(function)

    override fun getText() = "Make type parameter reified and function inline"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        inlineFix.invoke(project, editor, file)
        AddModifierFix(typeParameter, KtTokens.REIFIED_KEYWORD).invoke(project, editor, file)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtTypeReference>? {
            val element = Errors.CANNOT_CHECK_FOR_ERASED.cast(diagnostic)
            val typeReference = element.psiElement as? KtTypeReference ?: return null
            val function = typeReference.getStrictParentOfType<KtNamedFunction>() ?: return null
            val typeParameter = function.typeParameterList?.parameters?.firstOrNull {
                it.descriptor == element.a.constructor.declarationDescriptor
            } ?: return null
            return MakeTypeParameterReifiedAndFunctionInlineFix(typeReference, function, typeParameter)
        }
    }

}
