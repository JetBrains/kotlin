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

package org.jetbrains.jet.plugin.intentions

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
import org.jetbrains.jet.plugin.caches.resolve.analyze


public class ConvertToStringTemplateIntention : JetSelfTargetingIntention<JetBinaryExpression>("convert.to.string.template", javaClass()) {
    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        if (element.getOperationToken() != JetTokens.PLUS) return false

        val context = element.analyze()
        val elementType = BindingContextUtils.getRecordedTypeInfo(element, context)?.getType()
        if (!(KotlinBuiltIns.isString(elementType))) return false

        val (left, right) = Pair(element.getLeft(), element.getRight())
        if (left == null || right == null) return false

        return !(PsiUtilCore.hasErrorElementChild(left) || PsiUtilCore.hasErrorElementChild(right))
    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val parent = element.getParent()
        if (parent is JetBinaryExpression && isApplicableTo(parent)) {
            return applyTo(parent, editor)
        }

        val rightStr = mkString(element.getRight(), false)
        val resultStr = fold(element.getLeft(), rightStr)
        val expr = JetPsiFactory(element).createExpression(resultStr)

        element.replace(expr)
    }

    private fun fold(left: JetExpression?, right: String): String {
        val needsBraces = !right.isEmpty() && right.first() != '$' && Character.isJavaIdentifierPart(right.first())
        if (left is JetBinaryExpression && isApplicableTo(left)) {
            val l_right = mkString(left.getRight(), needsBraces)
            val newBase = "%s%s".format(l_right, right)
            return fold(left.getLeft(), newBase)
        }
        else {
            val leftStr = mkString(left, needsBraces)
            return "\"%s%s\"".format(leftStr, right)
        }
    }

    private fun mkString(expr: JetExpression?, needsBraces: Boolean): String {
        val expression = JetPsiUtil.deparenthesize(expr)
        val expressionText = expression?.getText() ?: ""
        return when (expression) {
            is JetConstantExpression -> {
                val context = expression.analyze()
                val trace = DelegatingBindingTrace(context, "Trace for evaluating constant")
                val constant = ConstantExpressionEvaluator.evaluate(expression, trace, null)
                if (constant is IntegerValueTypeConstant) {
                    val elementType = BindingContextUtils.getRecordedTypeInfo(expression, context)?.getType()
                    constant.getValue(elementType!!).toString()
                }
                else {
                    constant?.getValue().toString()
                }
            }
            is JetStringTemplateExpression -> {
                val base = if (expressionText.startsWith("\"\"\"") && (expressionText.endsWith("\"\"\""))) {
                    val unquoted = expressionText.substring(3, expressionText.length - 3)
                    StringUtil.escapeStringCharacters(unquoted)
                }
                else {
                    StringUtil.unquoteString(expressionText)
                }
                if (needsBraces && base.endsWith('$')) {
                    base.substring(0, base.length - 1) + "\\$"
                }
                else {
                    base
                }
            }
            is JetSimpleNameExpression ->
                if (needsBraces) "\${${expressionText}}" else "\$${expressionText}"
            null -> ""
            else -> "\${${expressionText.replaceAll("\n+", " ")}}"
        }
    }
}
