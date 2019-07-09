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
import org.jetbrains.kotlin.psi.KtIfExpression

class KotlinMissingIfBranchFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, element: PsiElement) {
        if (element !is KtIfExpression) return

        val document = editor.document
        val elseBranch = element.`else`
        val elseKeyword = element.elseKeyword

        if (elseKeyword != null) {
            if (elseBranch == null || elseBranch !is KtBlockExpression && elseBranch.startLine(document) > elseKeyword.startLine(document)) {
                document.insertString(elseKeyword.range.end, "{}")
                return
            }
        }

        val thenBranch = element.then
        if (thenBranch is KtBlockExpression) return

        val rParen = element.rightParenthesis ?: return

        var transformingOneLiner = false
        if (thenBranch != null && thenBranch.startLine(document) == element.startLine(document)) {
            if (element.condition != null) return
            transformingOneLiner = true
        }

        val probablyNextStatementParsedAsThen = elseKeyword == null && elseBranch == null && !transformingOneLiner

        if (thenBranch == null || probablyNextStatementParsedAsThen) {
            document.insertString(rParen.range.end, "{}")
        } else {
            document.insertString(rParen.range.end, "{")
            document.insertString(thenBranch.range.end + 1, "}")
        }
    }
}
