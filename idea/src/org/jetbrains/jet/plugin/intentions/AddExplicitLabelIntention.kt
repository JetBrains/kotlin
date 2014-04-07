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
import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.rename.inplace.VariableInplaceRenameHandler
import com.intellij.usageView.UsageInfo
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import java.util.*
import org.jetbrains.jet.plugin.intentions.LabeledExpressionAssociatedBlockMapping

public class AddExplicitLabelIntention() : PsiElementBaseIntentionAction() {
    private fun getLabel(element: PsiElement?): String? {
        val parentAsPrefixExpression = element?.getParent() as? JetPrefixExpression?

        if (element is JetNamedDeclaration) {
            return "@${element.getName()}"
        }
        else if (parentAsPrefixExpression != null && JetPsiUtil.isLabeledExpression(parentAsPrefixExpression)) {
            return parentAsPrefixExpression.getOperationReference().getReferencedName()
        }
        else {
            return null
        }
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        for (mapping in LabeledExpressionAssociatedBlockMapping.MAPPINGS) {
            val labeledExpression = PsiTreeUtil.getParentOfType(element, mapping.labeledExpressionType)
            if (labeledExpression != null) {
                val label = getLabel(PsiTreeUtil.getParentOfType(labeledExpression, *mapping.associatedBlockTypes))
                labeledExpression.replace(JetPsiFactory.createExpression(project, labeledExpression.getText() + label))
                return
            }
        }
        assert(false, "isAvailable() should have returned false")
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        for (mapping in LabeledExpressionAssociatedBlockMapping.MAPPINGS) {
            val labeledExpr = PsiTreeUtil.getParentOfType(element, mapping.labeledExpressionType)
            if (labeledExpr == null || labeledExpr.getLabelName() != null) {
                continue
            }

            val label = getLabel(PsiTreeUtil.getParentOfType(labeledExpr, *mapping.associatedBlockTypes))
            if (label == null) {
                return false
            }

            setText(JetBundle.message("add.explicit.label", label));
            return true
        }
        return false
    }

    override fun getFamilyName(): String {
        return JetBundle.message("add.explicit.label.family")
    }
}
