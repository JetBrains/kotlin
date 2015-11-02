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
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter

class RemoveUnusedFunctionParameterFix(parameter: KtParameter) : KotlinQuickFixAction<KtParameter>(parameter) {
    override fun getFamilyName() = ChangeFunctionSignatureFix.FAMILY_NAME

    override fun getText() = "Remove parameter '${element.name}'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val parameterDescriptor = element.resolveToDescriptor() as ValueParameterDescriptor
        ChangeFunctionSignatureFix.runRemoveParameter(parameterDescriptor, element)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtParameter> {
            return RemoveUnusedFunctionParameterFix(Errors.UNUSED_PARAMETER.cast(diagnostic).psiElement)
        }
    }
}
