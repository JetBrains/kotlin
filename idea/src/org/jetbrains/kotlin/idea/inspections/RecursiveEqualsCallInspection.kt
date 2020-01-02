/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.conventionNameCalls.isAnyEquals
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelectorOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver
import org.jetbrains.kotlin.util.OperatorNameConventions

class RecursiveEqualsCallInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {

            private fun KtExpression.isRecursiveEquals(argumentExpr: KtExpression?): Boolean {
                if (argumentExpr !is KtNameReferenceExpression) return false
                val context = analyze(BodyResolveMode.PARTIAL)
                val resolvedCall = getResolvedCall(context)
                val dispatchReceiver = resolvedCall?.dispatchReceiver as? ThisClassReceiver ?: return false
                val argumentDescriptor = context[BindingContext.REFERENCE_TARGET, argumentExpr] ?: return false
                val calledFunctionDescriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor
                if (calledFunctionDescriptor?.isAnyEquals() != true) return false

                val containingFunctionDescriptor = getNonStrictParentOfType<KtNamedFunction>()?.descriptor as? FunctionDescriptor
                return calledFunctionDescriptor == containingFunctionDescriptor &&
                        dispatchReceiver.classDescriptor == containingFunctionDescriptor.containingDeclaration &&
                        argumentDescriptor == containingFunctionDescriptor.valueParameters.singleOrNull()
            }

            private fun KtExpression.reportRecursiveEquals(invert: Boolean = false) {
                holder.registerProblem(
                    this,
                    "Recursive equals call",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    ReplaceWithReferentialEqualityFix(invert)
                )
            }

            override fun visitBinaryExpression(expr: KtBinaryExpression) {
                super.visitBinaryExpression(expr)
                if (expr.operationToken != KtTokens.EQEQ && expr.operationToken != KtTokens.EXCLEQ) return

                if (!expr.isRecursiveEquals(expr.right)) return
                expr.reportRecursiveEquals(invert = expr.operationToken == KtTokens.EXCLEQ)
            }

            override fun visitCallExpression(expr: KtCallExpression) {
                super.visitCallExpression(expr)
                val calleeExpression = expr.calleeExpression as? KtSimpleNameExpression ?: return
                if (calleeExpression.getReferencedNameAsName() != OperatorNameConventions.EQUALS) return

                if (!expr.isRecursiveEquals(expr.valueArguments.singleOrNull()?.getArgumentExpression())) return
                expr.reportRecursiveEquals()
            }
        }
    }
}

private class ReplaceWithReferentialEqualityFix(invert: Boolean) : LocalQuickFix {
    private val operator = if (invert) "!==" else "==="

    override fun getName() = "Replace with '$operator'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement
        val (right, target) = when (element) {
            is KtBinaryExpression -> {
                element.right to element
            }
            is KtCallExpression -> with(element) {
                valueArguments.first().getArgumentExpression() to getQualifiedExpressionForSelectorOrThis()
            }
            else -> return
        }
        if (right == null) return
        target.replace(KtPsiFactory(project).createExpressionByPattern("this $0 $1", operator, right))
    }
}

