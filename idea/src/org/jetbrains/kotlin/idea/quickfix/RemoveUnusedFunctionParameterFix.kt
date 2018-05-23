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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.resolveToParameterDescriptorIfAny
import org.jetbrains.kotlin.idea.intentions.RemoveEmptyPrimaryConstructorIntention
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RemoveUnusedFunctionParameterFix(parameter: KtParameter) : KotlinQuickFixAction<KtParameter>(parameter) {
    override fun getFamilyName() = ChangeFunctionSignatureFix.FAMILY_NAME

    override fun getText() = element?.let { "Remove parameter '${it.name}'" } ?: ""

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val parameter = element ?: return
        val parameterList = parameter.parent as? KtParameterList ?: return
        val parameterDescriptor = parameter.resolveToParameterDescriptorIfAny(BodyResolveMode.FULL) ?: return
        val parameterSize = parameterList.parameters.size
        val typeParameters = typeParameters(parameter)
        val primaryConstructor = parameterList.parent as? KtPrimaryConstructor

        ChangeFunctionSignatureFix.runRemoveParameter(parameterDescriptor, parameter)
        runRemoveUnusedTypeParameters(typeParameters)

        if (parameterSize > 1) {
            val nextParameter = parameterList.parameters.getOrNull(parameterDescriptor.index)
            if (nextParameter != null) {
                editor?.caretModel?.moveToOffset(nextParameter.startOffset)
            }
        }

        if (primaryConstructor != null) {
            val removeConstructorIntention = RemoveEmptyPrimaryConstructorIntention()
            if (removeConstructorIntention.isApplicableTo(primaryConstructor)) {
                editor?.caretModel?.moveToOffset(primaryConstructor.endOffset)
                runWriteAction {
                    removeConstructorIntention.applyTo(primaryConstructor, editor = null)
                }
            }
        }
    }

    private fun typeParameters(parameter: KtParameter): List<KtTypeParameter> {
        val parameterParent = parameter.getParentOfTypesAndPredicate(false, KtNamedFunction::class.java, KtClass::class.java) { true }
        return parameter.typeReference?.typeElement
            ?.collectDescendantsOfType<KtNameReferenceExpression> { true }
            ?.mapNotNull {
                val typeParameter = it.reference?.resolve() as? KtTypeParameter ?: return@mapNotNull null
                val parent = typeParameter.getParentOfTypesAndPredicate(false, KtNamedFunction::class.java, KtClass::class.java) { true }
                if (parent == parameterParent) typeParameter else null
            } ?: emptyList()
    }

    private fun runRemoveUnusedTypeParameters(typeParameters: List<KtTypeParameter>) {
        val unusedTypeParams = typeParameters.filter { ReferencesSearch.search(it).findFirst() == null }
        if (unusedTypeParams.isEmpty()) return
        runWriteAction {
            unusedTypeParams.forEach { typeParameter ->
                val typeParameterList = typeParameter.parent as? KtTypeParameterList ?: return@forEach
                if (typeParameterList.parameters.size == 1)
                    typeParameterList.delete()
                else
                    EditCommaSeparatedListHelper.removeItem(typeParameter)
            }
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtParameter>? {
            val parameter = Errors.UNUSED_PARAMETER.cast(diagnostic).psiElement
            val parameterOwner = parameter.parent.parent
            if (parameterOwner is KtFunctionLiteral || (parameterOwner is KtNamedFunction && parameterOwner.name == null)) return null
            return RemoveUnusedFunctionParameterFix(parameter)
        }
    }
}
