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

import com.intellij.codeInsight.CodeInsightUtilBase
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern

abstract class KotlinControlFlowExpressionSurrounderBase : KotlinExpressionSurrounder() {
    override fun isApplicableToStatements() = false

    override fun surroundExpression(project: Project, editor: Editor, expression: KtExpression): TextRange? {
        val factory = KtPsiFactory(expression)

        val newElement = factory.createExpressionByPattern(getPattern(), expression.text)
        val replaced = expression.replaced(newElement)

        CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(replaced)

        return getRange(editor, replaced)
    }

    protected abstract fun getPattern(): String
    protected abstract fun getRange(editor: Editor, replaced: KtExpression): TextRange?
}