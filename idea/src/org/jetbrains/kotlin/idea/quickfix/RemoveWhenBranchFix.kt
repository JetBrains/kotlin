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
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class RemoveWhenBranchFix(element: KtWhenEntry) : KotlinQuickFixAction<KtWhenEntry>(element) {
    override fun getFamilyName() = if (element?.isElse == true) {
        KotlinBundle.message("remove.else.branch")
    } else {
        KotlinBundle.message("remove.branch")
    }

    override fun getText() = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.delete()
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): RemoveWhenBranchFix? {
            return when (diagnostic.factory) {
                Errors.REDUNDANT_ELSE_IN_WHEN ->
                    (diagnostic.psiElement as? KtWhenEntry)?.let { RemoveWhenBranchFix(it) }
                Errors.SENSELESS_NULL_IN_WHEN ->
                    diagnostic.psiElement.getStrictParentOfType<KtWhenEntry>()?.let { RemoveWhenBranchFix(it) }
                else ->
                    null
            }
        }
    }
}
