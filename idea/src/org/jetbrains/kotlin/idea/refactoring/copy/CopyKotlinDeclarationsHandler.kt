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

package org.jetbrains.kotlin.idea.refactoring.copy

import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesDialog
import com.intellij.refactoring.copy.CopyHandlerDelegateBase
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.idea.refactoring.move.*
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.ifEmpty

class CopyKotlinDeclarationsHandler : CopyHandlerDelegateBase() {
    companion object {
        @set:TestOnly
        var Project.newName: String? by UserDataProperty(Key.create("NEW_NAME"))

        private fun PsiElement.getElementsToCopy(): List<PsiNamedElement> {
            val declarationOrFile = parentsWithSelf.firstOrNull { it is KtFile || (it is KtNamedDeclaration && it.parent is KtFile) }
            return when (declarationOrFile) {
                is KtFile -> declarationOrFile.declarations.filterIsInstance<KtNamedDeclaration>().ifEmpty { listOf(declarationOrFile) }
                is KtNamedDeclaration -> listOf(declarationOrFile)
                else -> emptyList()
            }
        }
    }

    override fun canCopy(elements: Array<out PsiElement>, fromUpdate: Boolean): Boolean {
        return elements.flatMap { it.getElementsToCopy().ifEmpty { return false } }.distinctBy { it.containingFile }.size == 1
    }

    enum class ExistingFilePolicy {
        APPEND, OVERWRITE, SKIP
    }

    private fun getOrCreateTargetFile(
            originalFile: KtFile,
            targetDirectory: PsiDirectory,
            targetFileName: String,
            commandName: String
    ): KtFile? {
        val existingFile = targetDirectory.findFile(targetFileName)
        if (existingFile == originalFile) return null
        if (existingFile != null) {
            val policy = getFilePolicy(existingFile, targetFileName, targetDirectory, commandName)
            when (policy) {
                ExistingFilePolicy.APPEND -> {}
                ExistingFilePolicy.OVERWRITE -> runWriteAction { existingFile.delete() }
                ExistingFilePolicy.SKIP -> return null
            }
        }
        return runWriteAction {
            if (existingFile != null && existingFile.isValid) {
                existingFile as KtFile
            } else {
                createKotlinFile(targetFileName, targetDirectory)
            }
        }
    }

    private fun getFilePolicy(
            existingFile: PsiFile?,
            targetFileName: String,
            targetDirectory: PsiDirectory,
            commandName: String
    ): ExistingFilePolicy {
        val isUnitTestMode = ApplicationManager.getApplication().isUnitTestMode
        return if (existingFile !is KtFile) {
            if (isUnitTestMode) return ExistingFilePolicy.OVERWRITE

            val answer = Messages.showOkCancelDialog(
                    "File $targetFileName already exists in ${targetDirectory.virtualFile.path}",
                    commandName,
                    "Overwrite",
                    "Cancel",
                    Messages.getQuestionIcon()
            )
            if (answer == Messages.OK) ExistingFilePolicy.OVERWRITE else ExistingFilePolicy.SKIP
        }
        else {
            if (isUnitTestMode) return ExistingFilePolicy.APPEND

            val answer = Messages.showYesNoCancelDialog(
                    "File $targetFileName already exists in ${targetDirectory.virtualFile.path}",
                    commandName,
                    "Append",
                    "Overwrite",
                    "Cancel",
                    Messages.getQuestionIcon()
            )
            when (answer) {
                Messages.YES -> ExistingFilePolicy.APPEND
                Messages.NO -> ExistingFilePolicy.OVERWRITE
                else -> ExistingFilePolicy.SKIP
            }
        }
    }

    override fun doCopy(elements: Array<out PsiElement>, defaultTargetDirectory: PsiDirectory?) {
        val elementsToCopy = elements.flatMap { it.getElementsToCopy() }
        if (elementsToCopy.isEmpty()) return

        val singleElementToCopy = elementsToCopy.singleOrNull()

        val originalFile = elementsToCopy.first().containingFile as KtFile
        val initialTargetDirectory = defaultTargetDirectory ?: originalFile.containingDirectory ?: return

        val project = initialTargetDirectory.project

        if (ProjectRootManager.getInstance(project).fileIndex.getSourceRootForFile(initialTargetDirectory.virtualFile) == null) return

        val commandName = "Copy Declarations"

        var openInEditor = false
        var newName: String? = singleElementToCopy?.name ?: originalFile.name
        var targetDirWrapper: AutocreatingPsiDirectoryWrapper = initialTargetDirectory.toDirectoryWrapper()

        if (!ApplicationManager.getApplication().isUnitTestMode) {
            if (singleElementToCopy != null && singleElementToCopy is KtNamedDeclaration) {
                val dialog = CopyKotlinDeclarationDialog(singleElementToCopy, initialTargetDirectory, project)
                dialog.title = commandName
                if (!dialog.showAndGet()) return

                openInEditor = dialog.openInEditor
                newName = dialog.newName ?: singleElementToCopy.name
                targetDirWrapper = dialog.targetDirectory?.toDirectoryWrapper() ?: return
            }
            else {
                val dialog = CopyFilesOrDirectoriesDialog(arrayOf(originalFile), initialTargetDirectory, project, false)
                if (!dialog.showAndGet()) return
                openInEditor = dialog.openInEditor()
                newName = dialog.newName
                targetDirWrapper = dialog.targetDirectory?.toDirectoryWrapper() ?: return
            }
        }
        else {
            project.newName?.let { newName = it }
        }

        if (singleElementToCopy != null && newName.isNullOrEmpty()) return

        val internalUsages = runReadAction {
            val targetPackageName = targetDirWrapper.getPackageName()
            val changeInfo = ContainerChangeInfo(
                    ContainerInfo.Package(originalFile.packageFqName),
                    ContainerInfo.Package(FqName(targetPackageName))
            )
            elementsToCopy.flatMap { elementToCopy ->
                (elementToCopy as KtElement).getInternalReferencesToUpdateOnPackageNameChange(changeInfo).filter {
                    val referencedElement = (it as? MoveRenameUsageInfo)?.referencedElement
                    referencedElement == null || !elementToCopy.isAncestor(referencedElement)
                }
            }
        }
        markInternalUsages(internalUsages)

        val restoredInternalUsages = ArrayList<UsageInfo>()

        project.executeCommand(commandName) {
            try {
                val targetDirectory = runWriteAction { targetDirWrapper.getOrCreateDirectory(initialTargetDirectory) }
                val targetFileName = if (newName?.contains(".") ?: false) newName!! else newName + "." + originalFile.virtualFile.extension

                val oldToNewElementsMapping = HashMap<PsiElement, PsiElement>()

                val targetFile: KtFile
                if (singleElementToCopy is KtFile) {
                    targetFile = runWriteAction { targetDirectory.copyFileFrom(targetFileName, singleElementToCopy) as KtFile }
                }
                else {
                    targetFile = getOrCreateTargetFile(originalFile, targetDirectory, targetFileName, commandName) ?: return@executeCommand
                    runWriteAction {
                        val newElements = elementsToCopy.map { targetFile.add(it.copy()) as KtNamedDeclaration }
                        elementsToCopy.zip(newElements).toMap(oldToNewElementsMapping)
                    }
                }

                runWriteAction {
                    for (newElement in oldToNewElementsMapping.values) {
                        restoredInternalUsages += restoreInternalUsages(newElement as KtElement, oldToNewElementsMapping, true)
                        postProcessMoveUsages(restoredInternalUsages, oldToNewElementsMapping)
                    }

                    performDelayedRefactoringRequests(project)
                }

                oldToNewElementsMapping.values.singleOrNull()?.let {
                    RenameProcessor(project, it, newName!!.quoteIfNeeded(), false, false).run()
                }

                if (openInEditor) {
                    EditorHelper.openFilesInEditor(arrayOf(targetFile))
                }
            }
            catch (e: IncorrectOperationException) {
                Messages.showMessageDialog(project, e.message, RefactoringBundle.message("error.title"), Messages.getErrorIcon())
            }
            finally {
                cleanUpInternalUsages(internalUsages + restoredInternalUsages)
            }
        }
    }

    override fun doClone(element: PsiElement) {

    }
}