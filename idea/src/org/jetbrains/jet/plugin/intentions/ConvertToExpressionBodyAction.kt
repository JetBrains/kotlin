package org.jetbrains.jet.plugin.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.types.JetType

public class ConvertToExpressionBodyAction : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String = JetBundle.message("convert.to.expression.body.action.family.name")

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        setText(JetBundle.message("convert.to.expression.body.action.name"))
        val data = calcData(element)
        return data != null && !containsReturn(data.value)
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val (declaration, value) = calcData(element)!!

        if (!declaration.hasDeclaredReturnType() && declaration is JetNamedFunction) {
            val valueType = expressionType(value)
            if (valueType == null || !KotlinBuiltIns.getInstance().isUnit(valueType)) {
                specifyTypeExplicitly(declaration, "Unit")
            }
        }

        val body = declaration.getBodyExpression()!!
        declaration.addBefore(JetPsiFactory.createEQ(project), body)
        body.replace(value)
    }

    private data class Data(val declaration: JetDeclarationWithBody, val value: JetExpression)

    private fun calcData(element: PsiElement): Data? {
        val declaration = PsiTreeUtil.getParentOfType(element, javaClass<JetDeclarationWithBody>())
        if (declaration == null || declaration is JetFunctionLiteral) return null
        val body = declaration.getBodyExpression()
        if (!declaration.hasBlockBody() || body !is JetBlockExpression) return null

        val statements = body.getStatements()
        if (statements.size != 1) return null
        val statement = statements[0]
        return when(statement) {
            is JetReturnExpression -> {
                val value = statement.getReturnedExpression()
                if (value != null) Data(declaration, value) else null
            }

            //TODO: IMO this is not good code, there should be a way to detect that JetExpression does not have value
            is JetDeclaration -> null // is JetExpression but does not have value
            is JetLoopExpression -> null // is JetExpression but does not have value

            is JetExpression -> {
                if (statement is JetBinaryExpression && statement.getOperationToken() == JetTokens.EQ) return null // assignment does not have value

                val expressionType = expressionType(statement)
                if (expressionType != null &&
                      (KotlinBuiltIns.getInstance().isUnit(expressionType) || KotlinBuiltIns.getInstance().isNothing(expressionType)))
                    Data(declaration, statement)
                else
                    null
            }

            else -> null
        }
    }

    private fun containsReturn(element: PsiElement): Boolean {
        if (element is JetReturnExpression) return true
        //TODO: would be better to have some interface of declaration where return can be used
        if (element is JetNamedFunction || element is JetPropertyAccessor) return false // can happen inside

        var child = element.getFirstChild()
        while (child != null) {
            if (containsReturn(child!!)) return true
            child = child!!.getNextSibling()
        }

        return false
    }
}
