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

package org.jetbrains.kotlin.idea.refactoring.move.moveFilesOrDirectories

import com.intellij.openapi.roots.JavaProjectRootsUtil
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.codeInsight.shorten.runWithElementsToShortenIsEmptyIgnored
import org.jetbrains.kotlin.idea.refactoring.move.PackageNameInfo
import org.jetbrains.kotlin.idea.refactoring.move.getInternalReferencesToUpdateOnPackageNameChange
import org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.DeferredJetFileKotlinMoveTarget
import org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.MoveKotlinTopLevelDeclarationsOptions
import org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.MoveKotlinTopLevelDeclarationsProcessor
import org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.Mover
import org.jetbrains.kotlin.idea.refactoring.move.postProcessMoveUsages
import org.jetbrains.kotlin.idea.refactoring.move.updatePackageDirective
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getPackage
import org.jetbrains.kotlin.psi.psiUtil.packageMatchesDirectory

public class MoveKotlinFileHandler : MoveFileHandler() {
    private var packageNameInfo: PackageNameInfo? = null
    private var declarationMoveProcessor: MoveKotlinTopLevelDeclarationsProcessor? = null

    private fun clearState() {
        packageNameInfo = null
        declarationMoveProcessor = null
    }

    override fun canProcessElement(element: PsiFile?): Boolean {
        if (element is PsiCompiledElement || element !is JetFile) return false
        return !JavaProjectRootsUtil.isOutsideJavaSourceRoot(element)
    }

    override fun findUsages(
            psiFile: PsiFile,
            newParent: PsiDirectory,
            searchInComments: Boolean,
            searchInNonJavaFiles: Boolean
    ): List<UsageInfo>? {
        clearState()

        if (psiFile !is JetFile || !(psiFile.updatePackageDirective ?: psiFile.packageMatchesDirectory())) return null

        val newPackage = newParent.getPackage() ?: return null

        val packageNameInfo = PackageNameInfo(psiFile.getPackageFqName(), FqName(newPackage.getQualifiedName()))
        if (packageNameInfo.oldPackageName == packageNameInfo.newPackageName) return null
        val project = psiFile.getProject()

        val declarationMoveProcessor = MoveKotlinTopLevelDeclarationsProcessor(
                project,
                MoveKotlinTopLevelDeclarationsOptions(
                        sourceFile = psiFile,
                        elementsToMove = psiFile.getDeclarations().filterIsInstance<JetNamedDeclaration>(),
                        moveTarget = DeferredJetFileKotlinMoveTarget(project, packageNameInfo.newPackageName) {
                            MoveFilesOrDirectoriesUtil.doMoveFile(psiFile, newParent)
                            newParent.findFile(psiFile.getName()) as? JetFile
                        },
                        updateInternalReferences = false
                ),
                Mover.Idle
        )

        this.packageNameInfo = packageNameInfo
        this.declarationMoveProcessor = declarationMoveProcessor

        return declarationMoveProcessor.findUsages().toList()
    }

    override fun prepareMovedFile(file: PsiFile, moveDestination: PsiDirectory, oldToNewMap: Map<PsiElement, PsiElement>) {

    }

    override fun updateMovedFile(file: PsiFile) {
        if (file !is JetFile) return

        file.updatePackageDirective = null
        val packageNameInfo = packageNameInfo ?: return

        val internalUsages = file.getInternalReferencesToUpdateOnPackageNameChange(packageNameInfo)
        file.getPackageDirective()?.setFqName(packageNameInfo.newPackageName)
        postProcessMoveUsages(internalUsages)
    }

    override fun retargetUsages(usageInfos: List<UsageInfo>?, oldToNewMap: Map<PsiElement, PsiElement>?) {
        val processor = declarationMoveProcessor ?: return
        processor.project.runWithElementsToShortenIsEmptyIgnored {
            try {
                usageInfos?.let { processor.execute(it) }
            } finally {
                clearState()
            }
        }
    }
}
