/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.conversion.copy.range
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isBoolean
import org.jetbrains.kotlin.utils.sure

class KotlinWithIfExpressionSurrounder(val withElse: Boolean) : KotlinExpressionSurrounder() {
    override fun isApplicable(expression: KtExpression) =
            super.isApplicable(expression) && (expression.analyze(BodyResolveMode.PARTIAL).getType(expression)?.isBoolean() ?: false)

    override fun surroundExpression(project: Project, editor: Editor, expression: KtExpression): TextRange? {
        val factory = KtPsiFactory(project)
        val ifExpression =
                expression.replace(
                        factory.createIf(
                                expression,
                                factory.createBlock("blockStubContentToBeRemovedLater"),
                                if (withElse) factory.createEmptyBody() else null
                        )
                ) as KtIfExpression

        CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(ifExpression)

        val firstStatementInThenRange = (ifExpression.then as? KtBlockExpression).sure {
            "Then branch should exist and be a block expression"
        }.statements.first().range

        editor.document.deleteString(firstStatementInThenRange.startOffset, firstStatementInThenRange.endOffset)

        return TextRange(firstStatementInThenRange.startOffset, firstStatementInThenRange.startOffset)
    }

    override fun getTemplateDescription() = "if (expr) { ... }" + (if (withElse) " else { ... }" else "")
}
