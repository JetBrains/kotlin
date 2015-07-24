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
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.packageMatchesDirectory

public class MoveKotlinFileHandler : MoveFileHandler() {
    private data class MoveContext(
            val packageNameInfo: PackageNameInfo,
            val declarationMoveProcessor: MoveKotlinTopLevelDeclarationsProcessor
    )

    private var moveContext: MoveContext? = null

    private fun initMoveContext(psiFile: PsiFile, newParent: PsiDirectory): MoveContext? {
        this.moveContext = null

        if (psiFile !is JetFile || !(psiFile.updatePackageDirective ?: psiFile.packageMatchesDirectory())) return null

        val newPackage = newParent.getPackage() ?: return null

        val packageNameInfo = PackageNameInfo(psiFile.getPackageFqName(), FqName(newPackage.getQualifiedName()))
        if (packageNameInfo.oldPackageName == packageNameInfo.newPackageName) return null
        val project = psiFile.getProject()

        val declarationMoveProcessor = MoveKotlinTopLevelDeclarationsProcessor(
                project,
                MoveKotlinTopLevelDeclarationsOptions(
                        elementsToMove = psiFile.getDeclarations().filterIsInstance<JetNamedDeclaration>(),
                        moveTarget = DeferredJetFileKotlinMoveTarget(project, packageNameInfo.newPackageName) {
                            MoveFilesOrDirectoriesUtil.doMoveFile(psiFile, newParent)
                            newParent.findFile(psiFile.getName()) as? JetFile
                        },
                        updateInternalReferences = false
                ),
                Mover.Idle
        )

        val moveContext = MoveContext(packageNameInfo, declarationMoveProcessor)
        this.moveContext = moveContext
        return moveContext
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
    ): List<UsageInfo> {
        val moveContext = initMoveContext(psiFile, newParent) ?: return emptyList()
        return moveContext.declarationMoveProcessor.findUsages().toList()
    }

    override fun prepareMovedFile(file: PsiFile, moveDestination: PsiDirectory, oldToNewMap: Map<PsiElement, PsiElement>) {
        initMoveContext(file, moveDestination)
    }

    override fun updateMovedFile(file: PsiFile) {
        if (file !is JetFile) return
        val moveContext = this.moveContext ?: return

        file.updatePackageDirective = null
        val packageNameInfo = moveContext.packageNameInfo

        val internalUsages = file.getInternalReferencesToUpdateOnPackageNameChange(packageNameInfo)
        file.getPackageDirective()?.setFqName(packageNameInfo.newPackageName)
        postProcessMoveUsages(internalUsages)
    }

    override fun retargetUsages(usageInfos: List<UsageInfo>?, oldToNewMap: Map<PsiElement, PsiElement>?) {
        val moveContext = this.moveContext ?: return
        val processor = moveContext.declarationMoveProcessor
        processor.project.runWithElementsToShortenIsEmptyIgnored {
            try {
                usageInfos?.let { processor.execute(it) }
            } finally {
                this.moveContext = null
            }
        }
    }
}
