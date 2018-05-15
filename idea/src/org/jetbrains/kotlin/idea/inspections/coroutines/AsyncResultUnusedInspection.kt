/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.coroutines

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.inspections.AbstractResultUnusedChecker
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

class AsyncResultUnusedInspection : AbstractResultUnusedChecker(
    expressionChecker = fun(expression): Boolean =
        expression is KtCallExpression && expression.calleeExpression?.text == "async",
    callChecker = fun(resolvedCall): Boolean =
        resolvedCall.resultingDescriptor.fqNameOrNull()?.asString() == "kotlinx.coroutines.experimental.async"
) {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        callExpressionVisitor(fun(expression) {
            if (!check(expression)) return
            holder.registerProblem(expression.calleeExpression ?: expression, "Result of 'async' is never used")
        })
}