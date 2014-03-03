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

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.usageView.UsageInfo
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.plugin.JetBundle
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.jet.lang.resolve.BindingContextUtils

public class ReplaceExplicitFunctionLiteralParamWithItIntention() : PsiElementBaseIntentionAction() {
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val funcExpr = findFunctionLiteralToActOn(element)!!
        val cursorWasOverParameterList = PsiTreeUtil.getParentOfType(element, javaClass<JetParameter>()) != null
        ParamRenamingProcessor(editor, funcExpr, cursorWasOverParameterList).run()
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        if (PsiTreeUtil.getParentOfType(element, javaClass<JetFunctionLiteralExpression>()) == null) {
            return false
        }

        val funcExpr = findFunctionLiteralToActOn(element)
        if (funcExpr == null || funcExpr.getValueParameters().size() != 1) {
            return false
        }

        val parameter = funcExpr.getValueParameters().first()
        if (parameter.getTypeReference() != null) {
            return false
        }

        setText(JetBundle.message("replace.explicit.function.literal.param.with.it", parameter.getName()))
        return true
    }

    override fun getFamilyName(): String {
        return JetBundle.message("replace.explicit.function.literal.param.with.it.family")
    }

    private fun findFunctionLiteralToActOn(element: PsiElement): JetFunctionLiteral? {
        val expression = PsiTreeUtil.getParentOfType(element, javaClass<JetParameter>()) as? JetElement?
        if (expression == null) {
            return null
        }

        val context = AnalyzerFacadeWithCache.getContextForElement(expression)
        val declaration = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, expression)?.getContainingDeclaration()
        if (declaration !is AnonymousFunctionDescriptor) {
            return null
        }

        return BindingContextUtils.descriptorToDeclaration(context, declaration) as JetFunctionLiteral
    }
}

private class ParamRenamingProcessor(val editor: Editor, val funcLiteral: JetFunctionLiteral, val cursorWasOverParameterList: Boolean) :
RenameProcessor(
        editor.getProject(),
        funcLiteral.getValueParameters().first(),
        "it",
        false,
        false
) {
    public override fun performRefactoring(usages: Array<out UsageInfo>?) {
        super.performRefactoring(usages)
        funcLiteral.deleteChildRange(funcLiteral.getValueParameterList(), funcLiteral.getArrowNode()!!.getPsi())
        if (cursorWasOverParameterList) {
            editor.getCaretModel().moveToOffset(funcLiteral.getBodyExpression()!!.getTextOffset())
        }

        CodeStyleManager.getInstance(editor.getProject()!!)!!.reformatText(funcLiteral.getContainingFile()!!,
                                                                           funcLiteral.getTextRange()!!.getStartOffset(),
                                                                           funcLiteral.getTextRange()!!.getEndOffset())

    }
}
