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
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.codeInsight.shorten.runWithElementsToShortenIsEmptyIgnored
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.packageMatchesDirectory
import org.jetbrains.kotlin.idea.refactoring.hasIdentifiersOnly
import org.jetbrains.kotlin.idea.refactoring.move.*
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

class MoveKotlinFileHandler : MoveFileHandler() {
    internal class InternalUsagesWrapper(file: KtFile, val usages: List<UsageInfo>) : UsageInfo(file)

    // This is special 'PsiElement' whose purpose is to wrap MoveKotlinTopLevelDeclarationsProcessor
    // so that it can be kept in the transition map
    private class MoveContext(
            psiManager: PsiManager,
            val declarationMoveProcessor: MoveKotlinDeclarationsProcessor
    ): LightElement(psiManager, KotlinLanguage.INSTANCE) {
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
        if (oldPackageName.asString() == newPackageName.asString()) return null
        if (!newPackageName.hasIdentifiersOnly()) return null

        return ContainerChangeInfo(ContainerInfo.Package(oldPackageName), ContainerInfo.Package(newPackageName.toSafe()))
    }

    fun initMoveProcessor(psiFile: PsiFile, newParent: PsiDirectory?): MoveKotlinDeclarationsProcessor? {
        if (psiFile !is KtFile) return null
        val packageNameInfo = psiFile.getPackageNameInfo(newParent, false) ?: return null

        val project = psiFile.project

        val newPackage = packageNameInfo.newContainer
        val moveTarget = when (newPackage) {
            ContainerInfo.UnknownPackage -> EmptyKotlinMoveTarget

            else -> KotlinMoveTargetForDeferredFile(newPackage.fqName!!, newParent) {
                MoveFilesOrDirectoriesUtil.doMoveFile(psiFile, newParent)
                newParent?.findFile(psiFile.name) as? KtFile
            }
        }

        val declarationMoveProcessor = MoveKotlinDeclarationsProcessor(
                project,
                MoveDeclarationsDescriptor(
                        elementsToMove = psiFile.declarations.filterIsInstance<KtNamedDeclaration>(),
                        moveTarget = moveTarget,
                        delegate = MoveDeclarationsDelegate.TopLevel,
                        updateInternalReferences = false
                ),
                Mover.Idle
        )
        return declarationMoveProcessor
    }

    override fun canProcessElement(element: PsiFile?): Boolean {
        if (element is PsiCompiledElement || element !is KtFile) return false
        return !JavaProjectRootsUtil.isOutsideJavaSourceRoot(element)
    }

    internal fun findInternalUsages(file: KtFile, newParent: PsiDirectory): InternalUsagesWrapper {
        val packageNameInfo = file.getPackageNameInfo(newParent, false)
        val usages = packageNameInfo?.let { file.getInternalReferencesToUpdateOnPackageNameChange(it) } ?: emptyList()
        return InternalUsagesWrapper(file, usages)
    }

    override fun findUsages(
            psiFile: PsiFile,
            newParent: PsiDirectory?,
            searchInComments: Boolean,
            searchInNonJavaFiles: Boolean
    ): List<UsageInfo> {
        if (psiFile !is KtFile) return emptyList()

        val usages = ArrayList<UsageInfo>()
        initMoveProcessor(psiFile, newParent)?.findUsages()?.let { usages += it }
        newParent?.let { usages += findInternalUsages(psiFile, it) }
        return usages
    }

    override fun prepareMovedFile(file: PsiFile, moveDestination: PsiDirectory, oldToNewMap: MutableMap<PsiElement, PsiElement>) {
        val moveProcessor = initMoveProcessor(file, moveDestination) ?: return
        val moveContext = MoveContext(file.manager, moveProcessor)
        oldToNewMap[moveContext] = moveContext
    }

    override fun updateMovedFile(file: PsiFile) {
        if (file !is KtFile) return
        val newDirectory = file.parent ?: return
        val packageNameInfo = file.getPackageNameInfo(newDirectory, true) ?: return
        file.packageDirective?.fqName = packageNameInfo.newContainer.fqName!!
    }

    override fun retargetUsages(usageInfos: List<UsageInfo>?, oldToNewMap: Map<PsiElement, PsiElement>) {
        val moveContext = oldToNewMap.keys.firstIsInstanceOrNull<MoveContext>() ?: return
        retargetUsages(usageInfos, moveContext.declarationMoveProcessor)
    }

    fun retargetUsages(usageInfos: List<UsageInfo>?, moveDeclarationsProcessor: MoveKotlinDeclarationsProcessor) {
        postProcessMoveUsages(usageInfos?.firstIsInstanceOrNull<InternalUsagesWrapper>()?.usages ?: emptyList())
        moveDeclarationsProcessor.project.runWithElementsToShortenIsEmptyIgnored {
            usageInfos?.let { moveDeclarationsProcessor.execute(it) }
        }
    }
}
