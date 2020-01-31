/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirFunctionCallImpl
import org.jetbrains.kotlin.fir.references.builder.buildResolvedCallableReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.calls.returnType
import org.jetbrains.kotlin.fir.resolve.constructFunctionalTypeRef
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.substituteOrNull
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.withNullability
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperatorCall
import org.jetbrains.kotlin.fir.scopes.impl.withReplacedConeType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.types.AbstractTypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.Variance
import java.lang.Math.min

class FirCallCompletionResultsWriterTransformer(
    override val session: FirSession,
    private val finalSubstitutor: ConeSubstitutor,
    private val typeCalculator: ReturnTypeCalculator,
    private val typeApproximator: AbstractTypeApproximator,
    private val integerOperatorsTypeUpdater: IntegerOperatorsTypeUpdater,
    private val integerApproximator: IntegerLiteralTypeApproximationTransformer,
) : FirAbstractTreeTransformer<ExpectedArgumentType?>(phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE) {

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: ExpectedArgumentType?,
    ): CompositeTransformResult<FirStatement> {
        val calleeReference =
            qualifiedAccessExpression.calleeReference as? FirNamedReferenceWithCandidate ?: return qualifiedAccessExpression.compose()

        val candidateFir = calleeReference.candidateSymbol.phasedFir
        val typeRef = (candidateFir as? FirTypedDeclaration)?.let {
            typeCalculator.tryCalculateReturnType(it)
        } ?: buildErrorTypeRef {
            source = calleeReference.source
            diagnostic = FirSimpleDiagnostic("Callee reference to candidate without return type: ${candidateFir.render()}")
        }

        qualifiedAccessExpression.replaceTypeRefWithSubstituted(calleeReference, typeRef)

        return qualifiedAccessExpression.transformCalleeReference(
            StoreCalleeReference,
            buildResolvedNamedReference {
                source = calleeReference.source
                name = calleeReference.name
                resolvedSymbol = calleeReference.candidateSymbol
            },
        ).compose()
    }

    private fun <D : FirExpression> D.replaceTypeRefWithSubstituted(
        calleeReference: FirNamedReferenceWithCandidate,
        typeRef: FirResolvedTypeRef,
    ): D {
        val resultTypeRef = typeRef.substituteTypeRef(calleeReference.candidate)
        replaceTypeRef(resultTypeRef)
        return this
    }

    private fun FirResolvedTypeRef.substituteTypeRef(
        candidate: Candidate,
    ): FirResolvedTypeRef {
        val initialType = candidate.substitutor.substituteOrNull(type)
        val finalType = finalSubstitutor.substituteOrNull(initialType)?.let { substitutedType ->
            typeApproximator.approximateToSuperType(
                substitutedType, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference,
            ) as ConeKotlinType? ?: substitutedType
        }

        return withReplacedConeType(finalType)
    }

    override fun transformCallableReferenceAccess(
        callableReferenceAccess: FirCallableReferenceAccess,
        data: ExpectedArgumentType?,
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
            buildResolvedCallableReference {
                source = calleeReference.source
                name = calleeReference.name
                resolvedSymbol = calleeReference.candidateSymbol
                inferredTypeArguments.addAll(computeTypeArguments(calleeReference.candidate))
            },
        ).compose()
    }

    override fun transformVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: ExpectedArgumentType?,
    ): CompositeTransformResult<FirStatement> {
        val calleeReference = variableAssignment.calleeReference as? FirNamedReferenceWithCandidate
            ?: return variableAssignment.compose()
        return variableAssignment.transformCalleeReference(
            StoreCalleeReference,
            buildResolvedNamedReference {
                source = calleeReference.source
                name = calleeReference.name
                resolvedSymbol = calleeReference.candidateSymbol
            },
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
                        buildTypeProjectionWithVariance {
                            source = argument.source
                            this.typeRef = typeRef.withReplacedConeType(type)
                            variance = argument.variance
                        }
                    }
                    else -> {
                        buildTypeProjectionWithVariance {
                            source = argument?.source
                            typeRef = buildResolvedTypeRef {
                                this.type = type
                            }
                            variance = Variance.INVARIANT
                        }
                    }
                }
            }

        val typeRef = typeCalculator.tryCalculateReturnType(declaration).let {
            if (functionCall.safe) {
                val nullableType = it.coneTypeUnsafe<ConeKotlinType>().withNullability(ConeNullability.NULLABLE, session.inferenceContext)
                it.withReplacedConeType(nullableType)
            } else {
                it
            }
        }

        var result = functionCall.transformSingle(integerOperatorsTypeUpdater, null)
            .transformCalleeReference(
                StoreCalleeReference,
                buildResolvedNamedReference {
                    source = calleeReference.source
                    name = calleeReference.name
                    resolvedSymbol = calleeReference.candidateSymbol
                },
            )
            .transformDispatchReceiver(StoreReceiver, subCandidate.dispatchReceiverExpression())
            .transformExtensionReceiver(StoreReceiver, subCandidate.extensionReceiverExpression())
        val resultType: FirTypeRef
        result = when (result) {
            is FirIntegerOperatorCall -> {
                val expectedType = data?.getExpectedType(functionCall)
                resultType =
                    typeRef.resolvedTypeFromPrototype(typeRef.coneTypeUnsafe<ConeIntegerLiteralType>().getApproximatedType(expectedType))
                result.transformArguments(this, expectedType?.toExpectedType()).transformSingle(integerApproximator, expectedType)
            }
            else -> {
                resultType = typeRef.substituteTypeRef(subCandidate)
                val vararg = subCandidate.argumentMapping?.values?.firstOrNull { it.isVararg }
                result.transformArguments(this, subCandidate.createArgumentsMapping()).apply call@{
                    if (vararg != null && this is FirFunctionCallImpl) {
                        // Create a FirVarargArgumentExpression for the vararg arguments
                        val resolvedArrayType = vararg.returnTypeRef.substitute(subCandidate)
                        val resolvedElementType = resolvedArrayType.arrayElementType(session)
                        var firstIndex = this@call.arguments.size
                        val varargArgument = buildVarargArgumentsExpression {
                            varargElementType = vararg.returnTypeRef.withReplacedConeType(resolvedElementType)
                            this.typeRef = vararg.returnTypeRef.withReplacedConeType(
                                vararg.returnTypeRef.substitute(
                                    subCandidate
                                )
                            )
                            for ((i, arg) in this@call.arguments.withIndex()) {
                                if (subCandidate.argumentMapping!![arg]?.isVararg ?: false) {
                                    firstIndex = min(firstIndex, i)
                                    arguments += arg
                                }
                            }
                        }
                        for (arg in varargArgument.arguments) {
                            arguments.remove(arg)
                        }
                        arguments.add(firstIndex, varargArgument)
                    }
                }.transformExplicitReceiver(integerApproximator, null)
            }
        }

        return result.copy(
            resultType = resultType,
            typeArguments = typeArguments,
        ).compose()
    }

    private fun FirTypeRef.substitute(candidate: Candidate): ConeKotlinType =
        coneTypeUnsafe<ConeKotlinType>()
            .let { candidate.substitutor.substituteOrSelf(it) }
            .let { finalSubstitutor.substituteOrSelf(it) }

    private fun Candidate.createArgumentsMapping(): ExpectedArgumentType? {
        return argumentMapping?.map { (argument, valueParameter) ->
                val expectedType = valueParameter.returnTypeRef.substitute(this)
                argument.unwrapArgument() to expectedType
            }
            ?.toMap()?.toExpectedType()
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: ExpectedArgumentType?,
    ): CompositeTransformResult<FirStatement> {
        val calleeReference =
            delegatedConstructorCall.calleeReference as? FirNamedReferenceWithCandidate ?: return delegatedConstructorCall.compose()

        val result = delegatedConstructorCall.transformArguments(this, calleeReference.candidate.createArgumentsMapping())
        return result.transformCalleeReference(
            StoreCalleeReference,
            buildResolvedNamedReference {
                source = calleeReference.source
                name = calleeReference.name
                resolvedSymbol = calleeReference.candidateSymbol
            },
        ).compose()
    }

    private fun computeTypeArguments(
        candidate: Candidate,
    ): List<ConeKotlinType> {
        val declaration = candidate.symbol.phasedFir as? FirCallableMemberDeclaration<*> ?: return emptyList()

        return declaration.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }
            .map { candidate.substitutor.substituteOrSelf(it) }
            .map { finalSubstitutor.substituteOrSelf(it) }
    }

    override fun transformAnonymousFunction(
        anonymousFunction: FirAnonymousFunction,
        data: ExpectedArgumentType?,
    ): CompositeTransformResult<FirStatement> {
        val expectedReturnType = data?.getExpectedType(anonymousFunction)
            ?.takeIf { it.isBuiltinFunctionalType(session) }
            ?.returnType(session) as? ConeClassLikeType

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

    // Transformations for synthetic calls generated by FirSyntheticCallGenerator

    override fun transformWhenExpression(whenExpression: FirWhenExpression, data: ExpectedArgumentType?) =
        transformSyntheticCall(whenExpression, data)

    override fun transformTryExpression(tryExpression: FirTryExpression, data: ExpectedArgumentType?) =
        transformSyntheticCall(tryExpression, data)

    override fun transformCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: ExpectedArgumentType?) =
        transformSyntheticCall(checkNotNullCall, data)

    private inline fun <reified D> transformSyntheticCall(
        syntheticCall: D,
        data: ExpectedArgumentType?,
    ): CompositeTransformResult<FirStatement>
            where D : FirResolvable, D : FirExpression {
        val syntheticCall = syntheticCall.transformChildren(this, data?.getExpectedType(syntheticCall)?.toExpectedType()) as D
        val calleeReference = syntheticCall.calleeReference as? FirNamedReferenceWithCandidate ?: return syntheticCall.compose()

        val declaration = calleeReference.candidate.symbol.fir as? FirSimpleFunction ?: return syntheticCall.compose()

        val typeRef = typeCalculator.tryCalculateReturnType(declaration)
        syntheticCall.replaceTypeRefWithSubstituted(calleeReference, typeRef)

        return (syntheticCall.transformCalleeReference(
            StoreCalleeReference,
            buildResolvedNamedReference {
                source = calleeReference.source
                name = calleeReference.name
                resolvedSymbol = calleeReference.candidateSymbol
            },
        ) as D).compose()
    }

    override fun <T> transformConstExpression(
        constExpression: FirConstExpression<T>,
        data: ExpectedArgumentType?,
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

private fun FirExpression.unwrapArgument(): FirExpression = when (this) {
    is FirWrappedArgumentExpression -> expression
    else -> this
}

private fun ConeKotlinType.approximateIfPossible(expectedType: ConeKotlinType?) = if (this is ConeIntegerLiteralType) {
    getApproximatedType(expectedType)
} else {
    this
}
