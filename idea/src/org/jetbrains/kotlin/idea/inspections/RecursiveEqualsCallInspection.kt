/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver

class RecursiveEqualsCallInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {

            override fun visitBinaryExpression(expr: KtBinaryExpression) {
                super.visitBinaryExpression(expr)
                if (expr.operationToken != KtTokens.EQEQ) return

                val context = expr.analyze(BodyResolveMode.PARTIAL)
                val resolvedCall = expr.getResolvedCall(context)
                val dispatchReceiver = resolvedCall?.dispatchReceiver as? ThisClassReceiver ?: return
                val calledFunctionDescriptor = resolvedCall.resultingDescriptor as? FunctionDescriptor
                if (calledFunctionDescriptor?.isAnyEquals() != true) return

                val containingFunctionDescriptor = expr.getNonStrictParentOfType<KtNamedFunction>()?.descriptor as? FunctionDescriptor
                if (calledFunctionDescriptor != containingFunctionDescriptor) return
                if (dispatchReceiver.classDescriptor != containingFunctionDescriptor.containingDeclaration) return

                holder.registerProblem(expr,
                                       "Recursive equals call",
                                       ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                                       ReplaceWithReferentialEqualityFix())
            }
        }
    }
}

private class ReplaceWithReferentialEqualityFix : LocalQuickFix {
    override fun getName() = "Replace with '==='"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val expr = descriptor.psiElement as? KtBinaryExpression ?: return
        val left = expr.left ?: return
        val right = expr.right ?: return
        expr.replace(KtPsiFactory(project).createExpressionByPattern("$0 $1 $2", left, "===", right))
    }
}

