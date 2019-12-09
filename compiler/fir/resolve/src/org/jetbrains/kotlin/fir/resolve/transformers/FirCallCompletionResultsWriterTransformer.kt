/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.impl.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.references.impl.FirResolvedNamedReferenceImpl
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.calls.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.constructFunctionalTypeRef
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substituteOrNull
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.withNullability
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperatorCall
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeProjectionWithVarianceImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.Variance

class FirCallCompletionResultsWriterTransformer(
    override val session: FirSession,
    private val finalSubstitutor: ConeSubstitutor,
    private val typeCalculator: ReturnTypeCalculator,
    private val typeApproximator: AbstractTypeApproximator,
    private val integerOperatorsTypeUpdater: IntegerOperatorsTypeUpdater,
    private val integerApproximator: IntegerLiteralTypeApproximationTransformer
) : FirAbstractTreeTransformer<ExpectedArgumentType?>(phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        val calleeReference =
            qualifiedAccessExpression.calleeReference as? FirNamedReferenceWithCandidate ?: return qualifiedAccessExpression.compose()
        calleeReference.candidate.substitutor

        val candidateFir = calleeReference.candidateSymbol.phasedFir
        val typeRef = (candidateFir as? FirTypedDeclaration)?.let {
            typeCalculator.tryCalculateReturnType(it)
        } ?: FirErrorTypeRefImpl(
            calleeReference.source,
            FirSimpleDiagnostic("Callee reference to candidate without return type: ${candidateFir.render()}")
        )

        qualifiedAccessExpression.replaceTypeRefWithSubstituted(calleeReference, typeRef)

        return qualifiedAccessExpression.transformCalleeReference(
            StoreCalleeReference,
            FirResolvedNamedReferenceImpl(
                calleeReference.source,
                calleeReference.name,
                calleeReference.candidateSymbol
            )
        ).compose()
    }

    private fun <D : FirExpression> D.replaceTypeRefWithSubstituted(
        calleeReference: FirNamedReferenceWithCandidate,
        typeRef: FirResolvedTypeRef
    ): D {
        val resultTypeRef = typeRef.substituteTypeRef(calleeReference.candidate)
        replaceTypeRef(resultTypeRef)
        return this
    }

    private fun FirResolvedTypeRef.substituteTypeRef(
        candidate: Candidate
    ): FirResolvedTypeRef {
        val initialType = candidate.substitutor.substituteOrNull(type)
        val finalType = finalSubstitutor.substituteOrNull(initialType)?.let { substitutedType ->
            typeApproximator.approximateToSuperType(
                substitutedType, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
            ) as ConeKotlinType? ?: substitutedType
        }

        return withReplacedConeType(finalType)
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        val calleeReference =
            callableReferenceAccess.calleeReference as? FirNamedReferenceWithCandidate ?: return callableReferenceAccess.compose()

        val typeRef = callableReferenceAccess.typeRef as FirResolvedTypeRef

        val initialType = calleeReference.candidate.substitutor.substituteOrSelf(typeRef.type)
        val finalType = finalSubstitutor.substituteOrSelf(initialType)

        val resultType = typeRef.withReplacedConeType(finalType)
        callableReferenceAccess.replaceTypeRef(resultType)

        return callableReferenceAccess.transformCalleeReference(
            StoreCalleeReference,
            FirResolvedCallableReferenceImpl(
                calleeReference.source,
                calleeReference.name,
                calleeReference.candidateSymbol
            ).apply {
                inferredTypeArguments.addAll(computeTypeArguments(calleeReference.candidate))
            }
        ).compose()
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        val calleeReference = variableAssignment.calleeReference as? FirNamedReferenceWithCandidate
            ?: return variableAssignment.compose()
        return variableAssignment.transformCalleeReference(
            StoreCalleeReference,
            FirResolvedNamedReferenceImpl(
                calleeReference.source,
                calleeReference.name,
                calleeReference.candidateSymbol
            )
        ).compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: ExpectedArgumentType?): CompositeTransformResult<FirStatement> {
        val calleeReference = functionCall.calleeReference as? FirNamedReferenceWithCandidate ?: return functionCall.compose()

        val subCandidate = calleeReference.candidate
        val declaration = subCandidate.symbol.phasedFir as FirCallableMemberDeclaration<*>
        val typeArguments = computeTypeArguments(subCandidate)
            .mapIndexed { index, type ->
                when (val argument = functionCall.typeArguments.getOrNull(index)) {
                    is FirTypeProjectionWithVariance -> {
                        val typeRef = argument.typeRef as FirResolvedTypeRef
                        FirTypeProjectionWithVarianceImpl(
                            argument.source,
                            typeRef.withReplacedConeType(type),
                            argument.variance
                        )
                    }
                    else -> {
                        FirTypeProjectionWithVarianceImpl(
                            argument?.source,
                            FirResolvedTypeRefImpl(null, type),
                            Variance.INVARIANT
                        )
                    }
                }
            }

        val typeRef = typeCalculator.tryCalculateReturnType(declaration).let {
            if (functionCall.safe) {
                val nullableType = it.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NULLABLE)
                it.withReplacedConeType(nullableType)
            } else {
                it
            }
        }

        var result = functionCall.transformSingle(integerOperatorsTypeUpdater, null)
            .transformCalleeReference(
                StoreCalleeReference,
                FirResolvedNamedReferenceImpl(
                    calleeReference.source,
                    calleeReference.name,
                    calleeReference.candidateSymbol
                )
            )
            .transformDispatchReceiver(StoreReceiver, subCandidate.dispatchReceiverExpression())
            .transformExtensionReceiver(StoreReceiver, subCandidate.extensionReceiverExpression())
        val resultType: FirTypeRef
        result = when (result) {
            is FirIntegerOperatorCall -> {
                val expectedType = data?.getExpectedType(functionCall)
                resultType = typeRef.resolvedTypeFromPrototype(typeRef.coneTypeUnsafe<ConeIntegerLiteralType>().getApproximatedType(expectedType))
                result.transformSingle(integerApproximator, expectedType)
            }
            else -> {
                resultType = typeRef.substituteTypeRef(subCandidate)
                result.transformArguments(this, subCandidate.createArgumentsMapping()).transformExplicitReceiver(integerApproximator, null)
            }
        }

        return result.copy(
            resultType = resultType,
            typeArguments = typeArguments
        ).compose()
    }

    private fun Candidate.createArgumentsMapping(): ExpectedArgumentType? {
        return argumentMapping?.map { (argument, valueParameter) ->
                val expectedType = valueParameter.returnTypeRef.coneTypeUnsafe<ConeKotlinType>()
                    .let { substitutor.substituteOrSelf(it) }
                    .let { finalSubstitutor.substituteOrSelf(it) }

                argument.expandArgument() to expectedType
            }
            ?.toMap()?.toExpectedType()
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        val calleeReference = delegatedConstructorCall.calleeReference as? FirNamedReferenceWithCandidate ?: return delegatedConstructorCall.compose()

        val result = delegatedConstructorCall.transformArguments(this, calleeReference.candidate.createArgumentsMapping())
        return result.transformCalleeReference(
            StoreCalleeReference,
            FirResolvedNamedReferenceImpl(
                calleeReference.source,
                calleeReference.name,
                calleeReference.candidateSymbol
            )
        ).compose()
    }

    private fun computeTypeArguments(
        candidate: Candidate
    ): List<ConeKotlinType> {
        val declaration = candidate.symbol.phasedFir as? FirCallableMemberDeclaration<*> ?: return emptyList()

        return declaration.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }
            .map { candidate.substitutor.substituteOrSelf(it) }
            .map { finalSubstitutor.substituteOrSelf(it) }
    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        val expectedReturnType = data?.getExpectedType(anonymousFunction)
            ?.takeIf { it.isBuiltinFunctionalType }
            ?.let { it.typeArguments.last() as? ConeClassLikeType }

        val initialType = anonymousFunction.returnTypeRef.coneTypeSafe<ConeKotlinType>()
        if (initialType != null) {
            val finalType = expectedReturnType ?: finalSubstitutor.substituteOrNull(initialType)

            val resultType = anonymousFunction.returnTypeRef.withReplacedConeType(finalType)

            anonymousFunction.transformReturnTypeRef(StoreType, resultType)

            anonymousFunction.replaceTypeRef(anonymousFunction.constructFunctionalTypeRef(session))
        }
        return transformElement(anonymousFunction, null)
    }

    override fun transformBlock(block: FirBlock, data: ExpectedArgumentType?): CompositeTransformResult<FirStatement> {
        val initialType = block.resultType.coneTypeSafe<ConeKotlinType>()
        if (initialType != null) {
            val finalType = finalSubstitutor.substituteOrNull(initialType)
            var resultType = block.resultType.withReplacedConeType(finalType)
            resultType.coneTypeSafe<ConeIntegerLiteralType>()?.let {
                resultType = resultType.resolvedTypeFromPrototype(it.getApproximatedType(data?.getExpectedType(block)))
            }
            block.replaceTypeRef(resultType)
        }
        return transformElement(block, data)
    }

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: ExpectedArgumentType?): CompositeTransformResult<FirStatement> {
        val calleeReference = whenExpression.calleeReference as? FirNamedReferenceWithCandidate ?: return whenExpression.compose()

        val whenExpression = whenExpression.transformChildren(this, data?.getExpectedType(whenExpression)?.toExpectedType()) as FirWhenExpression

        val declaration = whenExpression.candidate()?.symbol?.fir as? FirMemberFunction<*> ?: return whenExpression.compose()

        val subCandidate = calleeReference.candidate

        val typeRef = typeCalculator.tryCalculateReturnType(declaration)

        whenExpression.resultType = typeRef.substituteTypeRef(subCandidate)

        return whenExpression.transformCalleeReference(
            StoreCalleeReference,
            FirResolvedNamedReferenceImpl(
                calleeReference.source,
                calleeReference.name,
                calleeReference.candidateSymbol
            )
        ).compose()
    }

    override fun transformTryExpression(tryExpression: FirTryExpression, data: ExpectedArgumentType?): CompositeTransformResult<FirStatement> {
        val calleeReference = tryExpression.calleeReference as? FirNamedReferenceWithCandidate ?: return tryExpression.compose()

        val tryExpression = tryExpression.transformChildren(this, data) as FirTryExpression

        val declaration = tryExpression.candidate()?.symbol?.fir as? FirMemberFunction<*> ?: return tryExpression.compose()

        val subCandidate = calleeReference.candidate

        val typeRef = typeCalculator.tryCalculateReturnType(declaration)

        tryExpression.resultType = typeRef.substituteTypeRef(subCandidate)
        return tryExpression.transformCalleeReference(
            StoreCalleeReference,
            FirResolvedNamedReferenceImpl(
                calleeReference.source,
                calleeReference.name,
                calleeReference.candidateSymbol
            )
        ).compose()
    }

    override fun <T> transformConstExpression(
        constExpression: FirConstExpression<T>,
        data: ExpectedArgumentType?
    ): CompositeTransformResult<FirStatement> {
        if (data == ExpectedArgumentType.NoApproximation) return constExpression.compose()
        val expectedType = data?.getExpectedType(constExpression)
        return constExpression.transform(integerApproximator, expectedType)
    }
}

sealed class ExpectedArgumentType {
    class ArgumentsMap(val map: Map<FirExpression, ConeKotlinType>) : ExpectedArgumentType()
    class ExpectedType(val type: ConeKotlinType) : ExpectedArgumentType()
    object NoApproximation : ExpectedArgumentType()
}

private fun ExpectedArgumentType.getExpectedType(argument: FirExpression): ConeKotlinType? = when (this) {
    is ExpectedArgumentType.ArgumentsMap -> map[argument]
    is ExpectedArgumentType.ExpectedType -> type
    ExpectedArgumentType.NoApproximation -> null
}

private fun Map<FirExpression, ConeKotlinType>.toExpectedType(): ExpectedArgumentType = ExpectedArgumentType.ArgumentsMap(this)
fun ConeKotlinType.toExpectedType(): ExpectedArgumentType = ExpectedArgumentType.ExpectedType(this)

private fun FirExpression.expandArgument(): FirExpression = when (this) {
    is FirWrappedArgumentExpression -> expression
    else -> this
}

private fun ConeKotlinType.approximateIfPossible(expectedType: ConeKotlinType?) = if (this is ConeIntegerLiteralType) {
    getApproximatedType(expectedType)
} else {
    this
}