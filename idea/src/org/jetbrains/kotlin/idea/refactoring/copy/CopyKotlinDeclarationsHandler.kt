/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.copy

import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesDialog
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesHandler
import com.intellij.refactoring.copy.CopyHandlerDelegateBase
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.MultiMap
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.core.getFqNameWithImplicitPrefix
import org.jetbrains.kotlin.idea.core.packageMatchesDirectoryOrImplicit
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.idea.refactoring.move.*
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.KotlinDirectoryMoveTarget
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.MoveConflictChecker
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.sourceRoot
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

        private fun PsiElement.getCopyableElement() =
            parentsWithSelf.firstOrNull { it is KtFile || (it is KtNamedDeclaration && it.parent is KtFile) } as? KtElement

        private fun PsiElement.getDeclarationsToCopy(): List<KtElement> = when (val declarationOrFile = getCopyableElement()) {
            is KtFile -> declarationOrFile.declarations.filterIsInstance<KtNamedDeclaration>().ifEmpty { listOf(declarationOrFile) }
            is KtNamedDeclaration -> listOf(declarationOrFile)
            else -> emptyList()
        }
    }

    private val copyFilesHandler by lazy { CopyFilesOrDirectoriesHandler() }

    private fun getSourceFiles(elements: Array<out PsiElement>): Array<PsiElement>? {
        return elements
            .map { it.containingFile ?: it as? PsiFileSystemItem ?: return null }
            .toTypedArray()
    }

    private fun canCopyFiles(elements: Array<out PsiElement>, fromUpdate: Boolean): Boolean {
        val sourceFiles = getSourceFiles(elements) ?: return false
        if (!sourceFiles.any { it is KtFile }) return false
        return copyFilesHandler.canCopy(sourceFiles, fromUpdate)
    }

    private fun canCopyDeclarations(elements: Array<out PsiElement>): Boolean {
        val containingFile =
            elements
                .flatMap { it.getDeclarationsToCopy().ifEmpty { return false } }
                .distinctBy { it.containingFile }
                .singleOrNull()
                ?.containingFile ?: return false
        return containingFile.sourceRoot != null
    }

    override fun canCopy(elements: Array<out PsiElement>, fromUpdate: Boolean): Boolean {
        return canCopyDeclarations(elements) || canCopyFiles(elements, fromUpdate)
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
        if (existingFile != null) when (getFilePolicy(existingFile, targetFileName, targetDirectory, commandName)) {
            ExistingFilePolicy.APPEND -> {
            }
            ExistingFilePolicy.OVERWRITE -> runWriteAction { existingFile.delete() }
            ExistingFilePolicy.SKIP -> return null
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
        } else {
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
        if (!canCopyDeclarations(elements)) {
            val sourceFiles = getSourceFiles(elements) ?: return
            return copyFilesHandler.doCopy(sourceFiles, defaultTargetDirectory)
        }

        val elementsToCopy = elements.mapNotNull { it.getCopyableElement() }
        if (elementsToCopy.isEmpty()) return

        val singleElementToCopy = elementsToCopy.singleOrNull()

        val originalFile = elementsToCopy.first().containingFile as KtFile
        val initialTargetDirectory = defaultTargetDirectory ?: originalFile.containingDirectory ?: return

        val isSingleDeclarationInFile =
            singleElementToCopy is KtNamedDeclaration && originalFile.declarations.singleOrNull() == singleElementToCopy

        val project = initialTargetDirectory.project

        val commandName = "Copy Declarations"

        var openInEditor = false
        var newName: String? = singleElementToCopy?.name ?: originalFile.name
        var targetDirWrapper: AutocreatingPsiDirectoryWrapper = initialTargetDirectory.toDirectoryWrapper()
        var targetSourceRoot: VirtualFile? = initialTargetDirectory.sourceRoot ?: return

        val isUnitTestMode = ApplicationManager.getApplication().isUnitTestMode

        if (!isUnitTestMode) {
            if (singleElementToCopy != null && singleElementToCopy is KtNamedDeclaration) {
                val dialog = CopyKotlinDeclarationDialog(singleElementToCopy, initialTargetDirectory, project)
                dialog.title = commandName
                if (!dialog.showAndGet()) return

                openInEditor = dialog.openInEditor
                newName = dialog.newName ?: singleElementToCopy.name
                targetDirWrapper = dialog.targetDirectory?.toDirectoryWrapper() ?: return
                targetSourceRoot = dialog.targetSourceRoot
            } else {
                val dialog = CopyFilesOrDirectoriesDialog(arrayOf(originalFile), initialTargetDirectory, project, false)
                if (!dialog.showAndGet()) return
                openInEditor = dialog.openInEditor()
                newName = dialog.newName
                targetDirWrapper = dialog.targetDirectory?.toDirectoryWrapper() ?: return
                targetSourceRoot = dialog.targetDirectory?.sourceRoot
            }
        } else {
            project.newName?.let { newName = it }
        }

        if (singleElementToCopy != null && newName.isNullOrEmpty()) return

        val internalUsages = runReadAction {
            val targetPackageName = targetDirWrapper.getPackageName()
            val changeInfo = ContainerChangeInfo(
                ContainerInfo.Package(originalFile.packageFqName),
                ContainerInfo.Package(FqName(targetPackageName))
            )
            elementsToCopy.flatMapTo(LinkedHashSet()) { elementToCopy ->
                elementToCopy.getInternalReferencesToUpdateOnPackageNameChange(changeInfo).filter {
                    val referencedElement = (it as? MoveRenameUsageInfo)?.referencedElement
                    referencedElement == null || !elementToCopy.isAncestor(referencedElement)
                }
            }
        }
        markInternalUsages(internalUsages)

        fun doRefactor() {
            val restoredInternalUsages = ArrayList<UsageInfo>()

            project.executeCommand(commandName) {
                try {
                    val targetDirectory = runWriteAction { targetDirWrapper.getOrCreateDirectory(initialTargetDirectory) }
                    val targetFileName =
                        if (newName?.contains(".") == true) newName!! else newName + "." + originalFile.virtualFile.extension

                    val oldToNewElementsMapping = HashMap<PsiElement, PsiElement>()

                    val fileToCopy = when {
                        singleElementToCopy is KtFile -> singleElementToCopy
                        isSingleDeclarationInFile -> originalFile
                        else -> null
                    }

                    val targetFile: PsiFile
                    val copiedDeclaration: KtNamedDeclaration?
                    if (fileToCopy != null) {
                        targetFile = runWriteAction {
                            // implicit package prefix may change after copy
                            val targetDirectoryFqName = targetDirectory.getFqNameWithImplicitPrefix()
                            val copiedFile = targetDirectory.copyFileFrom(targetFileName, fileToCopy)
                            if (copiedFile is KtFile && fileToCopy.packageMatchesDirectoryOrImplicit()) {
                                targetDirectoryFqName?.let { copiedFile.packageFqName = it }
                            }
                            performDelayedRefactoringRequests(project)
                            copiedFile
                        }
                        copiedDeclaration = if (isSingleDeclarationInFile && targetFile is KtFile) {
                            targetFile.declarations.singleOrNull() as? KtNamedDeclaration
                        } else null
                    } else {
                        targetFile =
                            getOrCreateTargetFile(originalFile, targetDirectory, targetFileName, commandName) ?: return@executeCommand
                        runWriteAction {
                            val newElements = elementsToCopy.map { targetFile.add(it.copy()) as KtNamedDeclaration }
                            elementsToCopy.zip(newElements).toMap(oldToNewElementsMapping)
                            oldToNewElementsMapping[originalFile] = targetFile

                            for (newElement in oldToNewElementsMapping.values) {
                                restoredInternalUsages += restoreInternalUsages(newElement as KtElement, oldToNewElementsMapping, true)
                                postProcessMoveUsages(restoredInternalUsages, oldToNewElementsMapping)
                            }

                            performDelayedRefactoringRequests(project)
                        }
                        copiedDeclaration = oldToNewElementsMapping.values.filterIsInstance<KtNamedDeclaration>().singleOrNull()
                    }

                    copiedDeclaration?.let { newDeclaration ->
                        if (newName == newDeclaration.name) return@let
                        val selfReferences = ReferencesSearch.search(newDeclaration, LocalSearchScope(newDeclaration)).findAll()
                        runWriteAction {
                            selfReferences.forEach { it.handleElementRename(newName!!) }
                            newDeclaration.setName(newName!!)
                        }
                    }

                    if (openInEditor) {
                        EditorHelper.openFilesInEditor(arrayOf(targetFile))
                    }
                } catch (e: IncorrectOperationException) {
                    Messages.showMessageDialog(project, e.message, RefactoringBundle.message("error.title"), Messages.getErrorIcon())
                } finally {
                    cleanUpInternalUsages(internalUsages + restoredInternalUsages)
                }
            }
        }

        val conflicts = MultiMap<PsiElement, String>()

        if (!(isUnitTestMode && BaseRefactoringProcessor.ConflictsInTestsException.isTestIgnore())) {
            val targetSourceRootPsi = targetSourceRoot?.toPsiDirectory(project)
            if (targetSourceRootPsi != null && project == originalFile.project) {
                val conflictChecker = MoveConflictChecker(
                    project,
                    elementsToCopy,
                    KotlinDirectoryMoveTarget(FqName.ROOT, targetSourceRootPsi),
                    originalFile
                )
                conflictChecker.checkModuleConflictsInDeclarations(internalUsages, conflicts)
                conflictChecker.checkVisibilityInDeclarations(conflicts)
            }
        }

        project.checkConflictsInteractively(conflicts, onAccept = ::doRefactor)
    }

    override fun doClone(element: PsiElement) {

    }
}