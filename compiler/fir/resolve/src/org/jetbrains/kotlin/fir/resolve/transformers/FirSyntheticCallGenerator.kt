/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.addDefaultBoundIfNecessary
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirSimpleFunctionImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirTypeParameterImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirFunctionCallImpl
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.impl.FirStubReference
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.withNullability
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.SyntheticCallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeProjectionWithVarianceImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.types.Variance

class FirSyntheticCallGenerator(
    private val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val callCompleter: FirCallCompleter
) : BodyResolveComponents by components {
    private val whenSelectFunction: FirSimpleFunctionImpl = generateSyntheticSelectFunction(SyntheticCallableId.WHEN)
    private val trySelectFunction: FirSimpleFunctionImpl = generateSyntheticSelectFunction(SyntheticCallableId.TRY)
    private val idFunction: FirSimpleFunctionImpl = generateSyntheticSelectFunction(SyntheticCallableId.ID)
    private val checkNotNullFunction: FirSimpleFunctionImpl = generateSyntheticCheckNotNullFunction()

    fun generateCalleeForWhenExpression(whenExpression: FirWhenExpression): FirWhenExpression? {
        val stubReference = whenExpression.calleeReference
        // TODO: Investigate: assertion failed in ModularizedTest
        // assert(stubReference is FirStubReference)
        if (stubReference !is FirStubReference) return null

        val arguments = whenExpression.branches.map { it.result }
        val reference = generateCalleeReferenceWithCandidate(
            whenSelectFunction,
            arguments,
            SyntheticCallableId.WHEN.callableName
        ) ?: return null // TODO

        return whenExpression.transformCalleeReference(UpdateReference, reference)
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

        return tryExpression.transformCalleeReference(UpdateReference, reference)
    }

    fun generateCalleeForCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall): FirCheckNotNullCall? {
        val stubReference = checkNotNullCall.calleeReference
        if (stubReference !is FirStubReference) return null

        val reference = generateCalleeReferenceWithCandidate(
            checkNotNullFunction,
            checkNotNullCall.arguments,
            SyntheticCallableId.CHECK_NOT_NULL.callableName
        ) ?: return null // TODO

        return checkNotNullCall.transformCalleeReference(UpdateReference, reference)
    }

    fun resolveCallableReferenceWithSyntheticOuterCall(
        callableReferenceAccess: FirCallableReferenceAccess,
        expectedTypeRef: FirTypeRef?
    ): FirCallableReferenceAccess? {
        val arguments = listOf(callableReferenceAccess)

        val reference =
            generateCalleeReferenceWithCandidate(
                idFunction, arguments, SyntheticCallableId.ID.callableName, CallKind.SyntheticIdForCallableReferencesResolution
            ) ?: return null
        val fakeCallElement = FirFunctionCallImpl(null).copy(calleeReference = reference, arguments = arguments)

        return callCompleter.completeCall(fakeCallElement, expectedTypeRef).arguments[0] as FirCallableReferenceAccess?
    }

    private fun generateCalleeReferenceWithCandidate(
        function: FirSimpleFunctionImpl,
        arguments: List<FirExpression>,
        name: Name,
        callKind: CallKind = CallKind.SyntheticSelect
    ): FirNamedReferenceWithCandidate? {
        val callInfo = generateCallInfo(arguments, callKind)
        val candidate = generateCandidate(callInfo, function)
        val applicability = resolutionStageRunner.processCandidate(candidate)
        if (applicability <= CandidateApplicability.INAPPLICABLE) {
            return null
        }

        return FirNamedReferenceWithCandidate(null, name, candidate)
    }

    private fun generateCandidate(callInfo: CallInfo, function: FirSimpleFunctionImpl): Candidate =
        CandidateFactory(components, callInfo).createCandidate(
            symbol = function.symbol,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        )

    private fun generateCallInfo(arguments: List<FirExpression>, callKind: CallKind) = CallInfo(
        callKind = callKind,
        explicitReceiver = null,
        arguments = arguments,
        isSafeCall = false,
        typeArguments = emptyList(),
        session = session,
        containingFile = file,
        implicitReceiverStack = implicitReceiverStack
    ) { it.resultType }

    private fun generateSyntheticSelectFunction(callableId: CallableId): FirSimpleFunctionImpl {
        // Synthetic function signature:
        //   fun <K> select(vararg values: K): K
        val functionSymbol = FirSyntheticFunctionSymbol(callableId)
        val typeParameterSymbol = FirTypeParameterSymbol()
        val typeParameter =
            FirTypeParameterImpl(null, session, Name.identifier("K"), typeParameterSymbol, Variance.INVARIANT, false).apply {
                addDefaultBoundIfNecessary()
            }

        val returnType = FirResolvedTypeRefImpl(null, ConeTypeParameterTypeImpl(typeParameterSymbol.toLookupTag(), false))

        val argumentType = FirResolvedTypeRefImpl(null, returnType.coneTypeUnsafe<ConeKotlinType>().createArrayOf(session))
        val typeArgument = FirTypeProjectionWithVarianceImpl(null, returnType, Variance.INVARIANT)

        return generateMemberFunction(session, functionSymbol, callableId.callableName, typeArgument.typeRef).apply {
            typeParameters += typeParameter
            valueParameters += argumentType.toValueParameter(session, "branches", isVararg = true)
        }
    }

    private fun generateSyntheticCheckNotNullFunction(): FirSimpleFunctionImpl {
        // Synthetic function signature:
        //   fun <K> checkNotNull(arg: K?): K
        //
        // Note: The upper bound of `K` cannot be `Any` because of the following case:
        //   fun <X> test(a: X) = a!!
        // `X` is not a subtype of `Any` and hence cannot satisfy `K` if it had an upper bound of `Any`.
        val functionSymbol = FirSyntheticFunctionSymbol(SyntheticCallableId.CHECK_NOT_NULL)
        val typeParameterSymbol = FirTypeParameterSymbol()
        val typeParameter =
            FirTypeParameterImpl(null, session, Name.identifier("K"), typeParameterSymbol, Variance.INVARIANT, false).apply {
                addDefaultBoundIfNecessary()
            }

        val returnType = FirResolvedTypeRefImpl(null, ConeTypeParameterTypeImpl(typeParameterSymbol.toLookupTag(), false))

        val argumentType =
            FirResolvedTypeRefImpl(
                null, returnType.coneTypeUnsafe<ConeKotlinType>().withNullability(
                    ConeNullability.NULLABLE, session.inferenceContext
                )
            )
        val typeArgument = FirTypeProjectionWithVarianceImpl(null, returnType, Variance.INVARIANT)

        return generateMemberFunction(
            session,
            functionSymbol,
            SyntheticCallableId.CHECK_NOT_NULL.callableName,
            typeArgument.typeRef
        ).apply {
            typeParameters += typeParameter
            valueParameters += argumentType.toValueParameter(session, "arg")
        }
    }

    private fun generateMemberFunction(session: FirSession, symbol: FirNamedFunctionSymbol, name: Name, returnType: FirTypeRef): FirSimpleFunctionImpl {
        val status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL).apply {
            isExpect = false
            isActual = false
            isOverride = false
            isOperator = false
            isInfix = false
            isInline = false
            isTailRec = false
            isExternal = false
            isSuspend = false
        }
        return FirSimpleFunctionImpl(
            session = session,
            source = null,
            symbol = symbol,
            name = name,
            status = status,
            receiverTypeRef = null,
            returnTypeRef = returnType
        ).apply {
            this.resolvePhase = FirResolvePhase.BODY_RESOLVE
        }
    }

    private fun FirResolvedTypeRef.toValueParameter(session: FirSession, name: String, isVararg: Boolean = false): FirValueParameterImpl {
        val name = Name.identifier(name)
        return FirValueParameterImpl(
            session = session,
            source = null,
            name = name,
            returnTypeRef = this,
            defaultValue = null,
            isCrossinline = false,
            isNoinline = false,
            isVararg = isVararg,
            symbol = FirVariableSymbol(name)
        ).apply {
            this.resolvePhase = FirResolvePhase.BODY_RESOLVE
        }
    }
}

private object UpdateReference : FirTransformer<FirNamedReferenceWithCandidate>() {
    override fun <E : FirElement> transformElement(element: E, data: FirNamedReferenceWithCandidate): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformReference(reference: FirReference, data: FirNamedReferenceWithCandidate): CompositeTransformResult<FirReference> {
        return data.compose()
    }
}