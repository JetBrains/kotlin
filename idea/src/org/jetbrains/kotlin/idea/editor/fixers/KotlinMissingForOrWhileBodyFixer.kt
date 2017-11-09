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
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtWhileExpression

class KotlinMissingForOrWhileBodyFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, element: PsiElement) {
        if (!(element is KtForExpression || element is KtWhileExpression)) return
        val loopExpression = element as KtLoopExpression

        val doc = editor.document

        val body = loopExpression.body
        if (body is KtBlockExpression) return

        if (!loopExpression.isValidLoopCondition()) return

        if (body != null && body.startLine(doc) == loopExpression.startLine(doc)) return

        val rParen = loopExpression.rightParenthesis ?: return

        doc.insertString(rParen.range.end, "{}")
    }

    private fun KtLoopExpression.isValidLoopCondition() = leftParenthesis != null && rightParenthesis != null
}

