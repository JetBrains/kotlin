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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator

public class ConvertToStringTemplateInspection : IntentionBasedInspection<KtBinaryExpression>(
        ConvertToStringTemplateIntention(),
        { ConvertToStringTemplateIntention().shouldSuggestToConvert(it) }
)

public class ConvertToStringTemplateIntention : JetSelfTargetingOffsetIndependentIntention<KtBinaryExpression>(javaClass(), "Convert concatenation to template") {
    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        if (!isApplicableToNoParentCheck(element)) return false

        val parent = element.getParent()
        if (parent is KtBinaryExpression && isApplicableToNoParentCheck(parent)) return false

        return true
    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor) {
        applyTo(element)
    }

    public fun applyTo(element: KtBinaryExpression): KtStringTemplateExpression {
        return element.replaced(buildReplacement(element))
    }

    public fun shouldSuggestToConvert(expression: KtBinaryExpression): Boolean {
        val entries = buildReplacement(expression).entries
        return entries.none { it is KtBlockStringTemplateEntry } && entries.count { it is KtLiteralStringTemplateEntry } > 1
    }

    private fun isApplicableToNoParentCheck(expression: KtBinaryExpression): Boolean {
        if (expression.getOperationToken() != KtTokens.PLUS) return false
        if (!KotlinBuiltIns.isString(expression.analyze().getType(expression))) return false

        val left = expression.getLeft() ?: return false
        val right = expression.getRight() ?: return false
        return !PsiUtilCore.hasErrorElementChild(left) && !PsiUtilCore.hasErrorElementChild(right)
    }

    private fun buildReplacement(expression: KtBinaryExpression): KtStringTemplateExpression {
        val rightText = buildText(expression.getRight(), false)
        return fold(expression.getLeft(), rightText, KtPsiFactory(expression))
    }

    private fun fold(left: KtExpression?, right: String, factory: KtPsiFactory): KtStringTemplateExpression {
        val forceBraces = !right.isEmpty() && right.first() != '$' && right.first().isJavaIdentifierPart()

        if (left is KtBinaryExpression && isApplicableToNoParentCheck(left)) {
            val leftRight = buildText(left.getRight(), forceBraces)
            return fold(left.getLeft(), leftRight + right, factory)
        }
        else {
            val leftText = buildText(left, forceBraces)
            return factory.createExpression("\"$leftText$right\"") as KtStringTemplateExpression
        }
    }

    private fun buildText(expr: KtExpression?, forceBraces: Boolean): String {
        if (expr == null) return ""
        val expression = KtPsiUtil.safeDeparenthesize(expr)
        val expressionText = expression.getText()
        return when (expression) {
            is KtConstantExpression -> {
                val bindingContext = expression.analyze()
                val constant = ConstantExpressionEvaluator.getConstant(expression, bindingContext)
                constant?.getValue(bindingContext.getType(expression)!!).toString()
            }

            is KtStringTemplateExpression -> {
                val base = if (expressionText.startsWith("\"\"\"") && expressionText.endsWith("\"\"\"")) {
                    val unquoted = expressionText.substring(3, expressionText.length() - 3)
                    StringUtil.escapeStringCharacters(unquoted)
                }
                else {
                    StringUtil.unquoteString(expressionText)
                }
                if (forceBraces && base.endsWith('$')) {
                    base.dropLast(1) + "\\$"
                }
                else {
                    base
                }
            }

            is KtNameReferenceExpression ->
                "$" + (if (forceBraces) "{$expressionText}" else expressionText)

            null -> ""

            else -> "\${" + expressionText.replace("\n+".toRegex(), " ") + "}"
        }
    }
}
