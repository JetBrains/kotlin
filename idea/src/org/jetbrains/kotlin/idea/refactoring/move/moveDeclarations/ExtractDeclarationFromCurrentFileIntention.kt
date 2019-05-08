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
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.source.getPsi

private const val TIMEOUT_FOR_IMPORT_OPTIMIZING_MS: Long = 700L

class ExtractDeclarationFromCurrentFileIntention :
        SelfTargetingRangeIntention<KtClassOrObject>(KtClassOrObject::class.java, "Extract declaration from current file"),
        LowPriorityAction {
    override fun applicabilityRange(element: KtClassOrObject): TextRange? {
        if (element.name == null) return null
        if (element.parent !is KtFile) return null
        if (element.hasModifier(KtTokens.PRIVATE_KEYWORD)) return null
        if (element.containingKtFile.declarations.size == 1) return null

        val descriptor = element.resolveToDescriptorIfAny() ?: return null
        if (descriptor.sealedSubclasses
                .mapNotNull { it.source.getPsi() }
                .any { !element.isAncestor(it) }
        ) return null
        if (descriptor.getSuperClassNotAny()?.modality == Modality.SEALED) return null

        val keyword = when (element) {
            is KtClass -> element.getClassOrInterfaceKeyword()
            is KtObjectDeclaration -> element.getObjectKeyword()
            else -> return null
        }
        val startOffset = keyword?.startOffset ?: return null
        val endOffset = element.nameIdentifier?.endOffset ?: return null

        text = "Extract '${element.name}' from current file"

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
                        MoveCallback {
                            runBlocking { withTimeoutOrNull(TIMEOUT_FOR_IMPORT_OPTIMIZING_MS) { OptimizeImportsProcessor(project, file).run() } }
                        }
                ).show()
            }
            return
        }
        val moveTarget = KotlinMoveTargetForDeferredFile(packageName, directory, null) {
            createKotlinFile(targetFileName, directory, packageName.asString())
        }
        val descriptor = MoveDeclarationsDescriptor(
                project = project,
                moveSource = MoveSource(element),
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