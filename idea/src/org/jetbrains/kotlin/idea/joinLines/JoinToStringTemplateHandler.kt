/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.joinLines

import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.intentions.ConvertToStringTemplateIntention
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class JoinToStringTemplateHandler : JoinRawLinesHandlerDelegate {
    override fun tryJoinRawLines(document: Document, file: PsiFile, start: Int, end: Int): Int {
        if (file !is KtFile) return -1

        if (start == 0) return -1
        val c = document.charsSequence[start]
        val index = if (c == '\n') start - 1 else start

        val plus = file.findElementAt(index)?.takeIf { it.node?.elementType == KtTokens.PLUS } ?: return -1
        val expression = (plus.parent.parent as? KtBinaryExpression) ?: return -1
        val left = expression.left ?: return -1
        val right = expression.right ?: return -1
        if (left !is KtStringTemplateExpression || right !is KtStringTemplateExpression) return -1
        if (!ConvertToStringTemplateIntention.isApplicableToNoParentCheck(expression)) return -1

        val offset = left.endOffset - 1
        expression.replace(ConvertToStringTemplateIntention.buildReplacement(expression))
        return offset
    }

    override fun tryJoinLines(document: Document, file: PsiFile, start: Int, end: Int): Int = -1
}