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

import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.JavaToKotlinConverter
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.io.IOException
import java.util.*

class JavaToKotlinAction : AnAction() {
    companion object {
        private fun uniqueKotlinFileName(javaFile: VirtualFile): String {
            val ioFile = File(javaFile.path.replace('/', File.separatorChar))

            var i = 0
            while (true) {
                val fileName = javaFile.nameWithoutExtension + (if (i > 0) i else "") + ".kt"
                if (!ioFile.resolveSibling(fileName).exists()) return fileName
                i++
            }
        }

        val title = "Convert Java to Kotlin"

        private fun saveResults(javaFiles: List<PsiJavaFile>, convertedTexts: List<String>): List<VirtualFile> {
            val result = ArrayList<VirtualFile>()
            for ((psiFile, text) in javaFiles.zip(convertedTexts)) {
                try {
                    val document = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile)
                    if (document == null) {
                        MessagesEx.error(psiFile.project, "Failed to save conversion result: couldn't find document for " + psiFile.name).showLater()
                        continue
                    }
                    document.replaceString(0, document.textLength, text)
                    FileDocumentManager.getInstance().saveDocument(document)

                    val virtualFile = psiFile.virtualFile
                    if (ScratchRootType.getInstance().containsFile(virtualFile)) {
                        val mapping = ScratchFileService.getInstance().scratchesMapping
                        mapping.setMapping(virtualFile, KotlinFileType.INSTANCE.language)
                    }
                    else {
                        val fileName = uniqueKotlinFileName(virtualFile)
                        virtualFile.rename(this, fileName)
                    }
                }
                catch (e: IOException) {
                    MessagesEx.error(psiFile.project, e.message ?: "").showLater()
                }
            }
            return result
        }

        fun convertFiles(javaFiles: List<PsiJavaFile>, project: Project,
                         enableExternalCodeProcessing: Boolean = true,
                         askExternalCodeProcessing: Boolean = true): List<KtFile> {
            var converterResult: JavaToKotlinConverter.FilesResult? = null
            fun convert() {
                val converter = JavaToKotlinConverter(project, ConverterSettings.defaultSettings, IdeaJavaToKotlinServices)
                converterResult = converter.filesToKotlin(javaFiles, J2kPostProcessor(formatCode = true), ProgressManager.getInstance().progressIndicator)
            }


            if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    ::convert,
                    title,
                    true,
                    project)) return emptyList()


            var externalCodeUpdate: (() -> Unit)? = null

            if (enableExternalCodeProcessing && converterResult!!.externalCodeProcessing != null) {
                val question = "Some code in the rest of your project may require corrections after performing this conversion. Do you want to find such code and correct it too?"
                if (!askExternalCodeProcessing || (Messages.showOkCancelDialog(project, question, title, Messages.getQuestionIcon()) == Messages.OK)) {
                    ProgressManager.getInstance().runProcessWithProgressSynchronously(
                            {
                                runReadAction {
                                    externalCodeUpdate = converterResult!!.externalCodeProcessing!!.prepareWriteOperation(ProgressManager.getInstance().progressIndicator)
                                }
                            },
                            title,
                            true,
                            project)
                }
            }

            return project.executeWriteCommand("Convert files from Java to Kotlin", null) {
                CommandProcessor.getInstance().markCurrentCommandAsGlobal(project)

                val newFiles = saveResults(javaFiles, converterResult!!.results)

                externalCodeUpdate?.invoke()

                PsiDocumentManager.getInstance(project).commitAllDocuments()

                newFiles.singleOrNull()?.let {
                    FileEditorManager.getInstance(project).openFile(it, true)
                }

                newFiles.map { it.toPsiFile(project) as KtFile }
            }
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val javaFiles = selectedJavaFiles(e).filter { it.isWritable }.toList()
        val project = CommonDataKeys.PROJECT.getData(e.dataContext)!!

        val firstSyntaxError = javaFiles.asSequence().map { PsiTreeUtil.findChildOfType(it, PsiErrorElement::class.java) }.firstOrNull()

        if (firstSyntaxError != null) {
            val count = javaFiles.filter { PsiTreeUtil.hasErrorElements(it) }.count()
            val question = firstSyntaxError.containingFile.name +
                           (if (count > 1) " and ${count - 1} more Java files" else " file") +
                           " contain syntax errors, the conversion result may be incorrect"

            val okText = "Investigate Errors"
            val cancelText = "Proceed with Conversion"
            if (Messages.showOkCancelDialog(
                    project,
                    question,
                    title,
                    okText,
                    cancelText,
                    Messages.getWarningIcon()
            ) == Messages.OK) {
                NavigationUtil.activateFileWithPsiElement(firstSyntaxError.navigationElement)
                return
            }
        }

        convertFiles(javaFiles, project)
    }

    override fun update(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val project = e.project ?: return

        e.presentation.isEnabled = isAnyJavaFileSelected(project, virtualFiles)
    }

    private fun isAnyJavaFileSelected(project: Project, files: Array<VirtualFile>): Boolean {
        val manager = PsiManager.getInstance(project)

        if (files.any { manager.findFile(it) is PsiJavaFile && it.isWritable }) return true
        return files.any { it.isDirectory && isAnyJavaFileSelected(project, it.children) }
    }

    private fun selectedJavaFiles(e: AnActionEvent): Sequence<PsiJavaFile> {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return sequenceOf()
        val project = e.project ?: return sequenceOf()
        return allJavaFiles(virtualFiles, project)
    }

    private fun allJavaFiles(filesOrDirs: Array<VirtualFile>, project: Project): Sequence<PsiJavaFile> {
        val manager = PsiManager.getInstance(project)
        return allFiles(filesOrDirs)
                .asSequence()
                .mapNotNull { manager.findFile(it) as? PsiJavaFile }
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
}
