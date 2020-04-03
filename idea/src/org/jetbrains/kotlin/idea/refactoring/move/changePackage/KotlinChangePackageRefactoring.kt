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

import com.intellij.refactoring.RefactoringBundle
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.codeInsight.shorten.runRefactoringAndKeepDelayedRequests
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
            project.runRefactoringAndKeepDelayedRequests { declarationProcessor.execute(declarationUsages) }
        }
    }
}
