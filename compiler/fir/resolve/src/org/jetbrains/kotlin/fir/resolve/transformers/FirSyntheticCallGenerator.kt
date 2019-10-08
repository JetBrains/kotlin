/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.impl.FirMemberFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.references.FirStubReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.SyntheticCallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeProjectionWithVarianceImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.types.Variance

class FirSyntheticCallGenerator(private val transformer: FirBodyResolveTransformer) : BodyResolveComponents by transformer {
    private val whenSelectFunction: FirMemberFunctionImpl = generateSyntheticSelectFunction(SyntheticCallableId.WHEN)
    private val trySelectFunction: FirMemberFunctionImpl = generateSyntheticSelectFunction(SyntheticCallableId.TRY)

    fun generateCalleeForWhenExpression(whenExpression: FirWhenExpression): FirWhenExpression? {
        val stubReference = whenExpression.calleeReference
        assert(stubReference is FirStubReference)

        val arguments = whenExpression.branches.map { it.result }
        val reference = generateCalleeReferenceWithCandidate(
            whenSelectFunction,
            arguments,
            SyntheticCallableId.WHEN.callableName
        ) ?: return null // TODO

        return whenExpression.copy(calleeReference = reference)
    }

    fun generateCalleeForTryExpression(tryExpression: FirTryExpression): FirTryExpression? {
        val stubReference = tryExpression.calleeReference
        assert(stubReference is FirStubReference)

        val arguments = mutableListOf<FirExpression>()

        with(tryExpression) {
            arguments += tryBlock
            catches.forEach {
                arguments += it.block
            }
        }

        val reference = generateCalleeReferenceWithCandidate(
            trySelectFunction,
            arguments,
            SyntheticCallableId.TRY.callableName
        ) ?: return null // TODO

        return tryExpression.copy(calleeReference = reference)
    }

    private fun generateCalleeReferenceWithCandidate(
        function: FirMemberFunctionImpl,
        arguments: List<FirExpression>,
        name: Name
    ): FirNamedReferenceWithCandidate? {
        val callInfo = generateCallInfo(arguments)
        val candidate = generateCandidate(callInfo, function)
        val applicability = resolutionStageRunner.processCandidate(candidate)
        if (applicability <= CandidateApplicability.INAPPLICABLE) {
            return null
        }

        return FirNamedReferenceWithCandidate(null, name, candidate)
    }

    private fun generateCandidate(callInfo: CallInfo, function: FirMemberFunctionImpl): Candidate =
        CandidateFactory(transformer, callInfo).createCandidate(
            symbol = function.symbol,
            dispatchReceiverValue = null,
            implicitExtensionReceiverValue = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        )

    private fun generateCallInfo(arguments: List<FirExpression>) = CallInfo(
        callKind = CallKind.SyntheticSelect,
        explicitReceiver = null,
        arguments = arguments,
        isSafeCall = false,
        typeArguments = emptyList(),
        session = session,
        containingFile = file,
        container = container
    ) { it.resultType }

    private fun generateSyntheticSelectFunction(callableId: CallableId): FirMemberFunctionImpl {
        val functionSymbol = FirSyntheticFunctionSymbol(callableId)
        val typeParameterSymbol = FirTypeParameterSymbol()
        val typeParameter = FirTypeParameterImpl(session, null, typeParameterSymbol, Name.identifier("K"), Variance.INVARIANT, false)

        val returnType = FirResolvedTypeRefImpl(null, ConeTypeParameterTypeImpl(typeParameterSymbol.toLookupTag(), false))

        val argumentType = FirResolvedTypeRefImpl(null, returnType.coneTypeUnsafe<ConeKotlinType>().createArrayOf(session))
        val typeArgument = FirTypeProjectionWithVarianceImpl(null, Variance.INVARIANT, returnType)

        return generateMemberFunction(session, functionSymbol, callableId.callableName, typeArgument.typeRef).apply {
            typeParameters += typeParameter
            valueParameters += argumentType.toValueParameter(session, "branches", isVararg = true)
        }
    }

    private fun generateMemberFunction(session: FirSession, symbol: FirNamedFunctionSymbol, name: Name, returnType: FirTypeRef) =
        FirMemberFunctionImpl(
            session = session,
            psi = null,
            symbol = symbol,
            name = name,
            visibility = Visibilities.PUBLIC,
            modality = Modality.FINAL,
            isExpect = false,
            isActual = false,
            isOverride = false,
            isOperator = false,
            isInfix = false,
            isInline = false,
            isTailRec = false,
            isExternal = false,
            isSuspend = false,
            receiverTypeRef = null,
            returnTypeRef = returnType
        ).apply {
            this.resolvePhase = FirResolvePhase.BODY_RESOLVE
        }

    private fun FirResolvedTypeRef.toValueParameter(session: FirSession, name: String, isVararg: Boolean = false) = FirValueParameterImpl(
        session = session,
        psi = null,
        name = Name.identifier(name),
        returnTypeRef = this,
        defaultValue = null,
        isCrossinline = false,
        isNoinline = false,
        isVararg = isVararg
    ).apply {
        this.resolvePhase = FirResolvePhase.BODY_RESOLVE
    }
}
