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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetIfExpression
import org.jetbrains.jet.plugin.formatter.JetBlock
import org.jetbrains.jet.lang.psi.JetBlockExpression

public class KotlinMissingIfBranchFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, element: PsiElement) {
        if (element !is JetIfExpression) return
        val ifExpression = element as JetIfExpression

        val document = editor.getDocument()
        val elseBranch = ifExpression.getElse()
        val elseKeyword = ifExpression.getElseKeyword()

        if (elseKeyword != null) {
            if (elseBranch == null || elseBranch !is JetBlockExpression && elseBranch.startLine(editor.getDocument()) > elseKeyword.startLine(editor.getDocument())) {
                document.insertString(elseKeyword.range.end, "{}")
                return
            }
        }

        val thenBranch = ifExpression.getThen()
        if (thenBranch is JetBlockExpression) return

        val rParen = ifExpression.getRightParenthesis()
        if (rParen == null) return

        var transformingOneLiner = false
        if (thenBranch != null && thenBranch.startLine(editor.getDocument()) == ifExpression.startLine(editor.getDocument())) {
            if (ifExpression.getCondition() != null) return
            transformingOneLiner = true
        }

        val probablyNextStatementParsedAsThen = elseKeyword == null && elseBranch == null && !transformingOneLiner

        if (thenBranch == null || probablyNextStatementParsedAsThen) {
            document.insertString(rParen.range.end, "{}")
        }
        else {
            document.insertString(rParen.range.end, "{")
            document.insertString(thenBranch.range.end + 1, "}")
        }
    }
}
