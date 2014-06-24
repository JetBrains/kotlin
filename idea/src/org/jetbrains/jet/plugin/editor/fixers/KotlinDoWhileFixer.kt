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

package org.jetbrains.jet.plugin.editor.fixers

import com.intellij.lang.SmartEnterProcessorWithFixers
import org.jetbrains.jet.plugin.editor.KotlinSmartEnterHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetDoWhileExpression
import org.jetbrains.jet.lang.psi.JetBlockExpression

public class KotlinDoWhileFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, psiElement: PsiElement) {
        if (psiElement !is JetDoWhileExpression) return

        val doc = editor.getDocument()
        val stmt = psiElement as JetDoWhileExpression
        val start = stmt.range.start
        val body = stmt.getBody()

        val whileKeyword = stmt.getWhileKeywordElement()
        if (body == null) {
            if (whileKeyword == null) {
                doc.replaceString(start, start + "do".length(), "do {} while()")
            }
            else {
                doc.insertString(start + "do".length(), "{}")
            }
            return
        }
        else if (whileKeyword != null && body !is JetBlockExpression && body.startLine(doc) > stmt.startLine(doc)) {
            doc.insertString(start + "do".length(), "{")
            doc.insertString(whileKeyword.range.start - 1, "}")

            return
        }

        if (stmt.getCondition() == null) {
            val lParen = stmt.getLeftParenthesis()
            val rParen = stmt.getRightParenthesis()

            when {
                whileKeyword == null -> doc.insertString(stmt.range.end, "while()")
                lParen == null && rParen == null -> {
                    doc.replaceString(whileKeyword.range.start, whileKeyword.range.end, "while()")
                }
                lParen != null -> processor.registerUnresolvedError(lParen.range.end)
            }
        }
    }
}