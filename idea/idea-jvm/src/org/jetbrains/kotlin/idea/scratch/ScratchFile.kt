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
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.messages.Topic
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.scratch.ui.scratchFileOptions

abstract class ScratchFile(val project: Project, val file: VirtualFile) {
    var replScratchExecutor: SequentialScratchExecutor? = null
    var compilingScratchExecutor: ScratchExecutor? = null

    private val moduleListeners: MutableList<() -> Unit> = mutableListOf()
    var module: Module? = null
        private set

    fun getExpressions(): List<ScratchExpression> = runReadAction {
        getPsiFile()?.let { getExpressions(it) } ?: emptyList()
    }

    fun getPsiFile(): PsiFile? = runReadAction {
        file.toPsiFile(project)
    }

    fun setModule(value: Module?) {
        module = value
        moduleListeners.forEach { it() }
    }

    fun addModuleListener(f: (PsiFile, Module?) -> Unit) {
        moduleListeners.add {
            val selectedModule = module

            val psiFile = getPsiFile()
            if (psiFile != null) {
                f(psiFile, selectedModule)
            }
        }
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

    fun getExpressionAtLine(line: Int): ScratchExpression? {
        return getExpressions().find { line in it.lineStart..it.lineEnd }
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

interface ScratchFileListener {
    fun fileCreated(file: ScratchFile)

    companion object {
        val TOPIC = Topic.create(
            "ScratchFileListener",
            ScratchFileListener::class.java
        )
    }
}