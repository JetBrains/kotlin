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
import java.util.LinkedList
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import java.util.ArrayList
import java.util.HashSet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.codeStyle.CodeStyleManager
import java.io.IOException
import com.intellij.openapi.ui.ex.MessagesEx
import com.intellij.psi.PsiFile
import org.jetbrains.jet.plugin.j2k.J2kPostProcessor
import com.intellij.psi.PsiElement
import org.jetbrains.jet.j2k.PostProcessor
import com.intellij.openapi.vfs.CharsetToolkit

public class JavaToKotlinAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)!!
        val project = CommonDataKeys.PROJECT.getData(e.getDataContext())!!
        val selectedJavaFiles = getAllJavaFiles(virtualFiles, project)
        if (selectedJavaFiles.isEmpty()) return

        val converter = JavaToKotlinConverter(project, ConverterSettings.defaultSettings, FilesConversionScope(selectedJavaFiles), IdeaReferenceSearcher)
        CommandProcessor.getInstance().executeCommand(project, object : Runnable {
            override fun run() {
                val newFiles = convertFiles(converter, selectedJavaFiles)
                deleteFiles(selectedJavaFiles)
                reformatFiles(newFiles, project)
                for (vf in newFiles) {
                    FileEditorManager.getInstance(project).openFile(vf, true)
                }
            }
        }, "Convert files from Java to Kotlin", "group_id")
    }

    override fun update(e: AnActionEvent) {
        val enabled = e.getData<Array<VirtualFile>>(CommonDataKeys.VIRTUAL_FILE_ARRAY) != null
        e.getPresentation().setVisible(enabled)
        e.getPresentation().setEnabled(enabled)
    }

    private fun getChildrenRecursive(baseDir: VirtualFile?): List<VirtualFile> {
        val result = LinkedList<VirtualFile>()
        val children = if (baseDir != null) baseDir.getChildren() else VirtualFile.EMPTY_ARRAY
        result.addAll(children)
        for (f in children) {
            result.addAll(getChildrenRecursive(f))
        }
        return result
    }

    private fun getAllJavaFiles(vFiles: Array<VirtualFile>, project: Project): List<PsiJavaFile> {
        val filesSet = allVirtualFiles(vFiles)
        val manager = PsiManager.getInstance(project)
        val res = ArrayList<PsiJavaFile>()
        for (file in filesSet) {
            val psiFile = manager.findFile(file)
            if (psiFile != null && psiFile is PsiJavaFile) {
                res.add(psiFile)
            }
        }
        return res
    }

    private fun allVirtualFiles(vFiles: Array<VirtualFile>): Set<VirtualFile> {
        val filesSet = HashSet<VirtualFile>()
        for (f in vFiles) {
            filesSet.add(f)
            filesSet.addAll(getChildrenRecursive(f))
        }
        return filesSet
    }

    private fun reformatFiles(allJetFiles: List<VirtualFile>, project: Project) {
        for (vf in allJetFiles) {
            ApplicationManager.getApplication().runWriteAction {
                val psiFile = PsiManager.getInstance(project).findFile(vf)
                if (psiFile != null) {
                    CodeStyleManager.getInstance(project).reformat(psiFile)
                }
            }
        }
    }

    private fun convertFiles(converter: JavaToKotlinConverter, allJavaFilesNear: List<PsiJavaFile>): List<VirtualFile> {
        val result = LinkedList<VirtualFile>()
        for (f in allJavaFilesNear) {
            ApplicationManager.getApplication().runWriteAction {
                val vf = convertOneFile(converter, f)
                if (vf != null) {
                    result.add(vf)
                }
            }
        }
        return result
    }

    private fun deleteFiles(allJavaFilesNear: List<PsiJavaFile>) {
        for (psiFile in allJavaFilesNear) {
            ApplicationManager.getApplication().runWriteAction(object : Runnable {
                override fun run() {
                    try {
                        psiFile.getVirtualFile()?.delete(this)
                    }
                    catch (e: IOException) {
                        MessagesEx.error(psiFile.getProject(), e.getMessage()).showLater()
                    }
                }
            })
        }
    }

    private private fun convertOneFile(converter: JavaToKotlinConverter, psiFile: PsiFile): VirtualFile? {
        try {
            val virtualFile = psiFile.getVirtualFile()
            if (psiFile is PsiJavaFile && virtualFile != null) {
                val postProcessor = J2kPostProcessor(psiFile)
                val result = converter.elementsToKotlin(Pair<PsiElement, PostProcessor>(psiFile, postProcessor)).get(0) //TODO: convert all files in one call!
                val manager = psiFile.getManager()
                assert(manager != null)
                val copy = virtualFile.copy(manager, virtualFile.getParent(), virtualFile.getNameWithoutExtension() + ".kt")
                copy.setBinaryContent(CharsetToolkit.getUtf8Bytes(result))
                return copy
            }
        }
        catch (e: IOException) {
            MessagesEx.error(psiFile.getProject(), e.getMessage()).showLater()
        }

        return null
    }
}
