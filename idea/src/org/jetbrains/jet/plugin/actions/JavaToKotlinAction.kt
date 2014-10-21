/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jet.j2k.ConverterSettings
import org.jetbrains.jet.j2k.FilesConversionScope
import org.jetbrains.jet.j2k.IdeaReferenceSearcher
import org.jetbrains.jet.j2k.JavaToKotlinConverter
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import java.util.ArrayList
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.codeStyle.CodeStyleManager
import java.io.IOException
import com.intellij.openapi.ui.ex.MessagesEx
import org.jetbrains.jet.plugin.j2k.J2kPostProcessor
import com.intellij.openapi.vfs.CharsetToolkit
import org.jetbrains.jet.lang.psi.JetFile

public class JavaToKotlinAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)!!
        val project = CommonDataKeys.PROJECT.getData(e.getDataContext())!!
        val selectedJavaFiles = allJavaFiles(virtualFiles, project)
        if (selectedJavaFiles.isEmpty()) return

        CommandProcessor.getInstance().executeCommand(project, object : Runnable {
            override fun run() {
                val newFiles = convertFiles(selectedJavaFiles, project)
                deleteFiles(selectedJavaFiles)
                reformatFiles(newFiles, project)

                if (newFiles.size() == 1) {
                    FileEditorManager.getInstance(project).openFile(newFiles.single(), true)
                }
            }
        }, "Convert files from Java to Kotlin", null)
    }

    override fun update(e: AnActionEvent) {
        val enabled = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.isNotEmpty() ?: false
        e.getPresentation().setEnabled(enabled)
    }

    private fun allJavaFiles(filesOrDirs: Array<VirtualFile>, project: Project): List<PsiJavaFile> {
        val allFiles = allFiles(filesOrDirs)
        val manager = PsiManager.getInstance(project)
        val result = ArrayList<PsiJavaFile>()
        for (file in allFiles) {
            val psiFile = manager.findFile(file)
            if (psiFile is PsiJavaFile) {
                result.add(psiFile)
            }
        }
        return result
    }

    private fun allFiles(filesOrDirs: Array<VirtualFile>): Collection<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        filesOrDirs.forEach { result.addFilesRecursive(it) }
        return result
    }

    private fun MutableCollection<VirtualFile>.addFilesRecursive(root: VirtualFile) {
        if (root.isDirectory()) {
            root.getChildren()!!.forEach {
                addFilesRecursive(it)
            }
        }
        else {
            add(root)
        }
    }

    private fun reformatFiles(kotlinFiles: List<VirtualFile>, project: Project) {
        for (file in kotlinFiles) {
            ApplicationManager.getApplication().runWriteAction {
                val jetFile = PsiManager.getInstance(project).findFile(file) as? JetFile
                if (jetFile != null) {
                    CodeStyleManager.getInstance(project).reformat(jetFile)
                }
            }
        }
    }

    private fun convertFiles(javaFiles: List<PsiJavaFile>, project: Project): List<VirtualFile> {
        val converter = JavaToKotlinConverter(project, ConverterSettings.defaultSettings, FilesConversionScope(javaFiles), IdeaReferenceSearcher)

        val convertedTexts = converter.elementsToKotlin(javaFiles.map { it to J2kPostProcessor(it) })

        val result = ArrayList<VirtualFile>()
        for ((i, psiFile) in javaFiles.withIndices()) {
            ApplicationManager.getApplication().runWriteAction {
                val file = saveConversionResult(psiFile.getVirtualFile(), convertedTexts[i], project)
                if (file != null) {
                    result.add(file)
                }
            }
        }
        return result
    }

    private fun saveConversionResult(javaFile: VirtualFile, text: String, project: Project): VirtualFile? {
        try {
            val kotlinFile = javaFile.copy(this, javaFile.getParent(), javaFile.getNameWithoutExtension() + ".kt")
            kotlinFile.setBinaryContent(CharsetToolkit.getUtf8Bytes(text))
            return kotlinFile
        }
        catch (e: IOException) {
            MessagesEx.error(project, e.getMessage()).showLater()
            return null
        }
    }

    private fun deleteFiles(javaFiles: List<PsiJavaFile>) {
        for (psiFile in javaFiles) {
            ApplicationManager.getApplication().runWriteAction {
                try {
                    psiFile.getVirtualFile()?.delete(this)
                }
                catch (e: IOException) {
                    MessagesEx.error(psiFile.getProject(), e.getMessage()).showLater()
                }
            }
        }
    }
}
