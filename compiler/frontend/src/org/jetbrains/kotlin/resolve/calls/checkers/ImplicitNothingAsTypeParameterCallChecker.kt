/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.SPECIAL_FUNCTION_NAMES
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.DeferredType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter

object ImplicitNothingAsTypeParameterCallChecker : CallChecker {
    /*
     * The warning isn't reported in cases where there are lambda among the function arguments,
     * the return type of which is a type variable, that was inferred to Nothing.
     * This corresponds to useful cases in which this report will not be helpful.
     *
     * E.g.:
     *
     * 1) Return if null:
     *      x?.let { return }
     *
     * 2) Implicit receiver to shorter code writing:
     *      x.run {
     *          println(inv())
     *          return inv()
     *      }
     */
    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        val resultingDescriptor = resolvedCall.resultingDescriptor
        val inferredReturnType = resultingDescriptor.returnType
        val isBuiltinFunctionalType =
            resolvedCall.resultingDescriptor.dispatchReceiverParameter?.value?.type?.isBuiltinFunctionalType == true

        if (inferredReturnType is DeferredType || isBuiltinFunctionalType)
            return
        if (resultingDescriptor.name !in SPECIAL_FUNCTION_NAMES && resolvedCall.call.typeArguments.isEmpty()) {
            val lambdasFromArgumentsReturnTypes =
                resolvedCall.candidateDescriptor.valueParameters.filter { it.type.isFunctionType }
                    .map { it.returnType?.arguments?.last()?.type }.toSet()
            val unsubstitutedReturnType = resultingDescriptor.original.returnType
            val expectedType = context.resolutionContext.expectedType
            val hasImplicitNothing = inferredReturnType?.isNothing() == true &&
                    unsubstitutedReturnType?.isTypeParameter() == true &&
                    (TypeUtils.noExpectedType(expectedType) || !expectedType.isNothing())

            if (hasImplicitNothing && unsubstitutedReturnType !in lambdasFromArgumentsReturnTypes) {
                context.trace.report(Errors.IMPLICIT_NOTHING_AS_TYPE_PARAMETER.on(reportOn))
            }
        }
    }
}
