/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils
import org.jetbrains.kotlin.types.typeUtil.isNothing

object ImplicitNothingAsTypeParameterCallChecker : CallChecker {
    private val SPECIAL_FUNCTION_NAMES = ControlStructureTypingUtils.ResolveConstruct.values().map { it.specialFunctionName }.toSet()

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
        if (resolvedCall.candidateDescriptor.name !in SPECIAL_FUNCTION_NAMES && resolvedCall.call.typeArguments.isEmpty()) {
            val lambdasFromArgumentsReturnTypes =
                resolvedCall.candidateDescriptor.valueParameters.filter { it.type.isFunctionType }
                    .map { it.returnType?.arguments?.last()?.type }.toSet()

            val hasImplicitNothingExceptLambdaReturnTypes = resolvedCall.typeArguments.any { (unsubstitutedType, resultingType) ->
                resultingType.isNothing() && unsubstitutedType.defaultType !in lambdasFromArgumentsReturnTypes
            }

            if (hasImplicitNothingExceptLambdaReturnTypes) {
                context.trace.report(Errors.IMPLICIT_NOTHING_AS_TYPE_PARAMETER.on(reportOn))
            }
        }
    }
}
