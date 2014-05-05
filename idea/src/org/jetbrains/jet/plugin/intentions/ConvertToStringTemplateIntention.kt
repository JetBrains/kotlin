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


public class ConvertToStringTemplateIntention : JetSelfTargetingIntention<JetBinaryExpression>("convert.to.string.template", javaClass()) {
    override fun isApplicableTo(element: JetBinaryExpression): Boolean {
        if ( element.getOperationToken() != JetTokens.PLUS) return false
        val context = AnalyzerFacadeWithCache.getContextForElement(element)
        val elementType = BindingContextUtils.getRecordedTypeInfo(element, context)?.getType()
        val constructorDescriptor = (elementType?.getConstructor()?.getDeclarationDescriptor())
        if (constructorDescriptor == null) return false
        return DescriptorUtils.getFqName(constructorDescriptor).asString() == "kotlin.String"
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
        val needsBraces = right.first() != '$' && Character.isJavaIdentifierPart(right.first())
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
                val base = unquote(expression.getText())
                if (needsBraces && base.endsWith('$'))
                    base.substring(0, base.length - 1) + "\\$"
                else
                    base
            }
            is JetSimpleNameExpression ->
                if (needsBraces) "\${${expression.getText()}}" else "\$${expression.getText()}"
            null -> ""
            else -> "\${${expression.getText().replace('\n', ' ')}}"
        }
    }

    private fun unquote(str: String): String {
        val length = str.length
        if (length < 2 || str.first() != '"' || str.last() != '"')
            throw IllegalStateException("Cannot unquote string: ${str}")
        else
            return str.substring(1, length - 1)
    }
}
