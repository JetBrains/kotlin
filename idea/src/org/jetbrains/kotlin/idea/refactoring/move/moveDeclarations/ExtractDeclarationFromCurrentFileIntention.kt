/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.codeInsight.actions.OptimizeImportsProcessor
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.TextRange
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.util.CommonRefactoringUtil
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.intentions.SelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui.MoveKotlinTopLevelDeclarationsDialog
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.source.getPsi

private const val TIMEOUT_FOR_IMPORT_OPTIMIZING_MS: Long = 700L

class ExtractDeclarationFromCurrentFileIntention :
    SelfTargetingRangeIntention<KtClassOrObject>(KtClassOrObject::class.java, "Extract declaration from current file"),
    LowPriorityAction {

    private fun KtClassOrObject.tryGetExtraClassesToMove(): List<KtNamedDeclaration>? {

        val descriptor = resolveToDescriptorIfAny() ?: return null
        if (descriptor.getSuperClassNotAny()?.modality == Modality.SEALED) return null

        return descriptor.sealedSubclasses
            .mapNotNull { it.source.getPsi() as? KtNamedDeclaration }
            .filterNot { isAncestor(it) }
    }

    override fun applicabilityRange(element: KtClassOrObject): TextRange? {
        if (element.name == null) return null
        if (element.parent !is KtFile) return null
        if (element.hasModifier(KtTokens.PRIVATE_KEYWORD)) return null
        if (element.containingKtFile.declarations.size == 1) return null

        val extraClassesToMove = element.tryGetExtraClassesToMove() ?: return null

        val startOffset = when (element) {
            is KtClass -> element.startOffset
            is KtObjectDeclaration -> element.getObjectKeyword()?.startOffset
            else -> return null
        } ?: return null

        val endOffset = element.nameIdentifier?.endOffset ?: return null

        text = if (extraClassesToMove.isNotEmpty()) "Extract '${element.name}' and subclasses from current file"
        else "Extract '${element.name}' from current file"

        return TextRange(startOffset, endOffset)
    }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtClassOrObject, editor: Editor?) {
        if (editor == null) throw IllegalArgumentException("This intention requires an editor")
        val file = element.containingKtFile
        val project = file.project
        val originalOffset = editor.caretModel.offset - element.startOffset
        val directory = file.containingDirectory ?: return
        val packageName = file.packageFqName
        val targetFileName = "${element.name}.kt"
        val targetFile = directory.findFile(targetFileName)
        if (targetFile != null) {
            if (ApplicationManager.getApplication().isUnitTestMode) {
                throw CommonRefactoringUtil.RefactoringErrorHintException("File $targetFileName already exists")
            }

            // If automatic move is not possible, fall back to full-fledged Move Declarations refactoring
            ApplicationManager.getApplication().invokeLater {
                MoveKotlinTopLevelDeclarationsDialog(
                    project,
                    setOf(element),
                    packageName.asString(),
                    directory,
                    targetFile as? KtFile,
                    true,
                    true,
                    true,
                    true,
                    MoveCallback {
                        runBlocking {
                            withTimeoutOrNull(TIMEOUT_FOR_IMPORT_OPTIMIZING_MS) {
                                OptimizeImportsProcessor(
                                    project,
                                    file
                                ).run()
                            }
                        }
                    }
                ).show()
            }
            return
        }
        val moveTarget = KotlinMoveTargetForDeferredFile(packageName, directory, null) {
            createKotlinFile(targetFileName, directory, packageName.asString())
        }

        val moveSource = element.tryGetExtraClassesToMove()
            ?.let { additionalElements ->
                MoveSource(additionalElements.toMutableList().also { it.add(0, element) })
            }
            ?: MoveSource(element)

        val descriptor = MoveDeclarationsDescriptor(
            project = project,
            moveSource = moveSource,
            moveTarget = moveTarget,
            delegate = MoveDeclarationsDelegate.TopLevel,
            searchInCommentsAndStrings = false,
            searchInNonCode = false,
            moveCallback = MoveCallback {
                val newFile = directory.findFile(targetFileName) as KtFile
                val newDeclaration = newFile.declarations.first()
                NavigationUtil.activateFileWithPsiElement(newFile)
                FileEditorManager.getInstance(project).selectedTextEditor?.moveCaret(newDeclaration.startOffset + originalOffset)
                runBlocking { withTimeoutOrNull(TIMEOUT_FOR_IMPORT_OPTIMIZING_MS) { OptimizeImportsProcessor(project, file).run() } }
            }
        )

        MoveKotlinDeclarationsProcessor(descriptor).run()
    }
}