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

package org.jetbrains.kotlin.idea.actions

import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.j2k.IdeaResolverForConverter
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.IdeaReferenceSearcher
import org.jetbrains.kotlin.j2k.JavaToKotlinConverter
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.IOException
import java.util.ArrayList

public class JavaToKotlinAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val javaFiles = selectedJavaFiles(e).toList()
        val project = CommonDataKeys.PROJECT.getData(e.getDataContext())!!

        var convertedTexts: List<String>? = null
        fun convert() {
            val converter = JavaToKotlinConverter(project, ConverterSettings.defaultSettings,
                                                  IdeaReferenceSearcher, IdeaResolverForConverter, J2kPostProcessor(formatCode = true))
            val inputElements = javaFiles.map { JavaToKotlinConverter.InputElement(it, it) }
            convertedTexts = converter.elementsToKotlin(inputElements, ProgressManager.getInstance().getProgressIndicator())
        }

        if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    ApplicationManager.getApplication().runReadAction(::convert)
                },
                "Converting Java to Kotlin",
                true,
                project)) return


        CommandProcessor.getInstance().executeCommand(project, object : Runnable {
            override fun run() {
                CommandProcessor.getInstance().markCurrentCommandAsGlobal(project)

                val newFiles = saveResults(javaFiles, convertedTexts!!, project)
                deleteFiles(javaFiles)

                newFiles.singleOrNull()?.let {
                    FileEditorManager.getInstance(project).openFile(it, true)
                }
            }
        }, "Convert files from Java to Kotlin", null)
    }

    override fun update(e: AnActionEvent) {
        val enabled = selectedJavaFiles(e).any()
        e.getPresentation().setEnabled(enabled)
    }

    private fun selectedJavaFiles(e: AnActionEvent): Sequence<PsiJavaFile> {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return sequenceOf()
        val project = CommonDataKeys.PROJECT.getData(e.getDataContext()) ?: return sequenceOf()
        return allJavaFiles(virtualFiles, project)
    }

    private fun allJavaFiles(filesOrDirs: Array<VirtualFile>, project: Project): Sequence<PsiJavaFile> {
        val manager = PsiManager.getInstance(project)
        return allFiles(filesOrDirs)
                .sequence()
                .map { manager.findFile(it) as? PsiJavaFile }
                .filterNotNull()
    }

    private fun allFiles(filesOrDirs: Array<VirtualFile>): Collection<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        for (file in filesOrDirs) {
            VfsUtilCore.visitChildrenRecursively(file, object : VirtualFileVisitor<Unit>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    result.add(file)
                    return true
                }
            })
        }
        return result
    }

    private fun saveResults(javaFiles: List<PsiJavaFile>, convertedTexts: List<String>, project: Project): List<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        for ((i, psiFile) in javaFiles.withIndex()) {
            ApplicationManager.getApplication().runWriteAction {
                result.addIfNotNull(saveConversionResult(psiFile.getVirtualFile(), convertedTexts[i], project))
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
