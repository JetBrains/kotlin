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
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.psi.JetFunctionLiteral
import org.jetbrains.jet.plugin.references.JetReference
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils

public class ReplaceItWithExplicitFunctionLiteralParamIntention() : PsiElementBaseIntentionAction() {
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val simpleNameExpression = PsiTreeUtil.getParentOfType(element, javaClass<JetSimpleNameExpression>())!!

        val simpleNameReference = simpleNameExpression.getReference() as JetReference?
        val target = simpleNameReference?.resolveToDescriptors()?.first()!!

        val funcExpr = DescriptorToSourceUtils.descriptorToDeclaration(target.getContainingDeclaration()!!) as JetFunctionLiteral

        val newExpr = JetPsiFactory(simpleNameExpression).createExpression("{ it -> 42 }") as JetFunctionLiteralExpression
        funcExpr.addRangeAfter(newExpr.getFunctionLiteral().getValueParameterList(),
                               newExpr.getFunctionLiteral().getArrowNode()!!.getPsi(),
                               funcExpr.getOpenBraceNode().getPsi())
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument())

        val paramToRename = funcExpr.getValueParameters().first()
        editor.getCaretModel().moveToOffset(paramToRename.getTextOffset())
        VariableInplaceRenameHandler().doRename(paramToRename, editor, null)
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val simpleNameExpression = PsiTreeUtil.getParentOfType(element, javaClass<JetSimpleNameExpression>())
        if (simpleNameExpression == null || !isAutoCreatedIt(simpleNameExpression)) {
            return false
        }

        setText(JetBundle.message("replace.it.with.explicit.function.literal.param"))
        return true
    }

    override fun getFamilyName(): String {
        return JetBundle.message("replace.it.with.explicit.function.literal.param.family")
    }

    class object {
        fun isAutoCreatedIt(simpleNameExpression: JetSimpleNameExpression): Boolean {
            if (simpleNameExpression.getReferencedName() != "it") {
                return false
            }

            val bindingContext = AnalyzerFacadeWithCache.getContextForElement(simpleNameExpression)
            val reference = simpleNameExpression.getReference() as JetReference?
            val simpleNameTarget = reference?.resolveToDescriptors()?.firstOrNull() as? ValueParameterDescriptor?
            if (simpleNameTarget == null || bindingContext.get(BindingContext.AUTO_CREATED_IT, simpleNameTarget) != true) {
                return false
            }

            return true
        }
    }
}
