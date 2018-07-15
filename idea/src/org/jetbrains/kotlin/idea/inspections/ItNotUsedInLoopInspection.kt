/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.getThisLabelName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall

class ItNotUsedInLoopInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return callExpressionVisitor {
            if (it.calleeExpression?.text == "forEach") {
                val lambda = it.lambdaArguments.singleOrNull()?.getLambdaExpression() ?: return@callExpressionVisitor
                val descriptor = lambda.analyze()[BindingContext.FUNCTION, lambda.functionLiteral] ?: return@callExpressionVisitor
                val iterableParameter = descriptor.valueParameters.singleOrNull() ?: return@callExpressionVisitor

                var used = false
                lambda.bodyExpression?.acceptChildren(object : KtVisitorVoid() {
                    override fun visitKtElement(element: KtElement) {
                        if (!used) {
                            if (element.children.isNotEmpty()) {
                                element.acceptChildren(this)
                            } else {
                                val bindingContext = element.analyze()
                                val resolvedCall = element.getResolvedCall(bindingContext) ?: return

                                used = resolvedCall.candidateDescriptor == iterableParameter
                            }
                        }
                    }
                })

                if (!used) {
                    holder.registerProblem(
                        lambda,
                        "Loop parameter '${iterableParameter.getThisLabelName()}' is unused",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    )
                }
            }
        }
    }
}