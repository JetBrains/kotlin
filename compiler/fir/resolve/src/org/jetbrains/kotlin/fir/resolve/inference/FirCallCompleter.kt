/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.FirCallCompletionResultsWriterTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.InvocationKindTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.MapArguments
import org.jetbrains.kotlin.fir.resolve.transformers.StoreType
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.typeFromCallee
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.returnExpressions
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.types.model.StubTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

class FirCallCompleter(
    private val transformer: FirBodyResolveTransformer,
    components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents
) : BodyResolveComponents by components {
    private val completer = ConstraintSystemCompleter(components.inferenceComponents)

    fun <T> completeCall(call: T, expectedTypeRef: FirTypeRef?): T
            where T : FirResolvable, T : FirStatement {
        val typeRef = typeFromCallee(call)
        if (typeRef.type is ConeKotlinErrorType) {
            if (call is FirExpression) {
                call.resultType = typeRef
            }
            return call
        }
        val candidate = call.candidate() ?: return call
        val initialSubstitutor = candidate.substitutor

        val initialType = initialSubstitutor.substituteOrSelf(typeRef.type)

        if (call is FirExpression) {
            call.resultType = typeRef.resolvedTypeFromPrototype(initialType)
        }

        if (expectedTypeRef is FirResolvedTypeRef) {
            candidate.system.addSubtypeConstraint(initialType, expectedTypeRef.type, SimpleConstraintSystemConstraintPosition)
        }

        val completionMode = candidate.computeCompletionMode(inferenceComponents, expectedTypeRef, initialType)
        val replacements = mutableMapOf<FirExpression, FirExpression>()

        val analyzer =
            PostponedArgumentsAnalyzer(
                LambdaAnalyzerImpl(replacements), { it.resultType }, inferenceComponents, candidate, replacements,
                transformer.components.callResolver
            )

        call.transformSingle(InvocationKindTransformer, null)
        completer.complete(candidate.system.asConstraintSystemCompleterContext(), completionMode, listOf(call), initialType) {
            analyzer.analyze(candidate.system.asPostponedArgumentsAnalyzerContext(), it)
        }

        call.transformChildren(MapArguments, replacements.toMap())

        if (completionMode == KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode.FULL) {
            val finalSubstitutor =
                candidate.system.asReadOnlyStorage().buildAbstractResultingSubstitutor(inferenceComponents.ctx) as ConeSubstitutor
            return call.transformSingle(
                FirCallCompletionResultsWriterTransformer(session, finalSubstitutor, returnTypeCalculator),
                null
            )
        }
        return call
    }

    private inner class LambdaAnalyzerImpl(
        val replacements: MutableMap<FirExpression, FirExpression>
    ) : LambdaAnalyzer {
        override fun analyzeAndGetLambdaReturnArguments(
            lambdaArgument: FirAnonymousFunction,
            isSuspend: Boolean,
            receiverType: ConeKotlinType?,
            parameters: List<ConeKotlinType>,
            expectedReturnType: ConeKotlinType?,
            rawReturnType: ConeKotlinType,
            stubsForPostponedVariables: Map<TypeVariableMarker, StubTypeMarker>
        ): Pair<List<FirStatement>, InferenceSession> {

            val needItParam = lambdaArgument.valueParameters.isEmpty() && parameters.size == 1

            val itParam = when {
                needItParam -> {
                    val name = Name.identifier("it")
                    val itType = parameters.single()
                    FirValueParameterImpl(
                        null,
                        session,
                        FirResolvedTypeRefImpl(null, itType),
                        name,
                        FirVariableSymbol(name),
                        defaultValue = null,
                        isCrossinline = false,
                        isNoinline = false,
                        isVararg = false
                    )
                }
                else -> null
            }

            val expectedReturnTypeRef = expectedReturnType?.let { lambdaArgument.returnTypeRef.resolvedTypeFromPrototype(it) }

            val newLambdaExpression = lambdaArgument.copy(
                receiverTypeRef = receiverType?.let { lambdaArgument.receiverTypeRef?.resolvedTypeFromPrototype(it) },
                valueParameters = lambdaArgument.valueParameters.mapIndexed { index, parameter ->
                    parameter.transformReturnTypeRef(StoreType, parameter.returnTypeRef.resolvedTypeFromPrototype(parameters[index]))
                    parameter
                } + listOfNotNull(itParam),
                returnTypeRef = expectedReturnTypeRef ?: noExpectedType
            )

            replacements[lambdaArgument] =
                newLambdaExpression.transformSingle(transformer, ResolutionMode.LambdaResolution(expectedReturnTypeRef))

            val returnArguments = dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(newLambdaExpression)
            return returnArguments to InferenceSession.default
        }
    }
}
