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

import org.jetbrains.kotlin.psi.JetBinaryExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetConstantExpression
import org.jetbrains.kotlin.psi.JetStringTemplateExpression
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetPsiUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze


public class ConvertToStringTemplateIntention : JetSelfTargetingOffsetIndependentIntention<JetBinaryExpression>(javaClass(), "Convert concatenation to template") {
    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        if (element.getOperationToken() != JetTokens.PLUS) return false
        if (!KotlinBuiltIns.isString(element.analyze().getType(element))) return false

        val left = element.getLeft() ?: return false
        val right = element.getRight() ?: return false
        return !PsiUtilCore.hasErrorElementChild(left) && !PsiUtilCore.hasErrorElementChild(right)
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val parent = element.getParent()
        if (parent is JetBinaryExpression && isApplicableTo(parent)) {
            return applyTo(parent, editor)
        }

        val rightText = buildText(element.getRight(), false)
        val text = fold(element.getLeft(), rightText)

        element.replace(JetPsiFactory(element).createExpression(text))
    }

    private fun fold(left: JetExpression?, right: String): String {
        val needsBraces = !right.isEmpty() && right.first() != '$' && right.first().isJavaIdentifierPart()

        if (left is JetBinaryExpression && isApplicableTo(left)) {
            val leftRight = buildText(left.getRight(), needsBraces)
            return fold(left.getLeft(), leftRight + right)
        }
        else {
            val leftText = buildText(left, needsBraces)
            return "\"$leftText$right\""
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
                    constant?.getValue().toString()
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
