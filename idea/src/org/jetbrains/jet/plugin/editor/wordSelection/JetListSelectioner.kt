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

package org.jetbrains.jet.plugin.editor.wordSelection

import com.intellij.codeInsight.editorActions.wordSelection.BasicSelectioner
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.jet.lang.psi.JetParameterList
import org.jetbrains.jet.lang.psi.JetTypeArgumentList
import org.jetbrains.jet.lang.psi.JetTypeParameterList
import org.jetbrains.jet.lang.psi.JetValueArgumentList
import org.jetbrains.jet.lexer.JetTokens

import java.util.Arrays

public class JetListSelectioner : BasicSelectioner() {
    override fun canSelect(e: PsiElement): Boolean {
        return e is JetParameterList || e is JetValueArgumentList || e is JetTypeParameterList || e is JetTypeArgumentList
    }

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange> {
        val node = e.getNode()!!
        val startNode = node.findChildByType(TokenSet.create(JetTokens.LPAR, JetTokens.LT))
        val endNode = node.findChildByType(TokenSet.create(JetTokens.RPAR, JetTokens.GT))
        if (startNode != null && endNode != null) {
            return Arrays.asList<TextRange>(TextRange(startNode.getStartOffset() + 1, endNode.getStartOffset()))
        }
        else {
            return Arrays.asList<TextRange>()
        }
    }
}
