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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.refactoring.rename.RenameProcessor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class RenameParameterToMatchOverriddenMethodFix(
        private val parameter: KtParameter,
        private val newName: String
) : KotlinQuickFixAction<KtParameter>(parameter) {
    override fun getFamilyName() = "Rename"

    override fun getText() = "Rename parameter to match overridden method"

    override fun startInWriteAction(): Boolean = false

    public override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        RenameProcessor(project, parameter, newName, false, false).run()
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val parameter = diagnostic.psiElement.getNonStrictParentOfType<KtParameter>() ?: return null
            val parameterDescriptor = parameter.resolveToDescriptor() as ValueParameterDescriptor
            val parameterFromSuperclassName = parameterDescriptor
                    .overriddenDescriptors
                    .map { it.name.asString() }
                    .distinct()
                    .singleOrNull() ?: return null
            return RenameParameterToMatchOverriddenMethodFix(parameter, parameterFromSuperclassName)
        }
    }
}
