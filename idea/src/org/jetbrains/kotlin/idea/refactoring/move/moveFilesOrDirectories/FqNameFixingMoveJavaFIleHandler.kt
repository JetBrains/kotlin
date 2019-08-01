/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveFilesOrDirectories

import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.util.FileTypeUtils
import com.intellij.psi.util.PsiUtil
import com.intellij.refactoring.move.moveClassesOrPackages.MoveJavaFileHandler
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty

internal var PsiJavaFile.shouldFixFqName: Boolean by NotNullableUserDataProperty(Key.create("SHOULD_FIX_FQ_NAME"), false)

class FqNameFixingMoveJavaFileHandler : MoveFileHandler() {
    private val delegate = MoveJavaFileHandler()

    override fun canProcessElement(element: PsiFile) =
        delegate.canProcessElement(element)

    override fun findUsages(
        psiFile: PsiFile,
        newParent: PsiDirectory?,
        searchInComments: Boolean,
        searchInNonJavaFiles: Boolean
    ): MutableList<UsageInfo>? = delegate.findUsages(psiFile, newParent, searchInComments, searchInNonJavaFiles)

    override fun prepareMovedFile(file: PsiFile, moveDestination: PsiDirectory, oldToNewMap: MutableMap<PsiElement, PsiElement>) {
        delegate.prepareMovedFile(file, moveDestination, oldToNewMap)
        if (file is PsiJavaFile && file.shouldFixFqName) {
            val newPackage = JavaDirectoryService.getInstance().getPackage(moveDestination) ?: return
            if (!FileTypeUtils.isInServerPageFile(file) && !PsiUtil.isModuleFile(file)) {
                file.packageName = newPackage.qualifiedName
                with(PsiDocumentManager.getInstance(file.project)) {
                    commitDocument(getDocument(file) ?: return@with)
                }
            }
        }
    }

    override fun updateMovedFile(file: PsiFile) =
        delegate.updateMovedFile(file)

    override fun retargetUsages(usageInfos: MutableList<UsageInfo>, oldToNewMap: MutableMap<PsiElement, PsiElement>) =
        delegate.retargetUsages(usageInfos, oldToNewMap)
}