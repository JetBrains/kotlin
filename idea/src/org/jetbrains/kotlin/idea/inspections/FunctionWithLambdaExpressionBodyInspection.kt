/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.intentions.SpecifyExplicitLambdaSignatureIntention
import org.jetbrains.kotlin.idea.quickfix.SpecifyTypeExplicitlyFix
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.types.typeUtil.isNothing

class FunctionWithLambdaExpressionBodyInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : KtVisitorVoid() {
        override fun visitNamedFunction(function: KtNamedFunction) {
            check(function)
        }

        override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
            if (accessor.isSetter) return
            if (accessor.returnTypeReference != null) return
            check(accessor)
        }

        private fun check(element: KtDeclarationWithBody) {
            val callableDeclaration = element.getNonStrictParentOfType<KtCallableDeclaration>() ?: return
            if (callableDeclaration.typeReference != null) return
            val lambda = element.bodyExpression as? KtLambdaExpression ?: return
            val functionLiteral = lambda.functionLiteral
            if (functionLiteral.arrow != null || functionLiteral.valueParameterList != null) return
            val lambdaBody = functionLiteral.bodyBlockExpression ?: return

            val file = element.containingKtFile
            val used = ReferencesSearch.search(callableDeclaration).any()
            val fixes = listOfNotNull(
                IntentionWrapper(SpecifyTypeExplicitlyFix(), file),
                IntentionWrapper(AddArrowIntention(), file),
                if (!used && lambdaBody.statements.size == 1 && lambdaBody.allChildren.none { it is PsiComment }) RemoveBracesFix() else null,
                if (!used) WrapRunFix() else null
            )
            holder.registerProblem(
                lambda,
                "Function with `= { ... }` and inferred return type",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                *fixes.toTypedArray()
            )
        }
    }

    private class AddArrowIntention : SpecifyExplicitLambdaSignatureIntention() {
        override fun allowCaretInsideElement(element: PsiElement) = true
    }

    private class RemoveBracesFix : LocalQuickFix {
        override fun getName() = "Remove braces"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val lambda = descriptor.psiElement as? KtLambdaExpression ?: return
            val body = lambda.functionLiteral.bodyExpression ?: return
            val replaced = lambda.replaced(body)
            replaced.parentOfType<KtCallableDeclaration>()?.setTypeIfNeed()
        }
    }

    private class WrapRunFix : LocalQuickFix {
        override fun getName() = "Convert to run { ... }"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val lambda = descriptor.psiElement as? KtLambdaExpression ?: return
            val body = lambda.functionLiteral.bodyExpression ?: return
            val replaced = lambda.replaced(KtPsiFactory(lambda).createExpressionByPattern("run { $0 }", body))
            replaced.parentOfType<KtCallableDeclaration>()?.setTypeIfNeed()
        }
    }
}

private fun KtCallableDeclaration.setTypeIfNeed() {
    val type = (resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType
    if (type?.isNothing() == true) {
        this.setType(type)
    }
}
