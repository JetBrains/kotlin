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

abstract class MissingConditionFixer<T: PsiElement>() : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, element: PsiElement) {
        val workElement = getElement(element)
        if (workElement == null) return

        val doc = editor.getDocument()
        val lParen = getLeftParenthesis(workElement)
        val rParen = getRightParenthesis(workElement)
        val condition = getCondition(workElement)

        if (condition == null) {
            if (lParen == null || rParen == null) {
                var stopOffset = doc.getLineEndOffset(doc.getLineNumber(workElement.range.start))
                val then = getBody(workElement)
                if (then != null) {
                    stopOffset = Math.min(stopOffset, then.range.start)
                }

                stopOffset = Math.min(stopOffset, workElement.range.end)

                doc.replaceString(workElement.range.start, stopOffset, "$keyword ()")
                processor.registerUnresolvedError(workElement.range.start + "$keyword (".length())
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

    abstract val keyword: String
    abstract fun getElement(element: PsiElement?): T?
    abstract fun getCondition(element: T): PsiElement?
    abstract fun getLeftParenthesis(element: T): PsiElement?
    abstract fun getRightParenthesis(element: T): PsiElement?
    abstract fun getBody(element: T): PsiElement?
}