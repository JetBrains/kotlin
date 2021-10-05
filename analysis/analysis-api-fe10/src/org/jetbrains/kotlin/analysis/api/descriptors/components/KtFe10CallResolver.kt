/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.calls.*
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescFunctionSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.callableId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.diagnostics.KtNonBoundToPsiErrorDiagnostic
import org.jetbrains.kotlin.analysis.api.impl.base.components.AbstractKtCallResolver
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.references.readWriteAccessWithFullExpressionWithPossibleResolve
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findAssignment
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotationConstructor
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

internal class KtFe10CallResolver(override val analysisSession: KtFe10AnalysisSession) : AbstractKtCallResolver() {
    private companion object {
        private const val UNRESOLVED_CALL_MESSAGE = "Unresolved call"
    }

    override val token: ValidityToken
        get() = analysisSession.token

    override fun resolveAccessorCall(call: KtSimpleNameExpression): KtCall? {
        val bindingContext = analysisSession.analyze(call, KtFe10AnalysisSession.AnalysisMode.PARTIAL_WITH_DIAGNOSTICS)
        val resolvedCall = call.getResolvedCall(bindingContext) ?: return null
        val resultingDescriptor = resolvedCall.resultingDescriptor

        if (resultingDescriptor is PropertyDescriptor) {
            @Suppress("DEPRECATION")
            val access = call.readWriteAccessWithFullExpressionWithPossibleResolve(
                readWriteAccessWithFullExpressionByResolve = { null }
            ).first

            val setterValue = findAssignment(call)?.right
            val accessorSymbol = when (resultingDescriptor) {
                is SyntheticJavaPropertyDescriptor -> {
                    when {
                        access.isWrite -> resultingDescriptor.setMethod?.let { KtFe10DescFunctionSymbol(it, analysisSession) }
                        access.isRead -> KtFe10DescFunctionSymbol(resultingDescriptor.getMethod, analysisSession)
                        else -> null
                    }
                }
                else -> {
                    when {
                        access.isWrite -> resultingDescriptor.setter?.let { KtFe10DescPropertySetterSymbol(it, analysisSession) }
                        access.isRead -> resultingDescriptor.getter?.let { KtFe10DescPropertyGetterSymbol(it, analysisSession) }
                        else -> null
                    }
                }
            }

            if (accessorSymbol != null) {
                val target = when {
                    !access.isWrite || setterValue != null -> KtSuccessCallTarget(accessorSymbol, token)
                    else -> {
                        val diagnostic = KtNonBoundToPsiErrorDiagnostic(factoryName = null, "Setter value is missing", token)
                        KtErrorCallTarget(listOf(accessorSymbol), diagnostic, token)
                    }
                }

                val argumentMapping = LinkedHashMap<KtExpression, KtValueParameterSymbol>()
                if (access.isWrite && setterValue != null) {
                    val setterParameterSymbol = accessorSymbol.valueParameters.single()
                    argumentMapping[setterValue] = setterParameterSymbol
                }

                return KtFunctionCall(argumentMapping, target, KtSubstitutor.Empty(token), token)
            }
        }

        return null
    }

    override fun resolveCall(call: KtCallElement): KtCall? = withValidityAssertion {
        return resolveCall(call, isUsualCall = true)
    }

    override fun resolveCall(call: KtBinaryExpression): KtCall? = withValidityAssertion {
        return resolveCall(call, isUsualCall = false)
    }

    override fun resolveCall(call: KtUnaryExpression): KtCall? = withValidityAssertion {
        return resolveCall(call, isUsualCall = false)
    }

    override fun resolveCall(call: KtArrayAccessExpression): KtCall? = withValidityAssertion {
        return resolveCall(call, isUsualCall = false)
    }

    /**
     * Analyze the given call element (a function call, unary/binary operator call, or convention call).
     *
     * @param call the call element to analyze.
     * @param isUsualCall `true` if the call is a usual function call (`foo()` or `foo {}`).
     */
    private fun resolveCall(call: KtElement, isUsualCall: Boolean): KtCall? {
        val bindingContext = analysisSession.analyze(call, KtFe10AnalysisSession.AnalysisMode.PARTIAL_WITH_DIAGNOSTICS)
        val resolvedCall = call.getResolvedCall(bindingContext) ?: return getUnresolvedCall(call)

        val argumentMapping = createArgumentMapping(resolvedCall)

        fun getTarget(targetSymbol: KtFunctionLikeSymbol): KtCallTarget {
            if (resolvedCall.status == ResolutionStatus.SUCCESS) {
                return KtSuccessCallTarget(targetSymbol, token)
            }

            val diagnostic = KtNonBoundToPsiErrorDiagnostic(factoryName = null, UNRESOLVED_CALL_MESSAGE, token)
            return KtErrorCallTarget(listOf(targetSymbol), diagnostic, token)
        }

        val targetDescriptor = resolvedCall.resultingDescriptor

        val callableSymbol = targetDescriptor.toKtCallableSymbol(analysisSession) as? KtFunctionLikeSymbol ?: return null

        if (resolvedCall is VariableAsFunctionResolvedCall) {
            val variableDescriptor = resolvedCall.variableCall.resultingDescriptor
            val variableSymbol = variableDescriptor.toKtCallableSymbol(analysisSession) as? KtVariableLikeSymbol ?: return null

            val substitutor = KtSubstitutor.Empty(token)
            return if (resolvedCall.functionCall.resultingDescriptor.callableId in kotlinFunctionInvokeCallableIds) {
                KtFunctionalTypeVariableCall(variableSymbol, argumentMapping, getTarget(callableSymbol), substitutor, token)
            } else {
                KtVariableWithInvokeFunctionCall(variableSymbol, argumentMapping, getTarget(callableSymbol), substitutor, token)
            }
        }

        if (call is KtConstructorDelegationCall) {
            return KtDelegatedConstructorCall(argumentMapping, getTarget(callableSymbol), call.kind, token)
        }

        if (isUsualCall) {
            if (targetDescriptor.isAnnotationConstructor()) {
                return KtAnnotationCall(argumentMapping, getTarget(callableSymbol), token)
            }
        }

        return KtFunctionCall(argumentMapping, getTarget(callableSymbol), KtSubstitutor.Empty(token), token)
    }

    private fun getUnresolvedCall(call: KtElement): KtCall? {
        return when (call) {
            is KtSuperTypeCallEntry -> {
                val diagnostic = KtNonBoundToPsiErrorDiagnostic(factoryName = null, UNRESOLVED_CALL_MESSAGE, token)
                val target = KtErrorCallTarget(emptyList(), diagnostic, token)
                KtDelegatedConstructorCall(LinkedHashMap(), target, KtDelegatedConstructorCallKind.SUPER_CALL, token)
            }
            is KtConstructorDelegationCall -> {
                val diagnostic = KtNonBoundToPsiErrorDiagnostic(factoryName = null, UNRESOLVED_CALL_MESSAGE, token)
                val target = KtErrorCallTarget(emptyList(), diagnostic, token)
                return KtDelegatedConstructorCall(LinkedHashMap(), target, call.kind, token)
            }
            else -> null
        }
    }

    private val KtConstructorDelegationCall.kind: KtDelegatedConstructorCallKind
        get() = when {
            isCallToThis -> KtDelegatedConstructorCallKind.THIS_CALL
            else -> KtDelegatedConstructorCallKind.SUPER_CALL
        }

    private fun createArgumentMapping(resolvedCall: ResolvedCall<*>): LinkedHashMap<KtExpression, KtValueParameterSymbol> {
        val result = LinkedHashMap<KtExpression, KtValueParameterSymbol>()
        for ((parameter, arguments) in resolvedCall.valueArguments) {
            val parameterSymbol = KtFe10DescValueParameterSymbol(parameter, analysisSession)

            for (argument in arguments.arguments) {
                val expression = argument.getArgumentExpression() ?: continue
                result[expression] = parameterSymbol
            }
        }
        return result
    }
}