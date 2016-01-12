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

package org.jetbrains.kotlin.idea.refactoring.move.changePackage

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.codeInsight.shorten.runWithElementsToShortenIsEmptyIgnored
import org.jetbrains.kotlin.idea.refactoring.move.ContainerChangeInfo
import org.jetbrains.kotlin.idea.refactoring.move.ContainerInfo
import org.jetbrains.kotlin.idea.refactoring.move.getInternalReferencesToUpdateOnPackageNameChange
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.refactoring.move.postProcessMoveUsages
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration

class KotlinChangePackageRefactoring(val file: KtFile) {
    private val project = file.project

    fun run(newFqName: FqName) {
        val packageDirective = file.packageDirective ?: return
        val currentFqName = packageDirective.fqName

        val declarationProcessor = MoveKotlinDeclarationsProcessor(
                project,
                MoveDeclarationsDescriptor(
                        elementsToMove = file.declarations.filterIsInstance<KtNamedDeclaration>(),
                        moveTarget = object: KotlinMoveTarget {
                            override val targetContainerFqName = newFqName

                            override fun getOrCreateTargetPsi(originalPsi: PsiElement) = originalPsi.containingFile as? KtFile

                            override fun getTargetPsiIfExists(originalPsi: PsiElement) = null

                            override fun verify(file: PsiFile) = null
                        },
                        delegate = MoveDeclarationsDelegate.TopLevel,
                        updateInternalReferences = false
                ),
                Mover.Idle // we don't need to move any declarations physically
        )

        val declarationUsages = declarationProcessor.findUsages().toList()
        val changeInfo = ContainerChangeInfo(ContainerInfo.Package(currentFqName), ContainerInfo.Package(newFqName))
        val internalUsages = file.getInternalReferencesToUpdateOnPackageNameChange(changeInfo)

        project.executeWriteCommand("Change file's package to '${newFqName.asString()}'") {
            packageDirective.fqName = newFqName
            postProcessMoveUsages(internalUsages)
            project.runWithElementsToShortenIsEmptyIgnored { declarationProcessor.execute(declarationUsages) }
        }
    }
}
