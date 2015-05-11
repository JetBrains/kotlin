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

package org.jetbrains.kotlin.idea.editor.wordSelection

import com.intellij.psi.PsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import org.jetbrains.kotlin.psi.JetDeclaration
import java.util.ArrayList
import org.jetbrains.kotlin.psi.psiUtil.siblings
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

public class KotlinDeclarationSelectioner : ExtendWordSelectionHandlerBase() {
    override fun canSelect(e: PsiElement)
            = e is JetDeclaration

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
        val result = ArrayList<TextRange>()

        val firstChild = e.getFirstChild()
        val firstNonComment = firstChild
                .siblings(forward = true, withItself = true)
                .first { it !is PsiComment && it !is PsiWhiteSpace }

        val lastChild = e.getLastChild()
        val lastNonComment = lastChild
                .siblings(forward = false, withItself = true)
                .first { it !is PsiComment && it !is PsiWhiteSpace }

        if (firstNonComment != firstChild || lastNonComment != lastChild) {
            val start = firstNonComment.startOffset
            val end = lastNonComment.endOffset
            result.addAll(ExtendWordSelectionHandlerBase.expandToWholeLine(editorText, TextRange(start, end)))
        }

        result.addAll(ExtendWordSelectionHandlerBase.expandToWholeLine(editorText, e.getTextRange()))

        return result
    }
}
