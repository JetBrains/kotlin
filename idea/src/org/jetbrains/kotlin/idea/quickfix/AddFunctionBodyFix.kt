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
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class AddFunctionBodyFix(element: KtFunction) : KotlinQuickFixAction<KtFunction>(element) {
    override fun getFamilyName() = "Add function body"
    override fun getText() = familyName

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false
        return super.isAvailable(project, editor, file) && !element.hasBody()
    }

    public override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        if (!element.hasBody()) {
            element.add(KtPsiFactory(project).createEmptyBody())
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): AddFunctionBodyFix? {
            return diagnostic.psiElement.getNonStrictParentOfType<KtFunction>()?.let(::AddFunctionBodyFix)
        }
    }
}
