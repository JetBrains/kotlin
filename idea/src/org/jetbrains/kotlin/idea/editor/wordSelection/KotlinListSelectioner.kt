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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.JetParameterList
import org.jetbrains.kotlin.psi.JetTypeArgumentList
import org.jetbrains.kotlin.psi.JetTypeParameterList
import org.jetbrains.kotlin.psi.JetValueArgumentList
import org.jetbrains.kotlin.lexer.JetTokens
import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase

public class KotlinListSelectioner : ExtendWordSelectionHandlerBase() {
    default object {
        fun canSelect(e: PsiElement)
            = e is JetParameterList || e is JetValueArgumentList || e is JetTypeParameterList || e is JetTypeArgumentList
    }

    override fun canSelect(e: PsiElement) = KotlinListSelectioner.canSelect(e)

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
        val node = e.getNode()!!
        val startNode = node.findChildByType(TokenSet.create(JetTokens.LPAR, JetTokens.LT)) ?: return null
        val endNode = node.findChildByType(TokenSet.create(JetTokens.RPAR, JetTokens.GT)) ?: return null
        return listOf(TextRange(startNode.getStartOffset() + 1, endNode.getStartOffset()))
    }
}
