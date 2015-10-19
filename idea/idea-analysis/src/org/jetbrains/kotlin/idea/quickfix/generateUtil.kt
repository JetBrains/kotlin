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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.ifEmpty

public fun moveCaretIntoGeneratedElement(editor: Editor, element: PsiElement) {
    val project = element.project
    val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(element)

    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

    pointer.element?.let { moveCaretIntoGeneratedElementDocumentUnblocked(editor, it) }
}

private fun moveCaretIntoGeneratedElementDocumentUnblocked(editor: Editor, element: PsiElement): Boolean {
    // Inspired by GenerateMembersUtils.positionCaret()

    if (element is JetDeclarationWithBody && element.hasBody()) {
        val expression = element.getBodyExpression()
        if (expression is JetBlockExpression) {
            val lBrace = expression.getLBrace()
            val rBrace = expression.getRBrace()

            if (lBrace != null && rBrace != null) {
                val firstInBlock = lBrace.siblings(forward = true, withItself = false).first { it !is PsiWhiteSpace }
                val lastInBlock = rBrace.siblings(forward = false, withItself = false).first { it !is PsiWhiteSpace }

                val start = firstInBlock.getTextRange()!!.getStartOffset()
                val end = lastInBlock.getTextRange()!!.getEndOffset()

                editor.moveCaret(Math.min(start, end))

                if (start < end) {
                    editor.getSelectionModel().setSelection(start, end)
                }

                return true
            }
        }
    }

    if (element is JetWithExpressionInitializer && element.hasInitializer()) {
        val expression = element.getInitializer()
        if (expression == null) throw AssertionError()

        val initializerRange = expression.getTextRange()

        val offset = initializerRange?.getStartOffset() ?: element.getTextOffset()

        editor.moveCaret(offset)

        if (initializerRange != null) {
            editor.getSelectionModel().setSelection(initializerRange.getStartOffset(), initializerRange.getEndOffset())
        }

        return true
    }

    if (element is JetProperty) {
        for (accessor in element.getAccessors()) {
            if (moveCaretIntoGeneratedElementDocumentUnblocked(editor, accessor)) {
                return true
            }
        }
    }

    return false
}

public fun Editor.moveCaret(offset: Int, scrollType: ScrollType = ScrollType.RELATIVE) {
    getCaretModel().moveToOffset(offset)
    getScrollingModel().scrollToCaret(scrollType)
}

private fun findInsertAfterAnchor(editor: Editor?, body: JetClassBody): PsiElement? {
    val afterAnchor = body.lBrace ?: return null

    val offset = editor?.caretModel?.offset ?: body.startOffset
    val offsetCursorElement = PsiTreeUtil.findFirstParent(body.containingFile.findElementAt(offset)) {
        it.parent == body
    }

    if (offsetCursorElement is PsiWhiteSpace) {
        return removeAfterOffset(offset, offsetCursorElement)
    }

    if (offsetCursorElement != null && offsetCursorElement != body.rBrace) {
        return offsetCursorElement
    }

    return afterAnchor
}

private fun removeAfterOffset(offset: Int, whiteSpace: PsiWhiteSpace): PsiElement {
    val spaceNode = whiteSpace.node
    if (spaceNode.textRange.contains(offset)) {
        var beforeWhiteSpaceText = spaceNode.text.substring(0, offset - spaceNode.startOffset)
        if (!StringUtil.containsLineBreak(beforeWhiteSpaceText)) {
            // Prevent insertion on same line
            beforeWhiteSpaceText += "\n"
        }

        val factory = JetPsiFactory(whiteSpace.project)

        val insertAfter = whiteSpace.prevSibling
        whiteSpace.delete()

        val beforeSpace = factory.createWhiteSpace(beforeWhiteSpaceText)
        insertAfter.parent.addAfter(beforeSpace, insertAfter)

        return insertAfter.nextSibling
    }

    return whiteSpace
}

public fun <T : JetDeclaration> insertMembersAfter(
        editor: Editor?,
        classOrObject: JetClassOrObject,
        members: Collection<T>,
        anchor: PsiElement? = null
): List<T> {
    members.ifEmpty { return emptyList() }

    return runWriteAction<List<T>> {
        val body = classOrObject.getOrCreateBody()

        var afterAnchor = anchor ?: findInsertAfterAnchor(editor, body) ?: return@runWriteAction emptyList()
        val insertedMembers = members.mapTo(SmartList<T>()) {
            @Suppress("UNCHECKED_CAST")
            (body.addAfter(it, afterAnchor) as T).apply { afterAnchor = this }
        }

        ShortenReferences.DEFAULT.process(insertedMembers)

        if (editor != null) {
            moveCaretIntoGeneratedElement(editor, insertedMembers.first())
        }

        insertedMembers
    }
}

public fun <T : JetDeclaration> insertMember(editor: Editor, classOrObject: JetClassOrObject, declaration: T): T {
    return insertMembersAfter(editor, classOrObject, listOf(declaration)).single()
}