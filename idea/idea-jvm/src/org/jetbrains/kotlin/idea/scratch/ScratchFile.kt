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

package org.jetbrains.kotlin.idea.scratch

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.scratch.ui.scratchFileOptions

abstract class ScratchFile(val project: Project, val editor: TextEditor) {
    fun getExpressions(): List<ScratchExpression> = runReadAction {
        getPsiFile()?.let { getExpressions(it) } ?: emptyList()
    }

    fun getPsiFile(): PsiFile? = runReadAction {
        PsiDocumentManager.getInstance(project).getPsiFile(editor.editor.document)
    }

    fun getModule(): Module? {
        return editor.getScratchPanel()?.getModule()
    }

    val options: ScratchFileOptions
        get() = getPsiFile()?.virtualFile?.scratchFileOptions ?: ScratchFileOptions()

    fun saveOptions(update: ScratchFileOptions.() -> ScratchFileOptions) {
        val virtualFile = getPsiFile()?.virtualFile ?: return
        with(virtualFile) {
            val configToUpdate = scratchFileOptions ?: ScratchFileOptions()
            scratchFileOptions = configToUpdate.update()
        }
    }

    abstract fun getExpressions(psiFile: PsiFile): List<ScratchExpression>
    abstract fun hasErrors(): Boolean
}

data class ScratchExpression(val element: PsiElement, val lineStart: Int, val lineEnd: Int = lineStart)

data class ScratchFileOptions(
    val isRepl: Boolean = false,
    val isMakeBeforeRun: Boolean = false,
    val isInteractiveMode: Boolean = true
)