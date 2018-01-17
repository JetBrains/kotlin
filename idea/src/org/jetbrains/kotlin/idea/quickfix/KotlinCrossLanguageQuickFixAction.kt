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

import com.intellij.codeInsight.FileModificationService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

abstract class KotlinCrossLanguageQuickFixAction<out T : PsiElement>(element: T) : QuickFixActionBase<T>(element) {
    override val isCrossLanguageFix: Boolean
        get() = true

    override final fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        val element = element
        if (element != null && FileModificationService.getInstance().prepareFileForWrite(element.containingFile)) {
            invokeImpl(project, editor, file)
        }
    }

    protected abstract fun invokeImpl(project: Project, editor: Editor?, file: PsiFile)
}
