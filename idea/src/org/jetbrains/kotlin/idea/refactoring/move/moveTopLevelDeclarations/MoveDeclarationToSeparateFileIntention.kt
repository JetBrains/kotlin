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

package org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.navigation.NavigationUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.TextRange
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.idea.core.refactoring.createKotlinFile
import org.jetbrains.kotlin.idea.intentions.JetSelfTargetingRangeIntention
import org.jetbrains.kotlin.idea.quickfix.moveCaret
import org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations.ui.MoveKotlinTopLevelDeclarationsDialog
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

public class MoveDeclarationToSeparateFileIntention :
        JetSelfTargetingRangeIntention<JetClassOrObject>(JetClassOrObject::class.java, "Move declaration to separate file"),
        LowPriorityAction {
    override fun applicabilityRange(element: JetClassOrObject): TextRange? {
        if (element.name == null) return null
        if (element.parent !is JetFile) return null
        if (element.hasModifier(JetTokens.PRIVATE_KEYWORD)) return null
        if (element.getContainingJetFile().declarations.size() == 1) return null

        val keyword = when (element) {
            is JetClass -> element.getClassOrInterfaceKeyword()
            is JetObjectDeclaration -> element.getObjectKeyword()
            else -> return null
        }
        val startOffset = keyword?.startOffset ?: return null
        val endOffset = element.nameIdentifier?.endOffset ?: return null

        text = "Move '${element.name}' to separate file"

        return TextRange(startOffset, endOffset)
    }

    override fun applyTo(element: JetClassOrObject, editor: Editor) {
        val file = element.getContainingJetFile()
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
                MoveKotlinTopLevelDeclarationsDialog(project,
                                                     setOf(element),
                                                     packageName.asString(),
                                                     directory,
                                                     targetFile as? JetFile,
                                                     true,
                                                     true,
                                                     true,
                                                     null).show()
            }
            return
        }
        val moveTarget = DeferredJetFileKotlinMoveTarget(project, packageName) {
            createKotlinFile(targetFileName, directory, packageName.asString())
        }
        val moveOptions = MoveKotlinTopLevelDeclarationsOptions(
                elementsToMove = listOf(element),
                moveTarget = moveTarget,
                searchInCommentsAndStrings = false,
                searchInNonCode = false,
                updateInternalReferences = true,
                moveCallback = MoveCallback {
                    val newFile = directory.findFile(targetFileName) as JetFile
                    val newDeclaration = newFile.declarations.first()
                    NavigationUtil.activateFileWithPsiElement(newFile)
                    FileEditorManager.getInstance(project).selectedTextEditor?.moveCaret(newDeclaration.startOffset + originalOffset)
                }
        )
        MoveKotlinTopLevelDeclarationsProcessor(project, moveOptions).run()
    }
}