/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.builtins.isFunctionOrSuspendFunctionType
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnceWrtDiagnosticFactoryList
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.SPECIAL_FUNCTION_NAMES
import org.jetbrains.kotlin.resolve.calls.callUtil.getParameterForArgument
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.calls.tower.psiExpression
import org.jetbrains.kotlin.resolve.calls.tower.psiKotlinCall
import org.jetbrains.kotlin.types.DeferredType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isNothingOrNullableNothing
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
    private fun checkByReturnPositionWithoutExpected(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext,
    ): Boolean {
        val resultingDescriptor = resolvedCall.resultingDescriptor
        val expectedType = context.resolutionContext.expectedType
        val inferredReturnType = resultingDescriptor.returnType
        val isBuiltinFunctionalType =
            resolvedCall.resultingDescriptor.dispatchReceiverParameter?.value?.type?.isBuiltinFunctionalType == true

        if (inferredReturnType is DeferredType || isBuiltinFunctionalType) return false
        if (resultingDescriptor.name in SPECIAL_FUNCTION_NAMES || resolvedCall.call.typeArguments.isNotEmpty()) return false

        val lambdasFromArgumentsReturnTypes =
            resolvedCall.candidateDescriptor.valueParameters.filter { it.type.isFunctionOrSuspendFunctionType }
                .map { it.returnType?.arguments?.last()?.type }.toSet()
        val unsubstitutedReturnType = resultingDescriptor.original.returnType
        val hasImplicitNothing = inferredReturnType?.isNothing() == true &&
                unsubstitutedReturnType?.isTypeParameter() == true &&
                (TypeUtils.noExpectedType(expectedType) || !expectedType.isNothing())

        if (hasImplicitNothing && unsubstitutedReturnType !in lambdasFromArgumentsReturnTypes) {
            context.trace.reportDiagnosticOnceWrtDiagnosticFactoryList(
                Errors.IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION.on(reportOn),
                Errors.IMPLICIT_NOTHING_TYPE_ARGUMENT_AGAINST_NOT_NOTHING_EXPECTED_TYPE,
            )
            return true
        }

        return false
    }

    private fun ResolvedAtom.getResolvedCallAtom(bindingContext: BindingContext): ResolvedCallAtom? {
        if (this is SingleCallResolutionResult) return resultCallAtom

        val resolutionAtom = atom as? KotlinCallArgument ?: return null
        val resolvedCall = resolutionAtom.psiExpression.getResolvedCall(bindingContext)

        return if (resolvedCall is NewResolvedCallImpl) resolvedCall.resolvedCallAtom else null
    }

    private fun findFunctionsWithImplicitNothingAndReport(resolvedAtoms: List<ResolvedAtom>, context: CallCheckerContext): Boolean {
        var hasAlreadyReportedAtDepth = false

        for (resolvedAtom in resolvedAtoms) {
            val subResolveAtoms = resolvedAtom.subResolvedAtoms

            if (!subResolveAtoms.isNullOrEmpty() && findFunctionsWithImplicitNothingAndReport(subResolveAtoms, context)) {
                hasAlreadyReportedAtDepth = true
                continue
            }

            val resolvedCallAtom = resolvedAtom.getResolvedCallAtom(context.trace.bindingContext) ?: continue
            val candidateDescriptor = resolvedCallAtom.candidateDescriptor
            val isReturnTypeOwnTypeParameter = candidateDescriptor.typeParameters.any {
                it.typeConstructor == candidateDescriptor.returnType?.constructor
            }
            val isSpecialCall = candidateDescriptor.name in SPECIAL_FUNCTION_NAMES
            val hasExplicitTypeArguments = resolvedCallAtom.atom.psiKotlinCall.typeArguments.isNotEmpty() // not required

            if (!isSpecialCall && isReturnTypeOwnTypeParameter && !hasExplicitTypeArguments) {
                context.trace.reportDiagnosticOnceWrtDiagnosticFactoryList(
                    Errors.IMPLICIT_NOTHING_TYPE_ARGUMENT_AGAINST_NOT_NOTHING_EXPECTED_TYPE.on(
                        resolvedCallAtom.atom.psiKotlinCall.psiCall.run { calleeExpression ?: callElement },
                    ),
                    Errors.IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION,
                )
                hasAlreadyReportedAtDepth = true
            }
        }

        return hasAlreadyReportedAtDepth
    }

    private fun getSubResolvedAtomsToAnalyze(
        resolvedCall: ResolvedCall<*>,
        expectedType: KotlinType,
        bindingContext: BindingContext,
    ): List<ResolvedAtom>? {
        if (resolvedCall !is NewResolvedCallImpl) return null

        val hasNotNothingExpectedType = !TypeUtils.noExpectedType(expectedType) && !expectedType.isNothingOrNullableNothing()
        val hasNothingReturnType = resolvedCall.resultingDescriptor.returnType?.isNothingOrNullableNothing() == true
        val isSubResolvedAtomsNotEmpty = !resolvedCall.resolvedCallAtom.subResolvedAtoms.isNullOrEmpty()

        if (hasNotNothingExpectedType && hasNothingReturnType && isSubResolvedAtomsNotEmpty) {
            return resolvedCall.resolvedCallAtom.subResolvedAtoms
        }

        val resolvedAtomsFromArguments = resolvedCall.valueArguments.values.mapNotNull { argument ->
            if (argument !is ExpressionValueArgument) return@mapNotNull null

            val resolvedCallForArgument =
                argument.valueArgument?.getArgumentExpression()?.getResolvedCall(bindingContext) as? NewResolvedCallImpl
                    ?: return@mapNotNull null
            val expectedTypeForArgument = resolvedCall.getParameterForArgument(argument.valueArgument)?.type ?: return@mapNotNull null

            getSubResolvedAtomsToAnalyze(resolvedCallForArgument, expectedTypeForArgument, bindingContext)
        }.flatten()

        val extensionReceiver = resolvedCall.resolvedCallAtom.extensionReceiverArgument?.psiExpression
        val resolvedAtomsFromExtensionReceiver = extensionReceiver?.run {
            val extensionReceiverResolvedCall = getResolvedCall(bindingContext)
            // It's needed to exclude invoke with extension (when resolved call for extension equals to common resolved call)
            if (extensionReceiverResolvedCall == resolvedCall) return@run null

            getSubResolvedAtomsToAnalyze(
                getResolvedCall(bindingContext) ?: return@run null,
                resolvedCall.resultingDescriptor.extensionReceiverParameter?.type ?: return@run null,
                bindingContext,
            )
        }

        return if (resolvedAtomsFromExtensionReceiver != null) {
            resolvedAtomsFromArguments + resolvedAtomsFromExtensionReceiver
        } else resolvedAtomsFromArguments
    }

    private fun checkAgainstNotNothingExpectedType(resolvedCall: ResolvedCall<*>, context: CallCheckerContext): Boolean {
        val subResolvedAtoms =
            getSubResolvedAtomsToAnalyze(resolvedCall, context.resolutionContext.expectedType, context.trace.bindingContext) ?: return false

        return findFunctionsWithImplicitNothingAndReport(subResolvedAtoms, context)
    }

    override fun check(resolvedCall: ResolvedCall<*>, reportOn: PsiElement, context: CallCheckerContext) {
        checkByReturnPositionWithoutExpected(resolvedCall, reportOn, context) || checkAgainstNotNothingExpectedType(resolvedCall, context)
    }
}
