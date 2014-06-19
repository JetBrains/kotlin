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

import org.jetbrains.jet.plugin.editor.KotlinSmartEnterHandler
import com.intellij.lang.SmartEnterProcessorWithFixers
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetIfExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.openapi.util.TextRange

object KotlinIfConditionFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, element: PsiElement) {
        if (element !is JetIfExpression) return
        val ifExpression = element as JetIfExpression

        val doc = editor.getDocument()
        val lParen = ifExpression.getLeftParenthesis()
        val rParen = ifExpression.getRightParenthesis()
        val condition = ifExpression.getCondition()

        if (condition == null) {
            if (lParen == null || rParen == null) {
                var stopOffset = doc.getLineEndOffset(doc.getLineNumber(ifExpression.range.start))
                val then = ifExpression.getThen()
                if (then != null) {
                    stopOffset = Math.min(stopOffset, then.range.start)
                }

                stopOffset = Math.min(stopOffset, ifExpression.range.end)

                doc.replaceString(ifExpression.range.start, stopOffset, "if ()")
                processor.registerUnresolvedError(ifExpression.range.start + "if (".length())
            }
            else {
                processor.registerUnresolvedError(lParen.range.end)
            }
        }
        else {
            if (rParen == null) {
                doc.insertString(condition.range.end, ")")
            }
        }
    }
}