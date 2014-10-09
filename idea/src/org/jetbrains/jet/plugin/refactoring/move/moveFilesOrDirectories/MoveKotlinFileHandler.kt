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

package org.jetbrains.jet.plugin.refactoring.move.moveFilesOrDirectories

import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import com.intellij.openapi.roots.JavaProjectRootsUtil
import com.intellij.psi.PsiCompiledElement
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.plugin.references.JetSimpleNameReference
import org.jetbrains.jet.lang.psi.psiUtil.getPackage
import org.jetbrains.jet.plugin.refactoring.move.PackageNameInfo
import org.jetbrains.jet.plugin.refactoring.move.getInternalReferencesToUpdateOnPackageNameChange
import org.jetbrains.jet.plugin.refactoring.move.postProcessMoveUsages
import org.jetbrains.jet.plugin.refactoring.move.moveTopLevelDeclarations.MoveKotlinTopLevelDeclarationsProcessor
import org.jetbrains.jet.plugin.refactoring.move.moveTopLevelDeclarations.MoveKotlinTopLevelDeclarationsOptions
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import org.jetbrains.jet.plugin.refactoring.move.moveTopLevelDeclarations.DeferredJetFileKotlinMoveTarget
import org.jetbrains.jet.plugin.refactoring.move.moveTopLevelDeclarations.Mover
import org.jetbrains.jet.plugin.codeInsight.ensureElementsToShortenIsEmptyBeforeRefactoring

public class MoveKotlinFileHandler : MoveFileHandler() {
    private var packageNameInfo: PackageNameInfo? = null
    private var declarationMoveProcessor: MoveKotlinTopLevelDeclarationsProcessor? = null

    private fun clearState() {
        packageNameInfo = null
        declarationMoveProcessor = null
    }

    private fun JetFile.packageMatchesDirectory(): Boolean {
        return getPackageFqName().asString() == getParent()?.getPackage()?.getQualifiedName()
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

        if (psiFile !is JetFile || !psiFile.packageMatchesDirectory()) return null

        val newPackage = newParent.getPackage()
        if (newPackage == null) return null

        val packageNameInfo = PackageNameInfo(psiFile.getPackageFqName(), FqName(newPackage.getQualifiedName()))
        val project = psiFile.getProject()

        val declarationMoveProcessor = MoveKotlinTopLevelDeclarationsProcessor(
                project,
                MoveKotlinTopLevelDeclarationsOptions(
                        elementsToMove = psiFile.getDeclarations().filterIsInstance(javaClass<JetNamedDeclaration>()),
                        moveTarget = DeferredJetFileKotlinMoveTarget(project, packageNameInfo.newPackageName) {
                            MoveFilesOrDirectoriesUtil.doMoveFile(psiFile, newParent)
                            newParent.findFile(psiFile.getName()) as? JetFile
                        },
                        updateInternalReferences = false
                ),
                object: Mover {
                    [suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")]
                    override fun invoke(originalElement: JetNamedDeclaration, targetFile: JetFile): JetNamedDeclaration = originalElement
                }
        )

        this.packageNameInfo = packageNameInfo
        this.declarationMoveProcessor = declarationMoveProcessor

        return declarationMoveProcessor.findUsages().toList()
    }

    override fun prepareMovedFile(file: PsiFile, moveDestination: PsiDirectory, oldToNewMap: Map<PsiElement, PsiElement>) {

    }

    override fun updateMovedFile(file: PsiFile) {
        if (file !is JetFile) return

        val packageNameInfo = packageNameInfo
        if (packageNameInfo == null) return

        val usages = file.getInternalReferencesToUpdateOnPackageNameChange(packageNameInfo)
        postProcessMoveUsages(usages)

        val packageRef = file.getPackageDirective()?.getLastReferenceExpression()?.getReference() as? JetSimpleNameReference
        packageRef?.bindToFqName(packageNameInfo.newPackageName)
    }

    override fun retargetUsages(usageInfos: List<UsageInfo>?, oldToNewMap: Map<PsiElement, PsiElement>?) {
        val processor = declarationMoveProcessor ?: return

        val project = processor.project
        val ensureElementsToShortenIsEmpty = project.ensureElementsToShortenIsEmptyBeforeRefactoring

        try {
            project.ensureElementsToShortenIsEmptyBeforeRefactoring = false
            usageInfos?.let { processor.execute(it) }
        } finally {
            project.ensureElementsToShortenIsEmptyBeforeRefactoring = ensureElementsToShortenIsEmpty
            clearState()
        }
    }
}