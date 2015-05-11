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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.psi.JetFunctionLiteral
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

public class ReplaceExplicitFunctionLiteralParamWithItIntention() : PsiElementBaseIntentionAction() {
    override fun getFamilyName() = "Replace explicit lambda parameter with 'it'"

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val functionLiteral = targetFunctionLiteral(element, editor.getCaretModel().getOffset()) ?: return false

        val parameter = functionLiteral.getValueParameters().singleOrNull() ?: return false
        if (parameter.getTypeReference() != null) return false

        setText("Replace explicit parameter '${parameter.getName()}' with 'it'")
        return true
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val caretOffset = editor.getCaretModel().getOffset()
        val functionLiteral = targetFunctionLiteral(element, editor.getCaretModel().getOffset())!!
        val cursorInParameterList = functionLiteral.getValueParameterList()!!.getTextRange().containsOffset(caretOffset)
        ParamRenamingProcessor(editor, functionLiteral, cursorInParameterList).run()
    }

    private fun targetFunctionLiteral(element: PsiElement, caretOffset: Int): JetFunctionLiteral? {
        val expression = element.getParentOfType<JetSimpleNameExpression>(true)
        if (expression != null) {
            val reference = expression.getReference() as JetReference?
            val target = reference?.resolveToDescriptors(expression.analyze())?.firstOrNull() as? ParameterDescriptor ?: return null
            val functionDescriptor = target.getContainingDeclaration() as? AnonymousFunctionDescriptor ?: return null
            return DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor) as? JetFunctionLiteral
        }

        val functionLiteral = element.getParentOfType<JetFunctionLiteral>(true) ?: return null
        val arrow = functionLiteral.getArrow() ?: return null
        if (caretOffset > arrow.endOffset) return null
        return functionLiteral
    }

    private class ParamRenamingProcessor(
            val editor: Editor,
            val functionLiteral: JetFunctionLiteral,
            val cursorWasInParameterList: Boolean
    ) : RenameProcessor(editor.getProject(),
                        functionLiteral.getValueParameters().single(),
                        "it",
                        false,
                        false
    ) {
        public override fun performRefactoring(usages: Array<out UsageInfo>) {
            super.performRefactoring(usages)

            functionLiteral.deleteChildRange(functionLiteral.getValueParameterList(), functionLiteral.getArrow()!!)

            if (cursorWasInParameterList) {
                editor.getCaretModel().moveToOffset(functionLiteral.getBodyExpression()!!.getTextOffset())
            }

            val project = functionLiteral.getProject()
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument())
            CodeStyleManager.getInstance(project).adjustLineIndent(functionLiteral.getContainingFile(), functionLiteral.getTextRange())

        }
    }
}
