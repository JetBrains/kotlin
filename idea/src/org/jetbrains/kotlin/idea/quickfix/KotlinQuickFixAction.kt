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

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile

abstract class KotlinQuickFixAction<out T : PsiElement>(protected val element: T) : IntentionAction {

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        return element.isValid &&
               !element.project.isDisposed &&
               (file.manager.isInProject(file) || file is KtCodeFragment) &&
               (file is KtFile)
    }

    override final fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        if (file is KtFile && FileModificationService.getInstance().prepareFileForWrite(element.containingFile)) {
            invoke(project, editor, file)
        }
    }

    protected abstract fun invoke(project: Project, editor: Editor?, file: KtFile)

    override fun startInWriteAction() = true
}
