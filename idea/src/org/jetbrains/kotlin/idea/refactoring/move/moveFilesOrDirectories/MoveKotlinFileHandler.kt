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
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightElement
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.idea.codeInsight.shorten.runWithElementsToShortenIsEmptyIgnored
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.packageMatchesDirectory
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
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

public class MoveKotlinFileHandler : MoveFileHandler() {
    // This is special 'PsiElement' whose purpose is to wrap MoveKotlinTopLevelDeclarationsProcessor
    // so that it can be kept in the transition map
    class MoveContext(
            psiManager: PsiManager,
            val declarationMoveProcessor: MoveKotlinTopLevelDeclarationsProcessor
    ): LightElement(psiManager, JetLanguage.INSTANCE) {
        override fun toString() = ""
    }

    private fun JetFile.getPackageNameInfo(newParent: PsiDirectory, clearUserData: Boolean): PackageNameInfo? {
        val shouldUpdatePackageDirective = updatePackageDirective ?: packageMatchesDirectory()
        updatePackageDirective = if (clearUserData) null else shouldUpdatePackageDirective

        if (!shouldUpdatePackageDirective) return null
        val newPackage = newParent.getPackage() ?: return null

        val oldPackageName = getPackageFqName()
        val newPackageName = FqName(newPackage.getQualifiedName())
        if (oldPackageName == newPackageName) return null

        return PackageNameInfo(oldPackageName, newPackageName)
    }

    private fun initMoveProcessor(psiFile: PsiFile, newParent: PsiDirectory): MoveKotlinTopLevelDeclarationsProcessor? {
        if (psiFile !is JetFile) return null
        val packageNameInfo = psiFile.getPackageNameInfo(newParent, false) ?: return null

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
        return declarationMoveProcessor
    }

    override fun canProcessElement(element: PsiFile?): Boolean {
        if (element is PsiCompiledElement || element !is JetFile) return false
        return !JavaProjectRootsUtil.isOutsideJavaSourceRoot(element)
    }

    override fun findUsages(psiFile: PsiFile, newParent: PsiDirectory, searchInComments: Boolean, searchInNonJavaFiles: Boolean) =
            initMoveProcessor(psiFile, newParent)?.findUsages()?.toList() ?: emptyList()

    override fun prepareMovedFile(file: PsiFile, moveDestination: PsiDirectory, oldToNewMap: MutableMap<PsiElement, PsiElement>) {
        val moveProcessor = initMoveProcessor(file, moveDestination) ?: return
        val moveContext = MoveContext(file.getManager(), moveProcessor)
        oldToNewMap[moveContext] = moveContext
    }

    override fun updateMovedFile(file: PsiFile) {
        if (file !is JetFile) return

        val newDirectory = file.getParent() ?: return
        val packageNameInfo = file.getPackageNameInfo(newDirectory, true) ?: return

        val internalUsages = file.getInternalReferencesToUpdateOnPackageNameChange(packageNameInfo)
        file.getPackageDirective()?.setFqName(packageNameInfo.newPackageName)
        postProcessMoveUsages(internalUsages)
    }

    override fun retargetUsages(usageInfos: List<UsageInfo>?, oldToNewMap: Map<PsiElement, PsiElement>) {
        val moveContext = oldToNewMap.keySet().firstIsInstanceOrNull<MoveContext>() ?: return
        val processor = moveContext.declarationMoveProcessor
        processor.project.runWithElementsToShortenIsEmptyIgnored {
            usageInfos?.let { processor.execute(it) }
        }
    }
}
