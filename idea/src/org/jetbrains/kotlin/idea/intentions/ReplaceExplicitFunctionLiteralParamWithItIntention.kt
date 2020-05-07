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

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ReplaceExplicitFunctionLiteralParamWithItIntention : PsiElementBaseIntentionAction() {
    override fun getFamilyName() = KotlinBundle.message("replace.explicit.lambda.parameter.with.it")

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val functionLiteral = targetFunctionLiteral(element, editor.caretModel.offset) ?: return false

        val parameter = functionLiteral.valueParameters.singleOrNull() ?: return false
        if (parameter.typeReference != null) return false
        if (parameter.destructuringDeclaration != null) return false

        if (functionLiteral.anyDescendantOfType<KtFunctionLiteral> { literal ->
                literal.usesName(element.text) && (!literal.hasParameterSpecification() || literal.usesName("it"))
            }) return false

        val lambda = functionLiteral.parent as? KtLambdaExpression ?: return false
        val call = lambda.getStrictParentOfType<KtCallExpression>()
        if (call != null) {
            val argumentIndex = call.valueArguments.indexOfFirst { it.getArgumentExpression() == lambda }
            val callOrQualified = call.getQualifiedExpressionForSelectorOrThis()
            val newCallOrQualified = callOrQualified.copied()
            val newCall = newCallOrQualified.safeAs<KtQualifiedExpression>()?.callExpression ?: newCallOrQualified.safeAs() ?: return false
            val newArgument = newCall.valueArguments.getOrNull(argumentIndex) ?: newCall.lambdaArguments.singleOrNull() ?: return false
            newArgument.replace(KtPsiFactory(element).createLambdaExpression("", "TODO()"))
            val newContext = newCallOrQualified.analyzeAsReplacement(callOrQualified, callOrQualified.analyze(BodyResolveMode.PARTIAL))
            if (newCallOrQualified.getResolvedCall(newContext)?.resultingDescriptor == null) return false
        }

        text = KotlinBundle.message("replace.explicit.parameter.0.with.it", parameter.name.toString())
        return true
    }

    private fun KtFunctionLiteral.usesName(name: String): Boolean = anyDescendantOfType<KtSimpleNameExpression> { nameExpr ->
        nameExpr.getReferencedName() == name
    }

    override fun startInWriteAction(): Boolean = false

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val caretOffset = editor.caretModel.offset
        val functionLiteral = targetFunctionLiteral(element, editor.caretModel.offset) ?: return
        val cursorInParameterList = functionLiteral.valueParameterList?.textRange?.containsOffset(caretOffset) ?: return
        ParamRenamingProcessor(editor, functionLiteral, cursorInParameterList).run()
    }

    private fun targetFunctionLiteral(element: PsiElement, caretOffset: Int): KtFunctionLiteral? {
        val expression = element.getParentOfType<KtNameReferenceExpression>(true)
        if (expression != null) {
            val target = expression.resolveMainReferenceToDescriptors().singleOrNull() as? ParameterDescriptor ?: return null
            val functionDescriptor = target.containingDeclaration as? AnonymousFunctionDescriptor ?: return null
            return DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor) as? KtFunctionLiteral
        }

        val functionLiteral = element.getParentOfType<KtFunctionLiteral>(true) ?: return null
        val arrow = functionLiteral.arrow ?: return null
        if (caretOffset > arrow.endOffset) return null
        return functionLiteral
    }

    private class ParamRenamingProcessor(
        val editor: Editor,
        val functionLiteral: KtFunctionLiteral,
        val cursorWasInParameterList: Boolean
    ) : RenameProcessor(
        editor.project!!,
        functionLiteral.valueParameters.single(),
        "it",
        false,
        false
    ) {
        override fun performRefactoring(usages: Array<out UsageInfo>) {
            super.performRefactoring(usages)

            functionLiteral.deleteChildRange(functionLiteral.valueParameterList, functionLiteral.arrow ?: return)

            if (cursorWasInParameterList) {
                editor.caretModel.moveToOffset(functionLiteral.bodyExpression?.textOffset ?: return)
            }

            val project = functionLiteral.project
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
            CodeStyleManager.getInstance(project).adjustLineIndent(functionLiteral.containingFile, functionLiteral.textRange)

        }
    }
}
