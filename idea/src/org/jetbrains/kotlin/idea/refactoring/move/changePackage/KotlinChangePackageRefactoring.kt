/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.changePackage

import com.intellij.refactoring.RefactoringBundle
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.core.util.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.refactoring.move.ContainerChangeInfo
import org.jetbrains.kotlin.idea.refactoring.move.ContainerInfo
import org.jetbrains.kotlin.idea.refactoring.move.getInternalReferencesToUpdateOnPackageNameChange
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.refactoring.move.postProcessMoveUsages
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

class KotlinChangePackageRefactoring(val file: KtFile) {
    private val project = file.project

    fun run(newFqName: FqName) {
        val packageDirective = file.packageDirective ?: return
        val currentFqName = packageDirective.fqName

        val declarationProcessor = MoveKotlinDeclarationsProcessor(
            MoveDeclarationsDescriptor(
                project = project,
                moveSource = MoveSource(file),
                moveTarget = KotlinDirectoryMoveTarget(newFqName, file.containingDirectory!!),
                delegate = MoveDeclarationsDelegate.TopLevel
            )
        )

        val declarationUsages = project.runSynchronouslyWithProgress(RefactoringBundle.message("progress.text"), true) {
            runReadAction {
                declarationProcessor.findUsages().toList()
            }
        } ?: return
        val changeInfo = ContainerChangeInfo(ContainerInfo.Package(currentFqName), ContainerInfo.Package(newFqName))
        val internalUsages = file.getInternalReferencesToUpdateOnPackageNameChange(changeInfo)

        project.executeWriteCommand(KotlinBundle.message("text.change.file.package.to.0", newFqName)) {
            packageDirective.fqName = newFqName.quoteIfNeeded()
            postProcessMoveUsages(internalUsages)
            performDelayedRefactoringRequests(project)
            declarationProcessor.execute(declarationUsages)
        }
    }
}
