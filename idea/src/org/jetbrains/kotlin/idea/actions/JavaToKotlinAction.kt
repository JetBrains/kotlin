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

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.j2k.IdeaResolverForConverter
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.IdeaReferenceSearcher
import org.jetbrains.kotlin.j2k.JavaToKotlinConverter
import java.io.File
import java.io.IOException
import java.util.ArrayList

public class JavaToKotlinAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val javaFiles = selectedJavaFiles(e).toList()
        val project = CommonDataKeys.PROJECT.getData(e.getDataContext())!!

        var converterResult: JavaToKotlinConverter.FilesResult? = null
        fun convert() {
            val converter = JavaToKotlinConverter(project, ConverterSettings.defaultSettings, IdeaReferenceSearcher, IdeaResolverForConverter)
            converterResult = converter.filesToKotlin(javaFiles, J2kPostProcessor(formatCode = true), ProgressManager.getInstance().getProgressIndicator())
        }

        val title = "Convert Java to Kotlin"
        if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    runReadAction(::convert)
                },
                title,
                true,
                project)) return


        var externalCodeUpdate: (() -> Unit)? = null

        if (converterResult!!.externalCodeProcessing != null) {
            val question = "Some code in the rest of your project may require corrections after performing this conversion. Do you want to find such code and correct it too?"
            if (Messages.showOkCancelDialog(project, question, title, Messages.getQuestionIcon()) == Messages.OK) {
                ProgressManager.getInstance().runProcessWithProgressSynchronously(
                        {
                            runReadAction {
                                externalCodeUpdate = converterResult!!.externalCodeProcessing.prepareWriteOperation(ProgressManager.getInstance().getProgressIndicator())
                            }
                        },
                        title,
                        true,
                        project)
            }
        }

        project.executeWriteCommand("Convert files from Java to Kotlin") {
            CommandProcessor.getInstance().markCurrentCommandAsGlobal(project)

            val newFiles = saveResults(javaFiles, converterResult!!.results)

            externalCodeUpdate?.invoke()

            newFiles.singleOrNull()?.let {
                FileEditorManager.getInstance(project).openFile(it, true)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val enabled = selectedJavaFiles(e).any()
        e.getPresentation().setEnabled(enabled)
    }

    private fun selectedJavaFiles(e: AnActionEvent): Sequence<PsiJavaFile> {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return sequenceOf()
        val project = e.getProject() ?: return sequenceOf()
        return allJavaFiles(virtualFiles, project)
    }

    private fun allJavaFiles(filesOrDirs: Array<VirtualFile>, project: Project): Sequence<PsiJavaFile> {
        val manager = PsiManager.getInstance(project)
        return allFiles(filesOrDirs)
                .asSequence()
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

    private fun saveResults(javaFiles: List<PsiJavaFile>, convertedTexts: List<String>): List<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        for ((psiFile, text) in javaFiles.zip(convertedTexts)) {
            val virtualFile = psiFile.getVirtualFile()
            val fileName = uniqueKotlinFileName(virtualFile)
            try {
                virtualFile.rename(this, fileName)
                virtualFile.setBinaryContent(CharsetToolkit.getUtf8Bytes(text))
                result.add(virtualFile)
            }
            catch (e: IOException) {
                MessagesEx.error(psiFile.getProject(), e.getMessage()).showLater()
            }
        }
        return result
    }

    private fun uniqueKotlinFileName(javaFile: VirtualFile): String {
        val ioFile = File(javaFile.getPath().replace('/', File.separatorChar))

        var i = 0
        while (true) {
            val fileName = javaFile.getNameWithoutExtension() + (if (i > 0) i else "") + ".kt"
            if (!ioFile.resolveSibling(fileName).exists()) return fileName
            i++
        }
    }
}
