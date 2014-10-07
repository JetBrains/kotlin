/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody
import org.jetbrains.jet.lang.psi.JetBlockExpression
import com.intellij.psi.PsiWhiteSpace
import com.intellij.openapi.editor.ScrollType
import org.jetbrains.jet.lang.psi.JetWithExpressionInitializer
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.psiUtil.siblings

private fun moveCaretIntoGeneratedElement(editor: Editor, element: PsiElement): Boolean {
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
            if (moveCaretIntoGeneratedElement(editor, accessor)) {
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
