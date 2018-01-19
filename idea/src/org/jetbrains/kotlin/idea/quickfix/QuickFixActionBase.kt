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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.CREATEBYPATTERN_MAY_NOT_REFORMAT
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

abstract class QuickFixActionBase<out T : PsiElement>(element: T) : IntentionAction {
    private val elementPointer = element.createSmartPointer()

    protected val element: T?
        get() = elementPointer.element

    open val isCrossLanguageFix: Boolean = false

    protected open fun isAvailableImpl(project: Project, editor: Editor?, file: PsiFile) = true

    final override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            CREATEBYPATTERN_MAY_NOT_REFORMAT = true
        }
        try {
            val element = element ?: return false
            return element.isValid &&
                   !element.project.isDisposed &&
                   (file.manager.isInProject(file) || file is KtCodeFragment) &&
                   (file is KtFile || isCrossLanguageFix) &&
                   isAvailableImpl(project, editor, file)
        }
        finally {
            CREATEBYPATTERN_MAY_NOT_REFORMAT = false
        }
    }

    override fun startInWriteAction() = true
}