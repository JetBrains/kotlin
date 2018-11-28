/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.ide.IdeView
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.awt.datatransfer.DataFlavor

class KotlinFilePasteProvider : PasteProvider {
    override fun isPastePossible(dataContext: DataContext): Boolean = true

    override fun performPaste(dataContext: DataContext) {
        val text = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor) ?: return
        val project = project(dataContext)
        val ideView = ideView(dataContext)
        if (project == null || ideView == null || ideView.directories.isEmpty()) return

        val ktFile = KtPsiFactory(project).createFile(text)
        val fileName = (ktFile.declarations.firstOrNull()?.name ?: return) + ".kt"
        val directory = ideView.getOrChooseDirectory() ?: return
        project.executeWriteCommand("Create Kotlin file") {
            val file = try {
                directory.createFile(fileName)
            } catch (e: IncorrectOperationException) {
                return@executeWriteCommand
            }

            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(file)
            if (document != null) {
                document.setText(text)
                documentManager.commitDocument(document)
                val qualName = JavaDirectoryService.getInstance()?.getPackage(directory)?.qualifiedName
                if (qualName != null && file is KtFile) {
                    file.packageFqName = FqName(qualName)
                }
                OpenFileDescriptor(project, file.virtualFile).navigate(true)
            }
        }
    }

    override fun isPasteEnabled(dataContext: DataContext): Boolean {
        val project = project(dataContext)
        val ideView = ideView(dataContext)
        if (project == null || ideView == null || ideView.directories.isEmpty()) return false
        val text = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor) ?: return false
        //todo: KT-25329, to remove these heuristics
        if (text.contains(";\n") ||
            ((text.contains("public interface") || text.contains("public class")) &&
                    !text.contains("fun "))) return false //Optimisation for Java. Kotlin doesn't need that...
        val file = KtPsiFactory(project).createFile(text)
        return !PsiTreeUtil.hasErrorElements(file)
    }

    private fun project(dataContext: DataContext): Project? = CommonDataKeys.PROJECT.getData(dataContext)
    private fun ideView(dataContext: DataContext): IdeView? = LangDataKeys.IDE_VIEW.getData(dataContext)
}
