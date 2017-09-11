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

package org.jetbrains.kotlin.idea.codeInsight.surroundWith.expression

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.statement.KotlinIfSurrounderBase
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtParenthesizedExpression

class KotlinIfElseExpressionSurrounder(private val withBraces: Boolean) : KotlinControlFlowExpressionSurrounderBase() {
    override fun getPattern(): String {
        return if (withBraces) "if (a) { \n$0 } else {\n}" else "if (a) $0 else"
    }

    override fun getTemplateDescription(): String {
        return if (withBraces) "if () { expr } else {}" else "if () expr else"
    }

    override fun getRange(editor: Editor, replaced: KtExpression): TextRange? {
        val expression = when (replaced) {
            is KtParenthesizedExpression -> replaced.expression
            else -> replaced
        } as? KtIfExpression

        return expression?.let { KotlinIfSurrounderBase.getRange(editor, it) }
    }

}