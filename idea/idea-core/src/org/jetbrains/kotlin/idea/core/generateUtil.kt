/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.SmartList
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.ifEmpty

fun moveCaretIntoGeneratedElement(editor: Editor, element: PsiElement) {
    val project = element.project
    val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(element)

    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

    pointer.element?.let { moveCaretIntoGeneratedElementDocumentUnblocked(editor, it) }
}

private fun moveCaretIntoGeneratedElementDocumentUnblocked(editor: Editor, element: PsiElement): Boolean {
    // Inspired by GenerateMembersUtils.positionCaret()

    if (element is KtDeclarationWithBody && element.hasBody()) {
        val expression = element.bodyExpression
        if (expression is KtBlockExpression) {
            val lBrace = expression.lBrace
            val rBrace = expression.rBrace

            if (lBrace != null && rBrace != null) {
                val firstInBlock = lBrace.siblings(forward = true, withItself = false).first { it !is PsiWhiteSpace }
                val lastInBlock = rBrace.siblings(forward = false, withItself = false).first { it !is PsiWhiteSpace }

                val start = firstInBlock.textRange!!.startOffset
                val end = lastInBlock.textRange!!.endOffset

                editor.moveCaret(Math.min(start, end))

                if (start < end) {
                    editor.selectionModel.setSelection(start, end)
                }

                return true
            }
        }
    }

    if (element is KtDeclarationWithInitializer && element.hasInitializer()) {
        val expression = element.initializer ?: throw AssertionError()

        val initializerRange = expression.textRange

        val offset = initializerRange?.startOffset ?: element.getTextOffset()

        editor.moveCaret(offset)

        if (initializerRange != null) {
            val endOffset = expression.siblings(forward = true, withItself = false).lastOrNull()?.endOffset ?: initializerRange.endOffset
            editor.selectionModel.setSelection(initializerRange.startOffset, endOffset)
        }

        return true
    }

    if (element is KtProperty) {
        for (accessor in element.accessors) {
            if (moveCaretIntoGeneratedElementDocumentUnblocked(editor, accessor)) {
                return true
            }
        }
    }

    editor.moveCaret(element.endOffset)
    return false
}

fun Editor.unblockDocument() {
    project?.let {
        PsiDocumentManager.getInstance(it).doPostponedOperationsAndUnblockDocument(document)
    }
}

fun Editor.moveCaret(offset: Int, scrollType: ScrollType = ScrollType.RELATIVE) {
    caretModel.moveToOffset(offset)
    scrollingModel.scrollToCaret(scrollType)
}

private fun findInsertAfterAnchor(editor: Editor?, body: KtClassBody): PsiElement? {
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

        val factory = KtPsiFactory(whiteSpace.project)

        val insertAfter = whiteSpace.prevSibling
        whiteSpace.delete()

        val beforeSpace = factory.createWhiteSpace(beforeWhiteSpaceText)
        insertAfter.parent.addAfter(beforeSpace, insertAfter)

        return insertAfter.nextSibling
    }

    return whiteSpace
}

fun <T : KtDeclaration> insertMembersAfter(
        editor: Editor?,
        classOrObject: KtClassOrObject,
        members: Collection<T>,
        anchor: PsiElement? = null
): List<T> {
    members.ifEmpty { return emptyList() }

    return runWriteAction {
        val insertedMembers = SmartList<T>()

        val (parameters, otherMembers) = members.partition { it is KtParameter }

        parameters.mapNotNullTo(insertedMembers) {
            if (classOrObject !is KtClass) return@mapNotNullTo null

            @Suppress("UNCHECKED_CAST")
            (classOrObject.createPrimaryConstructorParameterListIfAbsent().addParameter(it as KtParameter) as T)
        }

        if (otherMembers.isNotEmpty()) {
            val body = classOrObject.getOrCreateBody()

            var afterAnchor = anchor ?: findInsertAfterAnchor(editor, body) ?: return@runWriteAction emptyList<T>()
            otherMembers.mapTo(insertedMembers) {
                if (classOrObject is KtClass && classOrObject.isEnum()) {
                    val enumEntries = classOrObject.declarations.filterIsInstance<KtEnumEntry>()
                    val bound = (enumEntries.lastOrNull() ?: classOrObject.allChildren.firstOrNull { it.node.elementType == KtTokens.SEMICOLON })
                    if (it !is KtEnumEntry) {
                        if (bound != null && afterAnchor.startOffset <= bound.startOffset) {
                            afterAnchor = bound
                        }
                    }
                    else if (bound == null && body.declarations.isNotEmpty()) {
                        afterAnchor = body.lBrace!!
                    }
                    else if (bound != null && afterAnchor.startOffset > bound.startOffset) {
                        afterAnchor = bound.prevSibling!!
                    }
                }

                @Suppress("UNCHECKED_CAST")
                (body.addAfter(it, afterAnchor) as T).apply { afterAnchor = this }
            }
        }

        ShortenReferences.DEFAULT.process(insertedMembers)

        if (editor != null) {
            moveCaretIntoGeneratedElement(editor, insertedMembers.first())
        }

        val codeStyleManager = CodeStyleManager.getInstance(classOrObject.project)
        insertedMembers.forEach { codeStyleManager.reformat(it) }

        insertedMembers
    }
}

fun <T : KtDeclaration> insertMember(editor: Editor?, classOrObject: KtClassOrObject, declaration: T, anchor: PsiElement? = null): T {
    return insertMembersAfter(editor, classOrObject, listOf(declaration), anchor).single()
}
