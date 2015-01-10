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
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.JetTokens

import java.util.ArrayList
import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase
import org.jetbrains.kotlin.kdoc.psi.api.KDoc

public class KotlinDocCommentSelectioner : ExtendWordSelectionHandlerBase() {
    override fun canSelect(e: PsiElement)
            = e is KDoc

    override fun select(e: PsiElement, editorText: CharSequence, cursorOffset: Int, editor: Editor): List<TextRange>? {
        return ExtendWordSelectionHandlerBase.expandToWholeLine(editorText, e.getTextRange())
    }
}
