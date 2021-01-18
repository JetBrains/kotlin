/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.util.CommonRefactoringUtil
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui.MoveKotlinTopLevelDeclarationsDialog
import org.jetbrains.kotlin.idea.refactoring.showWithTransaction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.source.getPsi

private const val TIMEOUT_FOR_IMPORT_OPTIMIZING_MS: Long = 700L

class ExtractDeclarationFromCurrentFileIntention : SelfTargetingRangeIntention<KtClassOrObject>(
    KtClassOrObject::class.java,
    KotlinBundle.lazyMessage("intention.extract.declarations.from.file.text")
), LowPriorityAction {
    private fun KtClassOrObject.tryGetExtraClassesToMove(): List<KtNamedDeclaration>? {

        val descriptor = resolveToDescriptorIfAny() ?: return null
        if (descriptor.getSuperClassNotAny()?.modality == Modality.SEALED) return null

        return descriptor.sealedSubclasses
            .mapNotNull { it.source.getPsi() as? KtNamedDeclaration }
            .filterNot { isAncestor(it) }
    }

    override fun applicabilityRange(element: KtClassOrObject): TextRange? {
        element.name ?: return null
        if (element.parent !is KtFile) return null
        if (element.hasModifier(KtTokens.PRIVATE_KEYWORD)) return null
        if (element.containingKtFile.run { declarations.size == 1 || containingDirectory === null }) return null
        val extraClassesToMove = element.tryGetExtraClassesToMove() ?: return null

        val startOffset = when (element) {
            is KtClass -> element.startOffset
            is KtObjectDeclaration -> element.getObjectKeyword()?.startOffset
            else -> return null
        } ?: return null

        val endOffset = element.nameIdentifier?.endOffset ?: return null

        setTextGetter(
            KotlinBundle.lazyMessage(
                "intention.extract.declarations.from.file.text.details",
                element.name.toString(),
                extraClassesToMove.size
            )
        )

        return TextRange(startOffset, endOffset)
    }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtClassOrObject, editor: Editor?) {
        requireNotNull(editor) { "This intention requires an editor" }
        val file = element.containingKtFile
        val project = file.project
        val originalOffset = editor.caretModel.offset - element.startOffset
        val directory = file.containingDirectory ?: return
        val packageName = file.packageFqName
        val targetFileName = "${element.name}.kt"
        val targetFile = directory.findFile(targetFileName)

        if (targetFile !== null) {
            if (ApplicationManager.getApplication().isUnitTestMode) {
                throw CommonRefactoringUtil.RefactoringErrorHintException(RefactoringBundle.message("file.already.exist", targetFileName))
            }
            // If automatic move is not possible, fall back to full-fledged Move Declarations refactoring
            runFullFledgedMoveRefactoring(project, element, packageName, directory, targetFile, file)
            return
        }

        val moveTarget = KotlinMoveTargetForDeferredFile(packageName, directory, targetFile = null) {
            createKotlinFile(targetFileName, directory, packageName.asString())
        }

        val moveSource = element.tryGetExtraClassesToMove()
            ?.let { additionalElements ->
                MoveSource(additionalElements.toMutableList().also { it.add(0, element) })
            }
            ?: MoveSource(element)

        val moveCallBack = MoveCallback {
            val newFile = directory.findFile(targetFileName) as KtFile
            val newDeclaration = newFile.declarations.first()
            NavigationUtil.activateFileWithPsiElement(newFile)
            FileEditorManager.getInstance(project).selectedTextEditor?.moveCaret(newDeclaration.startOffset + originalOffset)
            runBlocking { withTimeoutOrNull(TIMEOUT_FOR_IMPORT_OPTIMIZING_MS) { OptimizeImportsProcessor(project, file).run() } }
        }

        val descriptor = MoveDeclarationsDescriptor(
            project,
            moveSource,
            moveTarget,
            MoveDeclarationsDelegate.TopLevel,
            searchInCommentsAndStrings = false,
            searchInNonCode = false,
            moveCallback = moveCallBack
        )

        MoveKotlinDeclarationsProcessor(descriptor).run()
    }

    private fun runFullFledgedMoveRefactoring(
        project: Project,
        element: KtClassOrObject,
        packageName: FqName,
        directory: PsiDirectory,
        targetFile: PsiFile?,
        file: KtFile
    ) {
        ApplicationManager.getApplication().invokeLater {

            val callBack = MoveCallback {
                runBlocking {
                    withTimeoutOrNull(TIMEOUT_FOR_IMPORT_OPTIMIZING_MS) {
                        OptimizeImportsProcessor(project, file).run()
                    }
                }
            }

            MoveKotlinTopLevelDeclarationsDialog(
                project,
                setOf(element),
                packageName.asString(),
                directory,
                targetFile as? KtFile,
                /* moveToPackage = */ true,
                /* searchInComments = */ true,
                /* searchForTextOccurrences = */ true,
                /* deleteEmptySourceFiles = */ true,
                /* moveMppDeclarations = */ false,
                callBack
            ).showWithTransaction()
        }
    }
}