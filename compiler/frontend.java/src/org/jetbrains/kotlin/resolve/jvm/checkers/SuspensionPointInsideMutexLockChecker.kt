/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.isTopLevelInPackage
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.checkers.findEnclosingSuspendFunction
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.source.getPsi

class SuspensionPointInsideMutexLockChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val descriptor = resolvedCall.candidateDescriptor
        if (descriptor !is FunctionDescriptor || !descriptor.isSuspend) return

        val enclosingSuspendFunctionSource = findEnclosingSuspendFunction(context)?.source?.getPsi() ?: return

        // Search for `synchronized` call
        var parent = reportOn
        var child = reportOn
        var insideLambda = false
        while (parent != enclosingSuspendFunctionSource) {
            if (parent is KtCallExpression) {
                if (checkCall(context, parent, child, insideLambda, reportOn, resolvedCall)) break
            }
            if (parent is KtLambdaExpression) {
                insideLambda = true
            }
            // The lambda is inside parentheses -> keep the child the same to check whether it is the second argument
            if (parent !is KtValueArgumentList) {
                child = parent
            }
            // parent.parent can be null if we edit the file, see EA-2158254 and KT-27484
            parent = parent.parent ?: return
        }
    }

    private fun checkCall(
        context: CallCheckerContext,
        parent: KtCallExpression,
        child: PsiElement,
        insideLambda: Boolean,
        reportOn: PsiElement,
        resolvedCall: ResolvedCall<*>
    ): Boolean {
        val call = context.trace[BindingContext.CALL, parent.calleeExpression] ?: return false
        val resolved = context.trace[BindingContext.RESOLVED_CALL, call] ?: return false
        val isSynchronized = resolved.resultingDescriptor.isTopLevelInPackage("synchronized", "kotlin")
        if (isSynchronized) {
            val isSecondArgument = (resolved.valueArgumentsByIndex?.get(1) as? ExpressionValueArgument)?.valueArgument == child
            if (insideLambda && isSecondArgument) {
                reportProblem(context, reportOn, resolvedCall)
            }
            return true
        }

        val isWithLock = resolved.resultingDescriptor.isTopLevelInPackage("withLock", "kotlin.concurrent")
        if (isWithLock) {
            reportProblem(context, reportOn, resolvedCall)
            return true
        }
        return false
    }

    private fun reportProblem(context: CallCheckerContext, reportOn: PsiElement, resolvedCall: ResolvedCall<*>) {
        context.trace.report(ErrorsJvm.SUSPENSION_POINT_INSIDE_CRITICAL_SECTION.on(reportOn, resolvedCall.resultingDescriptor))
    }
}