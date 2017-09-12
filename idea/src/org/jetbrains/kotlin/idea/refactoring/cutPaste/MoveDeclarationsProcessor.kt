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

package org.jetbrains.kotlin.idea.refactoring.cutPaste

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.runRefactoringAndKeepDelayedRequests
import org.jetbrains.kotlin.idea.conversion.copy.range
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
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
        private val stubTexts: List<String>
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
            val filteredDeclarations = sourceContainer.declarations.filter { it.name in data.declarationNames }
            val stubs = data.stubTexts.toSet()
            if (filteredDeclarations.any { MoveDeclarationsTransferableData.STUB_RENDERER.render(it.unsafeResolveToDescriptor()) in stubs }) return null

            return MoveDeclarationsProcessor(project, sourceContainer, targetPsiFile, declarations, data.stubTexts)
        }
    }

    private val sourcePsiFile = (sourceContainer as KtElement).containingKtFile
    private val psiDocumentManager = PsiDocumentManager.getInstance(project)
    private val sourceDocument = psiDocumentManager.getDocument(sourcePsiFile)!!

    fun performRefactoring() {
        psiDocumentManager.commitAllDocuments()

        val commandName = "Usage update"
        val commandGroupId = Any() // we need to group both commands for undo

        val insertedRange = project.executeWriteCommand<RangeMarker>(commandName, commandGroupId) {
            //TODO: can stub declarations interfere with pasted declarations? I could not find such cases
            insertStubDeclarations()
        }
        psiDocumentManager.commitDocument(sourceDocument)

        val stubDeclarations = MoveDeclarationsCopyPasteProcessor.rangeToDeclarations(sourcePsiFile, insertedRange.startOffset, insertedRange.endOffset)
        assert(stubDeclarations.size == pastedDeclarations.size) //TODO: can they ever differ?

        val mover = object: Mover {
            override fun invoke(declaration: KtNamedDeclaration, targetContainer: KtElement): KtNamedDeclaration {
                val index = stubDeclarations.indexOf(declaration)
                assert(index >= 0)
                declaration.delete()
                return pastedDeclarations[index]
            }
        }

        val declarationProcessor = MoveKotlinDeclarationsProcessor(
                MoveDeclarationsDescriptor(
                        elementsToMove = stubDeclarations,
                        moveTarget = KotlinMoveTargetForExistingElement(targetPsiFile),
                        delegate = MoveDeclarationsDelegate.TopLevel,
                        project = project
                ),
                mover
        )

        val declarationUsages = declarationProcessor.findUsages().toList()

        project.executeWriteCommand(commandName, commandGroupId) {
            project.runRefactoringAndKeepDelayedRequests { declarationProcessor.execute(declarationUsages) }

            psiDocumentManager.doPostponedOperationsAndUnblockDocument(sourceDocument)
            assert(insertedRange.isValid)
            sourceDocument.deleteString(insertedRange.startOffset, insertedRange.endOffset)
        }
    }

    private fun insertStubDeclarations(): RangeMarker {
        val insertionOffset = sourceContainer.declarations.firstOrNull()?.startOffset
                              ?: when (sourceContainer) {
                                  is KtFile -> sourceContainer.textLength
                                  is KtObjectDeclaration -> sourceContainer.getBody()?.rBrace?.startOffset ?: sourceContainer.endOffset
                                  else -> error("Unknown sourceContainer: $sourceContainer")
                              }
        val textToInsert = "\n//start\n\n" + stubTexts.joinToString(separator = "\n") + "\n//end\n"
        sourceDocument.insertString(insertionOffset, textToInsert)
        return sourceDocument.createRangeMarker(TextRange(insertionOffset, insertionOffset + textToInsert.length))
    }
}