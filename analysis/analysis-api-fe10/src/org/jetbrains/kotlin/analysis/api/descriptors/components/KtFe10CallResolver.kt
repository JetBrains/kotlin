/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.calls.KtCallInfo
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.AbstractKtCallResolver
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.psi.KtElement

internal class KtFe10CallResolver(
    override val analysisSession: KtFe10AnalysisSession
) : AbstractKtCallResolver(), Fe10KtAnalysisSessionComponent {
    private companion object {
        private const val UNRESOLVED_CALL_MESSAGE = "Unresolved call"
    }

    override val token: ValidityToken
        get() = analysisSession.token

    override fun resolveCall(psi: KtElement): KtCallInfo? {
        return null
    }

//    override fun resolveAccessorCall(call: KtSimpleNameExpression): KtCall? {
//        val bindingContext = analysisContext.analyze(call, AnalysisMode.PARTIAL_WITH_DIAGNOSTICS)
//        val resolvedCall = call.getResolvedCall(bindingContext) ?: return null
//        val targetDescriptor = resolvedCall.candidateDescriptor
//
//        if (targetDescriptor is PropertyDescriptor) {
//            @Suppress("DEPRECATION")
//            val access = call.readWriteAccessWithFullExpressionWithPossibleResolve(
//                readWriteAccessWithFullExpressionByResolve = { null }
//            ).first
//
//            val setterValue = findAssignment(call)?.right
//            val accessorSymbol = when (targetDescriptor) {
//                is SyntheticJavaPropertyDescriptor -> {
//                    when {
//                        access.isWrite -> targetDescriptor.setMethod?.let { KtFe10DescFunctionSymbol.build(it, analysisContext) }
//                        access.isRead -> KtFe10DescFunctionSymbol.build(targetDescriptor.getMethod, analysisContext)
//                        else -> null
//                    }
//                }
//                else -> {
//                    when {
//                        access.isWrite -> targetDescriptor.setter?.let { KtFe10DescPropertySetterSymbol(it, analysisContext) }
//                        access.isRead -> targetDescriptor.getter?.let { KtFe10DescPropertyGetterSymbol(it, analysisContext) }
//                        else -> null
//                    }
//                }
//            }
//
//            if (accessorSymbol != null) {
//                val target = when {
//                    !access.isWrite || setterValue != null -> KtSuccessCallTarget(accessorSymbol, token)
//                    else -> {
//                        val diagnostic = KtNonBoundToPsiErrorDiagnostic(factoryName = null, "Setter value is missing", token)
//                        KtErrorCallTarget(listOf(accessorSymbol), diagnostic, token)
//                    }
//                }
//
//                val argumentMapping = LinkedHashMap<KtExpression, KtValueParameterSymbol>()
//                if (access.isWrite && setterValue != null) {
//                    val setterParameterSymbol = accessorSymbol.valueParameters.single()
//                    argumentMapping[setterValue] = setterParameterSymbol
//                }
//
//                return KtFunctionCall(argumentMapping, target, getSubstitutor(resolvedCall), token)
//            }
//        }
//
//        return null
//    }
//
//    override fun resolveCall(call: KtCallElement): KtCall? = withValidityAssertion {
//        return resolveCall(call, isUsualCall = true)
//    }
//
//    override fun resolveCall(call: KtBinaryExpression): KtCall? = withValidityAssertion {
//        return resolveCall(call, isUsualCall = false)
//    }
//
//    override fun resolveCall(call: KtUnaryExpression): KtCall? = withValidityAssertion {
//        return resolveCall(call, isUsualCall = false)
//    }
//
//    override fun resolveCall(call: KtArrayAccessExpression): KtCall? = withValidityAssertion {
//        return resolveCall(call, isUsualCall = false)
//    }
//
//    private fun getSubstitutor(vararg resolvedCall: ResolvedCall<*>): KtSubstitutor {
//        val typeArguments = if (resolvedCall.size == 1) {
//            resolvedCall[0].typeArguments
//        } else {
//            buildMap {
//                resolvedCall.forEach { putAll(it.typeArguments) }
//            }
//        }
//
//        if (typeArguments.isEmpty()) {
//            return KtSubstitutor.Empty(analysisContext.token)
//        }
//
//        val typeSubstitution = object : TypeConstructorSubstitution() {
//            override fun get(key: TypeConstructor): TypeProjection? {
//                val type = typeArguments[key.declarationDescriptor] ?: return null
//                return TypeProjectionImpl(Variance.INVARIANT, type)
//            }
//
//            override fun isEmpty() = typeArguments.isEmpty()
//        }
//
//        val typeSubstitutor = TypeSubstitutor.create(typeSubstitution)
//
//        return object : KtMapBackedSubstitutor {
//            override val token: ValidityToken
//                get() = analysisContext.token
//
//            val map: Map<KtTypeParameterSymbol, KtType> by cached {
//                val symbolicMap = LinkedHashMap<KtTypeParameterSymbol, KtType>(typeArguments.size)
//                for ((typeParameter, type) in typeArguments) {
//                    val typeParameterSymbol = KtFe10DescTypeParameterSymbol(typeParameter, analysisContext)
//                    symbolicMap[typeParameterSymbol] = type.toKtType(analysisContext)
//                }
//                return@cached symbolicMap
//            }
//
//            override fun getAsMap(): Map<KtTypeParameterSymbol, KtType> {
//                return map
//            }
//
//            override fun substituteOrNull(type: KtType): KtType {
//                require(type is KtFe10Type)
//                return typeSubstitutor.substitute(type.type).toKtType(analysisContext)
//            }
//        }
//    }
//
//    /**
//     * Analyze the given call element (a function call, unary/binary operator call, or convention call).
//     *
//     * @param call the call element to analyze.
//     * @param isUsualCall `true` if the call is a usual function call (`foo()` or `foo {}`).
//     */
//    private fun resolveCall(call: KtElement, isUsualCall: Boolean): KtCall? {
//        val bindingContext = analysisContext.analyze(call, AnalysisMode.PARTIAL_WITH_DIAGNOSTICS)
//        val resolvedCall = call.getResolvedCall(bindingContext) ?: return getUnresolvedCall(call)
//
//        val argumentMapping = createArgumentMapping(resolvedCall)
//
//        fun getTarget(targetSymbol: KtFunctionLikeSymbol): KtCallTarget {
//            if (resolvedCall.status == ResolutionStatus.SUCCESS) {
//                return KtSuccessCallTarget(targetSymbol, token)
//            }
//
//            val diagnostic = KtNonBoundToPsiErrorDiagnostic(factoryName = null, UNRESOLVED_CALL_MESSAGE, token)
//            return KtErrorCallTarget(listOf(targetSymbol), diagnostic, token)
//        }
//
//        val targetDescriptor = resolvedCall.candidateDescriptor
//
//        val callableSymbol = targetDescriptor.toKtCallableSymbol(analysisContext) as? KtFunctionLikeSymbol ?: return null
//
//        if (resolvedCall is VariableAsFunctionResolvedCall) {
//            val variableDescriptor = resolvedCall.variableCall.candidateDescriptor
//            val variableSymbol = variableDescriptor.toKtCallableSymbol(analysisContext) as? KtVariableLikeSymbol ?: return null
//
//            val substitutor = getSubstitutor(resolvedCall.functionCall, resolvedCall.variableCall)
//            return if (resolvedCall.functionCall.candidateDescriptor.callableIdIfNotLocal in kotlinFunctionInvokeCallableIds) {
//                KtFunctionalTypeVariableCall(variableSymbol, argumentMapping, getTarget(callableSymbol), substitutor, token)
//            } else {
//                KtVariableWithInvokeFunctionCall(variableSymbol, argumentMapping, getTarget(callableSymbol), substitutor, token)
//            }
//        }
//
//        if (call is KtConstructorDelegationCall) {
//            return KtDelegatedConstructorCall(argumentMapping, getTarget(callableSymbol), call.kind, token)
//        }
//
//        if (isUsualCall) {
//            if (targetDescriptor.isAnnotationConstructor() && (call is KtAnnotationEntry || call.parent is KtAnnotationEntry)) {
//                return KtAnnotationCall(argumentMapping, getTarget(callableSymbol), token)
//            }
//        }
//
//        return KtFunctionCall(argumentMapping, getTarget(callableSymbol), getSubstitutor(resolvedCall), token)
//    }
//
//    private fun getUnresolvedCall(call: KtElement): KtCall? {
//        return when (call) {
//            is KtSuperTypeCallEntry -> {
//                val diagnostic = KtNonBoundToPsiErrorDiagnostic(factoryName = null, UNRESOLVED_CALL_MESSAGE, token)
//                val target = KtErrorCallTarget(emptyList(), diagnostic, token)
//                KtDelegatedConstructorCall(LinkedHashMap(), target, KtDelegatedConstructorCallKind.SUPER_CALL, token)
//            }
//            is KtConstructorDelegationCall -> {
//                val diagnostic = KtNonBoundToPsiErrorDiagnostic(factoryName = null, UNRESOLVED_CALL_MESSAGE, token)
//                val target = KtErrorCallTarget(emptyList(), diagnostic, token)
//                return KtDelegatedConstructorCall(LinkedHashMap(), target, call.kind, token)
//            }
//            else -> null
//        }
//    }
//
//    private val KtConstructorDelegationCall.kind: KtDelegatedConstructorCallKind
//        get() = when {
//            isCallToThis -> KtDelegatedConstructorCallKind.THIS_CALL
//            else -> KtDelegatedConstructorCallKind.SUPER_CALL
//        }
//
//    private fun createArgumentMapping(resolvedCall: ResolvedCall<*>): LinkedHashMap<KtExpression, KtValueParameterSymbol> {
//        val result = LinkedHashMap<KtExpression, KtValueParameterSymbol>()
//        for ((parameter, arguments) in resolvedCall.valueArguments) {
//            val parameterSymbol = KtFe10DescValueParameterSymbol(parameter, analysisContext)
//
//            for (argument in arguments.arguments) {
//                val expression = argument.getArgumentExpression() ?: continue
//                result[expression] = parameterSymbol
//            }
//        }
//        return result
//    }
}