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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.resolve.calls.callUtil.getType

private fun KtStringTemplateExpression.singleExpressionOrNull() =
        children.singleOrNull()?.children?.firstOrNull() as? KtExpression

class RemoveSingleExpressionStringTemplateInspection : IntentionBasedInspection<KtStringTemplateExpression>(
        RemoveSingleExpressionStringTemplateIntention::class,
        additionalChecker = {
            templateExpression ->
            templateExpression.singleExpressionOrNull()?.let {
                KotlinBuiltIns.isString(it.getType(it.analyze()))
            } ?: false
        }
) {
    override val problemText = "Redundant string template"
}

class RemoveSingleExpressionStringTemplateIntention : SelfTargetingOffsetIndependentIntention<KtStringTemplateExpression>(
        KtStringTemplateExpression::class.java,
        "Remove single-expression string template"
) {
    override fun isApplicableTo(element: KtStringTemplateExpression) =
            element.singleExpressionOrNull() != null

    override fun applyTo(element: KtStringTemplateExpression, editor: Editor?) {
        val expression = element.singleExpressionOrNull() ?: return
        val type = expression.getType(expression.analyze())
        val newElement =
                if (KotlinBuiltIns.isString(type)) expression
                else KtPsiFactory(element).createExpressionByPattern("$0.$1()", expression, "toString")
        element.replace(newElement)
    }
}