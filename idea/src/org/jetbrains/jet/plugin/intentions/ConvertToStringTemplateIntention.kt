package org.jetbrains.jet.plugin.intentions

import org.jetbrains.jet.lang.psi.JetBinaryExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetConstantExpression
import org.jetbrains.jet.lang.psi.JetStringTemplateExpression
import org.jetbrains.jet.lang.evaluate.ConstantExpressionEvaluator
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace
import org.jetbrains.jet.lang.resolve.constants.IntegerValueTypeConstant
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.psi.JetPsiUtil
import com.intellij.psi.PsiErrorElement
import com.intellij.openapi.util.text.StringUtil


public class ConvertToStringTemplateIntention : JetSelfTargetingIntention<JetBinaryExpression>("convert.to.string.template", javaClass()) {
    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        if (element.getOperationToken() != JetTokens.PLUS) return false

        val context = AnalyzerFacadeWithCache.getContextForElement(element)

        val elementType = BindingContextUtils.getRecordedTypeInfo(element, context)?.getType()
        val constructorDescriptor = (elementType?.getConstructor()?.getDeclarationDescriptor())
        if (constructorDescriptor == null) return false
        if (DescriptorUtils.getFqName(constructorDescriptor).asString() != "kotlin.String") return false

        val (left, right) = Pair(element.getLeft(), element.getRight())
        if (left == null || right == null) return false

        return left.getChildren().none({ c -> c is PsiErrorElement }) &&
        right.getChildren().none({ c -> c is PsiErrorElement })

    }

    override fun applyTo(element: JetBinaryExpression, editor: Editor) {
        val parent = element.getParent()
        if (parent is JetBinaryExpression && isApplicableTo(parent))
            return applyTo(parent, editor)
        val right = mkString(element.getRight(), false)
        val resultStr = fold(element.getLeft(), right)
        val expr = JetPsiFactory.createExpression(element.getProject(), resultStr)
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
                val context = AnalyzerFacadeWithCache.getContextForElement(expression)
                val trace = DelegatingBindingTrace(context, "Trace for evaluating constant")
                val constant = ConstantExpressionEvaluator.evaluate(expression, trace, null)
                if ( constant is IntegerValueTypeConstant ) {
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
