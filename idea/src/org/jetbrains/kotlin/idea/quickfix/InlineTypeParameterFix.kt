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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext

class InlineTypeParameterFix(val typeReference: KtTypeReference) : KotlinQuickFixAction<KtTypeReference>(typeReference) {
    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val parameter = typeReference.parent as? KtTypeParameter ?: return
        val bound = parameter.extendsBound ?: return
        val parameterList = parameter.parent as? KtTypeParameterList ?: return
        val parameterListOwner = typeReference.getStrictParentOfType<KtTypeParameterListOwner>() ?: return
        val context = parameterListOwner.analyzeFully()
        val parameterDescriptor = context[BindingContext.TYPE_PARAMETER, parameter] ?: return
        parameterListOwner.forEachDescendantOfType<KtTypeReference>() {
            val typeElement = it.typeElement
            val type = context[BindingContext.TYPE, it]
            if (typeElement != null && type != null && type.constructor.declarationDescriptor == parameterDescriptor) {
                typeElement.replace(bound)
            }
        }

        if (parameterList.parameters.size == 1) {
            parameterList.delete()
        }
        else {
            EditCommaSeparatedListHelper.removeItem(parameter)
        }
    }

    override fun getText() = "Inline type parameter"

    override fun getFamilyName() = text

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) = InlineTypeParameterFix(Errors.FINAL_UPPER_BOUND.cast(diagnostic).psiElement)
    }
}