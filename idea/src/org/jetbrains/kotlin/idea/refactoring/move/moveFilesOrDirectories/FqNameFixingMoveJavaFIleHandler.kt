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

package org.jetbrains.kotlin.idea.refactoring.move.moveFilesOrDirectories

import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.util.FileTypeUtils
import com.intellij.refactoring.move.moveClassesOrPackages.MoveJavaFileHandler
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.psi.NotNullableUserDataProperty

internal var PsiJavaFile.shouldFixFqName: Boolean by NotNullableUserDataProperty(Key.create("SHOULD_FIX_FQ_NAME"), false)

class FqNameFixingMoveJavaFileHandler : MoveFileHandler() {
    private val delegate = MoveJavaFileHandler()

    override fun canProcessElement(element: PsiFile) =
            delegate.canProcessElement(element)

    override fun findUsages(psiFile: PsiFile, newParent: PsiDirectory?, searchInComments: Boolean, searchInNonJavaFiles: Boolean) =
            delegate.findUsages(psiFile, newParent, searchInComments, searchInNonJavaFiles)

    override fun prepareMovedFile(file: PsiFile, moveDestination: PsiDirectory, oldToNewMap: MutableMap<PsiElement, PsiElement>) {
        delegate.prepareMovedFile(file, moveDestination, oldToNewMap)
        if (file is PsiJavaFile && file.shouldFixFqName) {
            val newPackage = JavaDirectoryService.getInstance().getPackage(moveDestination) ?: return
            if (!FileTypeUtils.isInServerPageFile(file)) {
                file.packageName = newPackage.qualifiedName
            }
        }
    }

    override fun updateMovedFile(file: PsiFile) =
            delegate.updateMovedFile(file)

    override fun retargetUsages(usageInfos: MutableList<UsageInfo>, oldToNewMap: MutableMap<PsiElement, PsiElement>) =
            delegate.retargetUsages(usageInfos, oldToNewMap)
}