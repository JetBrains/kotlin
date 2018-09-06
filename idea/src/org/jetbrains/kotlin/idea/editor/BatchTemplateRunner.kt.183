/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.editor

import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import java.util.*

class BatchTemplateRunner(private val project: Project) {
    private val elementsAndFactories = ArrayList<Pair<SmartPsiElementPointer<*>, () -> Template?>>()

    fun addTemplateFactory(element: PsiElement, factory: () -> Template?) {
        elementsAndFactories.add(element.createSmartPointer() to factory)
    }

    fun runTemplates() {
        runTemplates(elementsAndFactories.iterator())
    }

    private fun getEditor(pointer: SmartPsiElementPointer<*>): Editor? {
        val element = pointer.element ?: return null
        val virtualFile = element.containingFile?.virtualFile ?: return null
        val descriptor = OpenFileDescriptor(project, virtualFile, element.textRange.startOffset)
        return FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
    }

    private fun runTemplates(iterator: Iterator<Pair<SmartPsiElementPointer<*>, () -> Template?>>) {
        if (!iterator.hasNext()) return

        val manager = TemplateManager.getInstance(project)
        project.executeWriteCommand("") {
            val (pointer, factory) = iterator.next()

            val editor = getEditor(pointer) ?: return@executeWriteCommand
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
            val template = factory() ?: return@executeWriteCommand
            manager.startTemplate(
                    editor,
                    template,
                    object : TemplateEditingAdapter() {
                        override fun templateFinished(template: Template, brokenOff: Boolean) {
                            if (brokenOff) return
                            ApplicationManager.getApplication().invokeLater { runTemplates(iterator) }
                        }
                    }
            )
        }
    }
}