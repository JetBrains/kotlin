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

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.light.LightElement
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.packageMatchesDirectory
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.refactoring.hasIdentifiersOnly
import org.jetbrains.kotlin.idea.refactoring.move.ContainerChangeInfo
import org.jetbrains.kotlin.idea.refactoring.move.ContainerInfo
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.refactoring.move.updatePackageDirective
import org.jetbrains.kotlin.idea.roots.isOutsideKotlinAwareSourceRoot
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.UserDataProperty

internal var KtFile.allElementsToMove: List<PsiElement>? by UserDataProperty(Key.create("SCOPE_TO_MOVE"))

class MoveKotlinFileHandler : MoveFileHandler() {
    internal class FileInfo(file: KtFile) : UsageInfo(file)

    // This is special 'PsiElement' whose purpose is to wrap MoveKotlinTopLevelDeclarationsProcessor
    // so that it can be kept in the usage info list
    private class MoveContext(
            val file: PsiFile,
            val declarationMoveProcessor: MoveKotlinDeclarationsProcessor
    ): LightElement(file.manager, KotlinLanguage.INSTANCE) {
        override fun toString() = ""
    }

    private fun KtFile.getPackageNameInfo(newParent: PsiDirectory?, clearUserData: Boolean): ContainerChangeInfo? {
        val shouldUpdatePackageDirective = updatePackageDirective ?: packageMatchesDirectory()
        updatePackageDirective = if (clearUserData) null else shouldUpdatePackageDirective

        if (!shouldUpdatePackageDirective) return null

        val oldPackageName = packageFqName
        val newPackage = newParent?.getPackage() ?: return ContainerChangeInfo(ContainerInfo.Package(oldPackageName),
                                                                               ContainerInfo.UnknownPackage)

        val newPackageName = FqNameUnsafe(newPackage.qualifiedName)
        if (oldPackageName.asString() == newPackageName.asString()
            && ModuleUtilCore.findModuleForPsiElement(this) == ModuleUtilCore.findModuleForPsiElement(newParent)) return null
        if (!newPackageName.hasIdentifiersOnly()) return null

        return ContainerChangeInfo(ContainerInfo.Package(oldPackageName), ContainerInfo.Package(newPackageName.toSafe()))
    }

    fun initMoveProcessor(psiFile: PsiFile, newParent: PsiDirectory?, withConflicts: Boolean): MoveKotlinDeclarationsProcessor? {
        if (psiFile !is KtFile) return null
        val packageNameInfo = psiFile.getPackageNameInfo(newParent, false) ?: return null

        val project = psiFile.project

        val newPackage = packageNameInfo.newContainer
        val moveTarget = when (newPackage) {
            ContainerInfo.UnknownPackage -> EmptyKotlinMoveTarget

            else -> KotlinMoveTargetForDeferredFile(newPackage.fqName!!, newParent) {
                newParent?.let {
                    MoveFilesOrDirectoriesUtil.doMoveFile(psiFile, it)
                    it.findFile(psiFile.name) as? KtFile
                }
            }
        }

        return MoveKotlinDeclarationsProcessor(
            MoveDeclarationsDescriptor(
                project = project,
                moveSource = MoveSource(psiFile),
                moveTarget = moveTarget,
                delegate = MoveDeclarationsDelegate.TopLevel,
                allElementsToMove = psiFile.allElementsToMove,
                analyzeConflicts = withConflicts
            )
        )
    }

    override fun canProcessElement(element: PsiFile?): Boolean {
        if (element is PsiCompiledElement || element !is KtFile) return false
        return !isOutsideKotlinAwareSourceRoot(element)
    }

    override fun findUsages(
            psiFile: PsiFile,
            newParent: PsiDirectory?,
            searchInComments: Boolean,
            searchInNonJavaFiles: Boolean
    ): List<UsageInfo> {
        return findUsages(psiFile, newParent, true)
    }

    fun findUsages(
            psiFile: PsiFile,
            newParent: PsiDirectory?,
            withConflicts: Boolean
    ): List<UsageInfo> {
        if (psiFile !is KtFile) return emptyList()

        val usages = arrayListOf<UsageInfo>(FileInfo(psiFile))
        initMoveProcessor(psiFile, newParent, withConflicts)?.let {
            usages += it.findUsages()
            usages += it.getConflictsAsUsages()
        }
        return usages
    }

    override fun prepareMovedFile(file: PsiFile, moveDestination: PsiDirectory, oldToNewMap: MutableMap<PsiElement, PsiElement>) {
        if (file !is KtFile) return
        val moveProcessor = initMoveProcessor(file, moveDestination, false) ?: return
        val moveContext = MoveContext(file, moveProcessor)
        oldToNewMap[moveContext] = moveContext
        val packageNameInfo = file.getPackageNameInfo(moveDestination, true) ?: return
        val newFqName = packageNameInfo.newContainer.fqName
        if (newFqName != null) {
            file.packageDirective?.fqName = newFqName.quoteIfNeeded()
        }
    }

    override fun updateMovedFile(file: PsiFile) {

    }

    override fun retargetUsages(usageInfos: List<UsageInfo>?, oldToNewMap: Map<PsiElement, PsiElement>) {
        val currentFile = (usageInfos?.firstOrNull() as? FileInfo)?.element
        val moveContext = oldToNewMap.keys.firstOrNull { it is MoveContext && it.file == currentFile} as? MoveContext ?: return
        retargetUsages(usageInfos, moveContext.declarationMoveProcessor)
    }

    fun retargetUsages(usageInfos: List<UsageInfo>?, moveDeclarationsProcessor: MoveKotlinDeclarationsProcessor) {
        usageInfos?.let { moveDeclarationsProcessor.doPerformRefactoring(it) }
    }
}
