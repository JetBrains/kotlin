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

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtValueArgumentList

class KotlinListSelectioner : ExtendWordSelectionHandlerBase() {
    companion object {
        fun canSelect(e: PsiElement) =
            e is KtParameterList || e is KtValueArgumentList || e is KtTypeParameterList || e is KtTypeArgumentList
    }

    override fun canSelect(e: PsiElement) = KotlinListSelectioner.canSelect(e)

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
        val node = e.node!!
        val startNode = node.findChildByType(TokenSet.create(KtTokens.LPAR, KtTokens.LT)) ?: return null
        val endNode = node.findChildByType(TokenSet.create(KtTokens.RPAR, KtTokens.GT)) ?: return null
        val innerRange = TextRange(startNode.startOffset + 1, endNode.startOffset)
        if (e is KtTypeArgumentList || e is KtTypeParameterList) {
            return listOf(
                innerRange,
                TextRange(startNode.startOffset, endNode.startOffset + endNode.textLength)
            )
        }
        return listOf(innerRange)
    }
}
