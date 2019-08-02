/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.constructFunctionalTypeRef
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substituteOrNull
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirTypeProjectionWithVarianceImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.types.Variance

class FirCallCompletionResultsWriterTransformer(
    override val session: FirSession,
    private val finalSubstitutor: ConeSubstitutor,
    private val typeCalculator: ReturnTypeCalculator
) : FirAbstractTreeTransformer(phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        val calleeReference =
            qualifiedAccessExpression.calleeReference as? FirNamedReferenceWithCandidate ?: return qualifiedAccessExpression.compose()
        calleeReference.candidate.substitutor

        val typeRef = typeCalculator.tryCalculateReturnType(calleeReference.candidateSymbol.phasedFir as FirTypedDeclaration)

        val initialType = calleeReference.candidate.substitutor.substituteOrNull(typeRef.type)
        val finalType = finalSubstitutor.substituteOrNull(initialType)

        val resultType = typeRef.withReplacedConeType(finalType)
        qualifiedAccessExpression.replaceTypeRef(resultType)

        return qualifiedAccessExpression.transformCalleeReference(
            StoreCalleeReference,
            FirResolvedCallableReferenceImpl(
                calleeReference.psi,
                calleeReference.name,
                calleeReference.candidateSymbol
            )
        ).compose()
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: Nothing?
    ): CompositeTransformResult<FirStatement> {
        val calleeReference = variableAssignment.calleeReference as? FirNamedReferenceWithCandidate
            ?: return variableAssignment.compose()
        return variableAssignment.transformCalleeReference(
            StoreCalleeReference,
            FirResolvedCallableReferenceImpl(
                calleeReference.psi,
                calleeReference.name,
                calleeReference.candidateSymbol
            )
        ).compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Nothing?): CompositeTransformResult<FirStatement> {
        val calleeReference = functionCall.calleeReference as? FirNamedReferenceWithCandidate ?: return functionCall.compose()
        val functionCall = functionCall.transformArguments(this, data) as FirFunctionCall

        val subCandidate = calleeReference.candidate
        val declaration = subCandidate.symbol.phasedFir as FirCallableMemberDeclaration<*>
        val newTypeParameters = declaration.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }
            .map { subCandidate.substitutor.substituteOrSelf(it) }
            .map { finalSubstitutor.substituteOrSelf(it) }
            .mapIndexed { index, type ->
                when (val argument = functionCall.typeArguments.getOrNull(index)) {
                    is FirTypeProjectionWithVariance -> {
                        val typeRef = argument.typeRef as FirResolvedTypeRef
                        FirTypeProjectionWithVarianceImpl(
                            argument.psi,
                            argument.variance,
                            typeRef.withReplacedConeType(type)
                        )
                    }
                    else -> {
                        FirTypeProjectionWithVarianceImpl(
                            argument?.psi,
                            Variance.INVARIANT,
                            FirResolvedTypeRefImpl(null, type, emptyList())
                        )
                    }
                }
            }

        val typeRef = typeCalculator.tryCalculateReturnType(declaration)

        val initialType = subCandidate.substitutor.substituteOrNull(typeRef.type)
        val finalType = finalSubstitutor.substituteOrNull(initialType)

        val resultType = typeRef.withReplacedConeType(finalType)

        return functionCall.copy(
            resultType = resultType,
            typeArguments = newTypeParameters,
            calleeReference = FirResolvedCallableReferenceImpl(
                calleeReference.psi,
                calleeReference.name,
                calleeReference.candidateSymbol
            )
        ).compose()

    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        val initialType = anonymousFunction.returnTypeRef.coneTypeSafe<ConeKotlinType>()
        if (initialType != null) {
            val finalType = finalSubstitutor.substituteOrNull(initialType)

            val resultType = anonymousFunction.returnTypeRef.withReplacedConeType(finalType)

            anonymousFunction.transformReturnTypeRef(StoreType, resultType)

            anonymousFunction.replaceTypeRef(anonymousFunction.constructFunctionalTypeRef(session))
        }
        return super.transformAnonymousFunction(anonymousFunction, data)
    }

    override fun transformBlock(block: FirBlock, data: Nothing?): CompositeTransformResult<FirStatement> {
        val initialType = block.resultType.coneTypeSafe<ConeKotlinType>()
        if (initialType != null) {
            val finalType = finalSubstitutor.substituteOrNull(initialType)
            val resultType = block.resultType.withReplacedConeType(finalType)
            block.replaceTypeRef(resultType)
        }
        return super.transformBlock(block, data)
    }

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: Nothing?): CompositeTransformResult<FirStatement> {
        val calleeReference = whenExpression.calleeReference as? FirNamedReferenceWithCandidate ?: return whenExpression.compose()

        val whenExpression = whenExpression.transformChildren(this, data) as FirWhenExpression

        val declaration = whenExpression.candidate()?.symbol?.fir as? FirMemberFunction<*> ?: return whenExpression.compose()

        val subCandidate = calleeReference.candidate

        val typeRef = typeCalculator.tryCalculateReturnType(declaration)

        val initialType = subCandidate.substitutor.substituteOrNull(typeRef.type)
        val finalType = finalSubstitutor.substituteOrNull(initialType)

        val resultType = typeRef.withReplacedConeType(finalType)

        return whenExpression.copy(
            resultType = resultType,
            calleeReference = FirResolvedCallableReferenceImpl(
                calleeReference.psi,
                calleeReference.name,
                calleeReference.candidateSymbol
            )
        ).compose()
    }

    override fun transformTryExpression(tryExpression: FirTryExpression, data: Nothing?): CompositeTransformResult<FirStatement> {
        val calleeReference = tryExpression.calleeReference as? FirNamedReferenceWithCandidate ?: return tryExpression.compose()

        val tryExpression = tryExpression.transformChildren(this, data) as FirTryExpression

        val declaration = tryExpression.candidate()?.symbol?.fir as? FirMemberFunction<*> ?: return tryExpression.compose()

        val subCandidate = calleeReference.candidate

        val typeRef = typeCalculator.tryCalculateReturnType(declaration)

        val initialType = subCandidate.substitutor.substituteOrNull(typeRef.type)
        val finalType = finalSubstitutor.substituteOrNull(initialType)

        val resultType = typeRef.withReplacedConeType(finalType)

        return tryExpression.copy(
            resultType = resultType,
            calleeReference = FirResolvedCallableReferenceImpl(
                calleeReference.psi,
                calleeReference.name,
                calleeReference.candidateSymbol
            )
        ).compose()

    }
}