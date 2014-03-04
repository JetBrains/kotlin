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

public class ConvertToBlockBodyAction : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String = JetBundle.message("convert.to.block.body.action.family.name")

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        setText(JetBundle.message("convert.to.block.body.action.name"))
        return findDeclaration(element) != null
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val declaration = findDeclaration(element)!!
        val body = declaration.getBodyExpression()!!

        fun generateBody(returnsValue: Boolean): JetExpression {
            val bodyType = expressionType(body)
            val needReturn = returnsValue &&
                    (bodyType == null || (!KotlinBuiltIns.getInstance().isUnit(bodyType) && !KotlinBuiltIns.getInstance().isNothing(bodyType)))

            val oldBodyText = body.getText()!!
            val newBodyText = if (needReturn) "return ${oldBodyText}" else oldBodyText
            return JetPsiFactory.createFunctionBody(project, newBodyText)
        }

        if (declaration is JetNamedFunction) {
            val returnType = functionReturnType(declaration)!!
            if (!declaration.hasDeclaredReturnType() && !KotlinBuiltIns.getInstance().isUnit(returnType)) {
                specifyTypeExplicitly(declaration, returnType)
            }

            val newBody = generateBody(!KotlinBuiltIns.getInstance().isUnit(returnType) && !KotlinBuiltIns.getInstance().isNothing(returnType))

            declaration.getEqualsToken()!!.delete()
            body.replace(newBody)
        }
        else if (declaration is JetPropertyAccessor) {
            val newBody = generateBody(declaration.isGetter())
            declaration.getEqualsToken()!!.delete()
            body.replace(newBody)
        }
        else {
            throw RuntimeException("Unknown declaration type: $declaration")
        }
    }

    private fun findDeclaration(element: PsiElement): JetDeclarationWithBody? {
        val declaration = PsiTreeUtil.getParentOfType(element, javaClass<JetDeclarationWithBody>())
        if (declaration == null || declaration is JetFunctionLiteral || declaration.hasBlockBody()) return null
        val body = declaration.getBodyExpression()
        if (body == null) return null

        return when (declaration) {
            is JetNamedFunction -> {
                val returnType = functionReturnType(declaration)
                if (returnType == null) return null
                if (!declaration.hasDeclaredReturnType() && returnType.isError()) return null // do not convert when type is implicit and unknown
                declaration
            }

            is JetPropertyAccessor -> declaration

            else -> throw RuntimeException("Unknown declaration type: $declaration")
        }
    }
}
