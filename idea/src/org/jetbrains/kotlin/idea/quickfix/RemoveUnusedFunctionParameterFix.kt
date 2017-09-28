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
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*

class RemoveUnusedFunctionParameterFix(parameter: KtParameter) : KotlinQuickFixAction<KtParameter>(parameter) {
    override fun getFamilyName() = ChangeFunctionSignatureFix.FAMILY_NAME

    override fun getText() = element?.let { "Remove parameter '${it.name}'" } ?: ""

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val primaryConstructorParameterList = (element.parent as? KtParameterList)?.takeIf { it.parent is KtPrimaryConstructor }
        if (primaryConstructorParameterList != null && primaryConstructorParameterList.parameters.size == 1) {
            runWriteAction {
                primaryConstructorParameterList.delete()
            }
        }
        else {
            val parameterDescriptor = element.unsafeResolveToDescriptor() as ValueParameterDescriptor
            ChangeFunctionSignatureFix.runRemoveParameter(parameterDescriptor, element)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtParameter>? {
            val parameter = Errors.UNUSED_PARAMETER.cast(diagnostic).psiElement
            val parameterOwner = parameter.parent.parent
            if (parameterOwner is KtFunctionLiteral ||
                    (parameterOwner is KtNamedFunction && parameterOwner.name == null)) return null
            return RemoveUnusedFunctionParameterFix(parameter)
        }
    }
}
