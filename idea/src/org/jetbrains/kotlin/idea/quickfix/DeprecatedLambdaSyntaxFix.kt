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
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.idea.util.psiModificationUtil.getFunctionLiteralArgumentName
import org.jetbrains.kotlin.idea.util.psiModificationUtil.moveInsideParenthesesAndReplaceWith
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
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
    }
}

public class DeprecatedLambdaSyntaxInWholeProjectFix(element: JetFunctionLiteralExpression) :
        JetWholeProjectModalAction<JetFunctionLiteralExpression, Collection<DeprecatedSyntaxFix>>(
                element, JetBundle.message("migrate.lambda.syntax.in.whole.project.modal.title")) {

    override fun getText() = JetBundle.message("migrate.lambda.syntax.in.whole.project")
    override fun getFamilyName() = JetBundle.message("migrate.lambda.syntax.in.whole.project.family")

    override fun collectDataForFile(project: Project, file: JetFile): Collection<DeprecatedSyntaxFix>? {
        val lambdas = ArrayList<DeprecatedSyntaxFix>()
        file.accept(LambdaCollectionVisitor(lambdas), 0)
        return lambdas.sortBy { -it.level }
    }

    override fun applyChangesForFile(project: Project, file: JetFile, data: Collection<DeprecatedSyntaxFix>) {
        data.forEach {
            it.runFix()
        }
    }

    private class LambdaCollectionVisitor(val lambdas: MutableCollection<DeprecatedSyntaxFix>) : JetTreeVisitor<Int>() {
        override fun visitFunctionLiteralExpression(functionLiteralExpression: JetFunctionLiteralExpression, data: Int): Void? {
            functionLiteralExpression.acceptChildren(this, data + 1)
            if (JetPsiUtil.isDeprecatedLambdaSyntax(functionLiteralExpression)) {
                lambdas.add(DeprecatedSyntaxFix.createFix(functionLiteralExpression, data))
            }
            return null
        }
    }

    companion object Factory : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic)
                = (diagnostic.getPsiElement() as? JetFunctionLiteralExpression)?.let { DeprecatedLambdaSyntaxInWholeProjectFix(it) }
    }
}

private trait DeprecatedSyntaxFix {
    val level: Int

    // you must run it under write action
    fun runFix()

    internal companion object {
        fun createFix(functionLiteralExpression: JetFunctionLiteralExpression, level: Int = 0): DeprecatedSyntaxFix {
            val functionLiteral = functionLiteralExpression.getFunctionLiteral()
            val hasNoReturnAndReceiverType = !functionLiteral.hasDeclaredReturnType() && functionLiteral.getReceiverTypeReference() == null

            return if (hasNoReturnAndReceiverType) DeparenthesizeParameterList(functionLiteralExpression, level)
            else LambdaToFunctionExpression(functionLiteralExpression, level)
        }
    }
}

private class DeparenthesizeParameterList(
        val functionLiteralExpression: JetFunctionLiteralExpression,
        override val level: Int = 0
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
        val functionLiteralExpression: JetFunctionLiteralExpression,
        override val level: Int = 0
): DeprecatedSyntaxFix {
    val functionLiteralArgumentName: String?
    val receiverType: String?
    val returnType: String?

    init {
        val bindingContext = functionLiteralExpression.analyze()
        val functionLiteralType = bindingContext.get(BindingContext.EXPRESSION_TYPE, functionLiteralExpression)
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

    private fun JetElement.replaceWithReturn(psiFactory: JetPsiFactory) {
        if (this is JetReturnExpression) {
            return
        }
        else {
            replace(psiFactory.createReturn(getText()))
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

        val blockExpression = functionLiteral.getBodyExpression()
        if (blockExpression == null) return functionWithEmptyBody

        val statements = blockExpression.getStatements()
        if (statements.isEmpty()) return functionWithEmptyBody

        if (statements.size() == 1) {
            return psiFactory.createFunction(functionDeclaration + " = " + statements.first().getText())
        }

        // many statements
        if (returnType != null) statements.last().replaceWithReturn(psiFactory)

        return psiFactory.createFunction(functionDeclaration + "{ " + blockExpression.getText() + "}")
    }

}
