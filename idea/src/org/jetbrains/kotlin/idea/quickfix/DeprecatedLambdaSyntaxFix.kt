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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionFactory
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.psiModificationUtil.getFunctionLiteralArgumentName
import org.jetbrains.kotlin.idea.util.psiModificationUtil.moveInsideParenthesesAndReplaceWith
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.sure
import java.util.ArrayList

public class DeprecatedLambdaSyntaxFix(element: JetFunctionLiteralExpression) : JetIntentionAction<JetFunctionLiteralExpression>(element) {
    override fun getText() = JetBundle.message("migrate.lambda.syntax")
    override fun getFamilyName() = JetBundle.message("migrate.lambda.syntax.family")

    override fun invoke(project: Project, editor: Editor, file: JetFile) {
        DeprecatedSyntaxFix.createFix(element).runFix()
    }

    companion object Factory : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic)
                = (diagnostic.getPsiElement() as? JetFunctionLiteralExpression)?.let { DeprecatedLambdaSyntaxFix(it) }

        public fun createWholeProjectFixFactory(): JetSingleIntentionActionFactory = createIntentionFactory {
            JetWholeProjectForEachElementOfTypeFix.createByTaskFactory(
                    taskFactory = fun (it: JetFunctionLiteralExpression) = fixTaskFactory(it),
                    taskProcessor = { it.runFix() },
                    modalTitle = JetBundle.message("migrate.lambda.syntax.in.whole.project.modal.title"),
                    name = JetBundle.message("migrate.lambda.syntax.in.whole.project"),
                    familyName = JetBundle.message("migrate.lambda.syntax.in.whole.project.family")
            )
        }

        private fun fixTaskFactory(functionLiteralExpression: JetFunctionLiteralExpression): DeprecatedSyntaxFix? {
            return if (JetPsiUtil.isDeprecatedLambdaSyntax(functionLiteralExpression)) {
                DeprecatedSyntaxFix.createFix(functionLiteralExpression)
            }
            else {
                null
            }
        }
    }
}

private trait DeprecatedSyntaxFix {
    // you must run it under write action
    fun runFix()

    internal companion object {
        fun createFix(functionLiteralExpression: JetFunctionLiteralExpression): DeprecatedSyntaxFix {
            val functionLiteral = functionLiteralExpression.getFunctionLiteral()
            val hasNoReturnAndReceiverType = !functionLiteral.hasDeclaredReturnType() && functionLiteral.getReceiverTypeReference() == null

            return if (hasNoReturnAndReceiverType) DeparenthesizeParameterList(functionLiteralExpression)
            else LambdaToFunctionExpression(functionLiteralExpression)
        }
    }
}

private class DeparenthesizeParameterList(
        val functionLiteralExpression: JetFunctionLiteralExpression
): DeprecatedSyntaxFix {

    override fun runFix() {
        if (!JetPsiUtil.isDeprecatedLambdaSyntax(functionLiteralExpression)) return

        val psiFactory = JetPsiFactory(functionLiteralExpression)
        val functionLiteral = functionLiteralExpression.getFunctionLiteral()
        val parameterList = functionLiteral.getValueParameterList()
        if (parameterList != null && parameterList.isParenthesized()) {
            val oldParameterList = parameterList.getText()
            val newParameterList = oldParameterList.substring(1..oldParameterList.length() - 2)
            parameterList.replace(psiFactory.createFunctionLiteralParameterList(newParameterList))
        }
    }
}

private class LambdaToFunctionExpression(
        val functionLiteralExpression: JetFunctionLiteralExpression
): DeprecatedSyntaxFix {
    val functionLiteralArgumentName: String?
    val receiverType: String?
    val returnType: String?

    init {
        val bindingContext = functionLiteralExpression.analyze()
        val functionLiteralType = bindingContext.getType(functionLiteralExpression)
        assert(functionLiteralType != null && KotlinBuiltIns.isFunctionOrExtensionFunctionType(functionLiteralType)) {
            "Broken function type for expression: ${functionLiteralExpression.getText()}, at: ${DiagnosticUtils.atLocation(functionLiteralExpression)}"
        }
        receiverType = KotlinBuiltIns.getReceiverType(functionLiteralType)?.let { IdeDescriptorRenderers.SOURCE_CODE.renderType(it) }
        returnType = KotlinBuiltIns.getReturnTypeFromFunctionType(functionLiteralType).let {
            if (KotlinBuiltIns.isUnit(it))
                null
            else
                IdeDescriptorRenderers.SOURCE_CODE.renderType(it)
        }
        functionLiteralArgumentName = getFunctionLiteralArgument()?.getFunctionLiteralArgumentName(bindingContext)
    }

    override fun runFix() {
        if (!JetPsiUtil.isDeprecatedLambdaSyntax(functionLiteralExpression)) return

        val newFunctionExpression = createFunctionExpression()

        val literalArgument = getFunctionLiteralArgument()
        val replacedFunctionExpression = if (literalArgument == null) {
            functionLiteralExpression.replace(newFunctionExpression)
        }
        else {
            literalArgument.moveInsideParenthesesAndReplaceWith(newFunctionExpression, functionLiteralArgumentName).
                    getValueArguments().last().getArgumentExpression()
        }

        val functionExpression = JetPsiUtil.deparenthesize(replacedFunctionExpression as JetExpression) as JetNamedFunction

        ShortenReferences.DEFAULT.process(
                listOf(functionExpression.getReceiverTypeReference(), functionExpression.getTypeReference()).filterNotNull())
    }

    private fun getFunctionLiteralArgument(): JetFunctionLiteralArgument? {
        val argument = functionLiteralExpression.getParentOfType<JetFunctionLiteralArgument>(strict = false)

        if (argument != null && argument.getFunctionLiteral() == functionLiteralExpression) {
            return argument
        }
        return null
    }

    private fun JetExpression.replaceWithReturn(psiFactory: JetPsiFactory) {
        if (this is JetReturnExpression) {
            return
        }
        else {
            replace(psiFactory.createExpressionByPattern("return $0", this))
        }
    }

    private fun getLambdaLabelName(): String? {
        val labeledExpression = functionLiteralExpression.getParentOfType<JetLabeledExpression>(strict = false)
        if (labeledExpression != null && JetPsiUtil.deparenthesize(labeledExpression.getBaseExpression()) == functionLiteralExpression) {
            return labeledExpression.getLabelName()
        }
        return null
    }

    private fun createFunctionExpression(): JetNamedFunction {
        val psiFactory = JetPsiFactory(functionLiteralExpression)
        val functionLiteral = functionLiteralExpression.getFunctionLiteral()
        val functionName = getLambdaLabelName()
        val parameterList = functionLiteral.getValueParameterList()?.getText()

        val functionDeclaration = "fun " +
                                  (receiverType?.let { "$it." } ?: "") +
                                  (functionName ?: "") +
                                  (parameterList ?: "()") +
                                  (returnType?.let { ": $it" } ?: "")

        val functionWithEmptyBody = psiFactory.createFunction(functionDeclaration + " {}")

        val blockExpression = functionLiteral.getBodyExpression() ?: return functionWithEmptyBody

        val statements = blockExpression.getStatements()
        if (statements.isEmpty()) return functionWithEmptyBody

        if (statements.size() == 1) {
            return psiFactory.createFunction(functionDeclaration + " = " + statements.first().getText())
        }

        // many statements
        if (returnType != null) statements.filterIsInstance<JetExpression>().lastOrNull()?.replaceWithReturn(psiFactory)

        val fromElement = functionLiteral.getArrow() ?: functionLiteral.getLBrace()
        val toElement = functionLiteral.getRBrace()
        // to include comments in the start/end of the body
        val bodyText = fromElement.siblings(withItself = false)
                .takeWhile { it != toElement }
                .map { it.getText() }
                .joinToString("")
        return psiFactory.createFunction(functionDeclaration + "{ " + bodyText + "}")
    }

}
