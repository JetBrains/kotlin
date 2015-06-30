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
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator

public class ConvertToStringTemplateInspection : IntentionBasedInspection<JetBinaryExpression>(
        ConvertToStringTemplateIntention(),
        { ConvertToStringTemplateIntention().isConversionResultSimple(it) }
)

public class ConvertToStringTemplateIntention : JetSelfTargetingOffsetIndependentIntention<JetBinaryExpression>(javaClass(), "Convert concatenation to template") {
    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        if (!isApplicableToNoParentCheck(element)) return false

        val parent = element.getParent()
        if (parent is JetBinaryExpression && isApplicableToNoParentCheck(parent)) return false

        return true
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        applyTo(element)
    }

    public fun applyTo(element: JetBinaryExpression): JetStringTemplateExpression {
        return element.replaced(buildReplacement(element))
    }

    public fun isConversionResultSimple(expression: JetBinaryExpression): Boolean {
        return buildReplacement(expression).getEntries().none { it is JetBlockStringTemplateEntry }
    }

    private fun isApplicableToNoParentCheck(expression: JetBinaryExpression): Boolean {
        if (expression.getOperationToken() != JetTokens.PLUS) return false
        if (!KotlinBuiltIns.isString(expression.analyze().getType(expression))) return false

        val left = expression.getLeft() ?: return false
        val right = expression.getRight() ?: return false
        return !PsiUtilCore.hasErrorElementChild(left) && !PsiUtilCore.hasErrorElementChild(right)
    }

    private fun buildReplacement(expression: JetBinaryExpression): JetStringTemplateExpression {
        val rightText = buildText(expression.getRight(), false)
        return fold(expression.getLeft(), rightText, JetPsiFactory(expression))
    }

    private fun fold(left: JetExpression?, right: String, factory: JetPsiFactory): JetStringTemplateExpression {
        val forceBraces = !right.isEmpty() && right.first() != '$' && right.first().isJavaIdentifierPart()

        if (left is JetBinaryExpression && isApplicableToNoParentCheck(left)) {
            val leftRight = buildText(left.getRight(), forceBraces)
            return fold(left.getLeft(), leftRight + right, factory)
        }
        else {
            val leftText = buildText(left, forceBraces)
            return factory.createExpression("\"$leftText$right\"") as JetStringTemplateExpression
        }
    }

    private fun buildText(expr: JetExpression?, forceBraces: Boolean): String {
        if (expr == null) return ""
        val expression = JetPsiUtil.safeDeparenthesize(expr)
        val expressionText = expression.getText()
        return when (expression) {
            is JetConstantExpression -> {
                val bindingContext = expression.analyze()
                val constant = ConstantExpressionEvaluator.getConstant(expression, bindingContext)
                if (constant is IntegerValueTypeConstant) {
                    val type = bindingContext.getType(expression)!!
                    constant.getValue(type).toString()
                }
                else {
                    constant?.value.toString()
                }
            }

            is JetStringTemplateExpression -> {
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

            is JetSimpleNameExpression ->
                "$" + (if (forceBraces) "{$expressionText}" else expressionText)

            null -> ""

            else -> "\${" + expressionText.replace("\n+".toRegex(), " ") + "}"
        }
    }
}
