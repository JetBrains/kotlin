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

package org.jetbrains.kotlin.idea.editor.fixers

import com.intellij.lang.SmartEnterProcessorWithFixers
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.editor.KotlinSmartEnterHandler
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDoWhileExpression

class KotlinDoWhileFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, psiElement: PsiElement) {
        if (psiElement !is KtDoWhileExpression) return

        val doc = editor.document
        val start = psiElement.range.start
        val body = psiElement.body

        val whileKeyword = psiElement.whileKeyword
        if (body == null) {
            if (whileKeyword == null) {
                doc.replaceString(start, start + "do".length, "do {} while()")
            } else {
                doc.insertString(start + "do".length, "{}")
            }
            return
        } else if (whileKeyword != null && body !is KtBlockExpression && body.startLine(doc) > psiElement.startLine(doc)) {
            doc.insertString(whileKeyword.range.start, "}")
            doc.insertString(start + "do".length, "{")

            return
        }

        if (psiElement.condition == null) {
            val lParen = psiElement.leftParenthesis
            val rParen = psiElement.rightParenthesis

            when {
                whileKeyword == null -> doc.insertString(psiElement.range.end, "while()")
                lParen == null && rParen == null -> {
                    doc.replaceString(whileKeyword.range.start, whileKeyword.range.end, "while()")
                }
                lParen != null -> processor.registerUnresolvedError(lParen.range.end)
            }
        }
    }
}
