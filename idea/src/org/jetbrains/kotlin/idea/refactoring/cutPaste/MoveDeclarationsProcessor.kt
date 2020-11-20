/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.cutPaste

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.refactoring.RefactoringBundle
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.runRefactoringAndKeepDelayedRequests
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.core.util.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.refactoring.cutPaste.MoveDeclarationsTransferableData.Companion.STUB_RENDERER
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.getSourceRoot
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class MoveDeclarationsProcessor(
    val project: Project,
    private val sourceContainer: KtDeclarationContainer,
    private val targetPsiFile: KtFile,
    val pastedDeclarations: List<KtNamedDeclaration>,
    private val imports: List<String>,
    private val sourceDeclarationsText: List<String>
) {
    companion object {
        fun build(editor: Editor, cookie: MoveDeclarationsEditorCookie): MoveDeclarationsProcessor? {
            val data = cookie.data
            val project = editor.project ?: return null
            val range = cookie.bounds.range ?: return null

            val sourceFileUrl = data.sourceFileUrl
            val sourceFile = VirtualFileManager.getInstance().findFileByUrl(sourceFileUrl) ?: return null
            if (sourceFile.getSourceRoot(project) == null) return null

            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            psiDocumentManager.commitAllDocuments()

            val targetPsiFile = psiDocumentManager.getPsiFile(editor.document) as? KtFile ?: return null
            if (targetPsiFile.virtualFile.getSourceRoot(project) == null) return null
            val sourcePsiFile = PsiManager.getInstance(project).findFile(sourceFile) as? KtFile ?: return null

            val sourceObject = data.sourceObjectFqName?.let { fqName ->
                sourcePsiFile.findDescendantOfType<KtObjectDeclaration> { it.fqName?.asString() == fqName } ?: return null
            }
            val sourceContainer: KtDeclarationContainer = sourceObject ?: sourcePsiFile

            if (targetPsiFile == sourceContainer) return null

            val declarations = MoveDeclarationsCopyPasteProcessor.rangeToDeclarations(targetPsiFile, range.startOffset, range.endOffset)
            if (declarations.isEmpty() || declarations.any { it.parent !is KtFile }) return null

            if (sourceContainer == sourcePsiFile && sourcePsiFile.packageFqName == targetPsiFile.packageFqName) return null

            // check that declarations were cut (not copied)
            if (sourceContainer.declarations.any { declaration -> declaration.text in data.declarationTexts }) {
                return null
            }

            return MoveDeclarationsProcessor(
                project,
                sourceContainer,
                targetPsiFile,
                declarations,
                data.imports,
                data.declarationTexts
            )
        }
    }

    private val sourcePsiFile = (sourceContainer as KtElement).containingKtFile
    private val psiDocumentManager = PsiDocumentManager.getInstance(project)
    private val sourceDocument = psiDocumentManager.getDocument(sourcePsiFile)!!

    fun performRefactoring() {
        psiDocumentManager.commitAllDocuments()

        val commandName = KotlinBundle.message("action.usage.update.text")
        val commandGroupId = Any() // we need to group both commands for undo

        // temporary revert imports to the state before they have been changed
        val importsSubstitution = if (sourcePsiFile.importDirectives.size != imports.size) {
            val startOffset = sourcePsiFile.importDirectives.minOfOrNull { it.startOffset } ?: 0
            val endOffset = sourcePsiFile.importDirectives.minOfOrNull { it.endOffset } ?: 0
            val importsDeclarationsText = sourceDocument.getText(TextRange(startOffset, endOffset))

            val tempImportsText = imports.joinToString(separator = "\n")
            project.executeWriteCommand(commandName, commandGroupId) {
                sourceDocument.deleteString(startOffset, endOffset)
                sourceDocument.insertString(startOffset, tempImportsText)
            }
            psiDocumentManager.commitDocument(sourceDocument)

            ImportsSubstitution(importsDeclarationsText, tempImportsText, startOffset)
        } else {
            null
        }

        val tmpRangeAndDeclarations = insertStubDeclarations(commandName, commandGroupId, sourceDeclarationsText)
        assert(tmpRangeAndDeclarations.second.size == pastedDeclarations.size)

        val stubTexts = tmpRangeAndDeclarations.second.map { STUB_RENDERER.render(it.unsafeResolveToDescriptor()) }

        project.executeWriteCommand(commandName, commandGroupId) {
            sourceDocument.deleteString(tmpRangeAndDeclarations.first.startOffset, tmpRangeAndDeclarations.first.endOffset)
        }
        psiDocumentManager.commitDocument(sourceDocument)

        val stubRangeAndDeclarations = insertStubDeclarations(commandName, commandGroupId, stubTexts)
        val stubDeclarations = stubRangeAndDeclarations.second
        assert(stubDeclarations.size == pastedDeclarations.size)

        importsSubstitution?.let {
            project.executeWriteCommand(commandName, commandGroupId) {
                sourceDocument.deleteString(it.startOffset, it.startOffset + it.tempImportsText.length)
                sourceDocument.insertString(it.startOffset, it.originalImportsText)
            }
            psiDocumentManager.commitDocument(sourceDocument)
        }

        val mover = object : Mover {
            override fun invoke(declaration: KtNamedDeclaration, targetContainer: KtElement): KtNamedDeclaration {
                val index = stubDeclarations.indexOf(declaration)
                assert(index >= 0)
                declaration.delete()
                return pastedDeclarations[index]
            }
        }

        val declarationProcessor = MoveKotlinDeclarationsProcessor(
            MoveDeclarationsDescriptor(
                moveSource = MoveSource(stubDeclarations),
                moveTarget = KotlinMoveTargetForExistingElement(targetPsiFile),
                delegate = MoveDeclarationsDelegate.TopLevel,
                project = project
            ),
            mover
        )

        val declarationUsages = project.runSynchronouslyWithProgress(RefactoringBundle.message("progress.text"), true) {
            runReadAction {
                declarationProcessor.findUsages().toList()
            }
        } ?: return

        project.executeWriteCommand(commandName, commandGroupId) {
            project.runRefactoringAndKeepDelayedRequests { declarationProcessor.execute(declarationUsages) }

            psiDocumentManager.doPostponedOperationsAndUnblockDocument(sourceDocument)
            val insertedStubRange = stubRangeAndDeclarations.first
            assert(insertedStubRange.isValid)
            sourceDocument.deleteString(insertedStubRange.startOffset, insertedStubRange.endOffset)
        }
    }

    private data class ImportsSubstitution(val originalImportsText: String, val tempImportsText: String, val startOffset: Int)

    private fun insertStubDeclarations(
        commandName: String,
        commandGroupId: Any?,
        values: List<String>
    ): Pair<RangeMarker, List<KtNamedDeclaration>> {
        val insertedRange = project.executeWriteCommand(commandName, commandGroupId) {
            val insertionOffset = sourceContainer.declarations.firstOrNull()?.startOffset
                ?: when (sourceContainer) {
                    is KtFile -> sourceContainer.textLength
                    is KtObjectDeclaration -> sourceContainer.getBody()?.rBrace?.startOffset ?: sourceContainer.endOffset
                    else -> error("Unknown sourceContainer: $sourceContainer")
                }
            val textToInsert = "\n//start\n\n${values.joinToString(separator = "\n")}\n//end\n"
            sourceDocument.insertString(insertionOffset, textToInsert)
            sourceDocument.createRangeMarker(TextRange(insertionOffset, insertionOffset + textToInsert.length))
        }
        psiDocumentManager.commitDocument(sourceDocument)

        val declarations =
            MoveDeclarationsCopyPasteProcessor.rangeToDeclarations(
                sourcePsiFile,
                insertedRange.startOffset,
                insertedRange.endOffset
            )

        return Pair(insertedRange, declarations)
    }

}