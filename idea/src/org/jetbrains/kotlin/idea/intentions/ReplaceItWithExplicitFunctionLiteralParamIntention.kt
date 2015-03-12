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
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import org.jetbrains.kotlin.psi.JetFunctionLiteralExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.JetFunctionLiteral
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

public class ReplaceItWithExplicitFunctionLiteralParamIntention() : PsiElementBaseIntentionAction() {
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val simpleNameExpression = element.getStrictParentOfType<JetSimpleNameExpression>()!!

        val simpleNameReference = simpleNameExpression.getReference() as JetReference?
        val target = simpleNameReference?.resolveToDescriptors()?.first()!!

        val funcExpr = DescriptorToSourceUtils.descriptorToDeclaration(target.getContainingDeclaration()!!) as JetFunctionLiteral

        val newExpr = JetPsiFactory(simpleNameExpression).createExpression("{ it -> 42 }") as JetFunctionLiteralExpression
        funcExpr.addRangeAfter(newExpr.getFunctionLiteral().getValueParameterList(),
                               newExpr.getFunctionLiteral().getArrowNode()!!.getPsi(),
                               funcExpr.getLBrace())
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument())

        val paramToRename = funcExpr.getValueParameters().first()
        editor.getCaretModel().moveToOffset(paramToRename.getTextOffset())
        VariableInplaceRenameHandler().doRename(paramToRename, editor, null)
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        val simpleNameExpression = element.getStrictParentOfType<JetSimpleNameExpression>()
        if (simpleNameExpression == null || !isAutoCreatedIt(simpleNameExpression)) {
            return false
        }

        setText(JetBundle.message("replace.it.with.explicit.function.literal.param"))
        return true
    }

    override fun getFamilyName(): String {
        return JetBundle.message("replace.it.with.explicit.function.literal.param.family")
    }

    default object {
        fun isAutoCreatedIt(simpleNameExpression: JetSimpleNameExpression): Boolean {
            if (simpleNameExpression.getReferencedName() != "it") {
                return false
            }

            val bindingContext = simpleNameExpression.analyze()
            val reference = simpleNameExpression.getReference() as JetReference?
            val simpleNameTarget = reference?.resolveToDescriptors()?.firstOrNull() as? ValueParameterDescriptor?
            if (simpleNameTarget == null || bindingContext.get(BindingContext.AUTO_CREATED_IT, simpleNameTarget) != true) {
                return false
            }

            return true
        }
    }
}
