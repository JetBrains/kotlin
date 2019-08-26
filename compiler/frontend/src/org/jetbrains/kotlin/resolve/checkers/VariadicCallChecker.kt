/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class VariadicCallChecker : CallChecker {
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        if (resolvedCall.candidateDescriptor !is FunctionDescriptor)
            return

        val variadicTypeParameters = resolvedCall.candidateDescriptor.typeParameters.filter { it.isVariadic }
        if (variadicTypeParameters.isEmpty())
            return

        val typeArgumentList = resolvedCall.call.callElement.safeAs<KtCallExpression>()?.typeArgumentList
        typeArgumentList?.let {
            context.trace.report(Errors.UNSUPPORTED.on(it, "Explicit type arguments for variadic functions are not supported"))
        }
    }
}
