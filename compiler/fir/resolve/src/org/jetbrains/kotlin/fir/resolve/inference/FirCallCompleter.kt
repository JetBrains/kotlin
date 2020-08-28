/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.FirCallCompletionResultsWriterTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.InvocationKindTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.resolve.typeFromCallee
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.StubTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class FirCallCompleter(
    private val transformer: FirBodyResolveTransformer,
    private val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents
) : BodyResolveComponents by components {
    val completer = ConstraintSystemCompleter(components)
    private val inferenceSession
        get() = inferenceComponents.inferenceSession

    data class CompletionResult<T>(val result: T, val callCompleted: Boolean)

    fun <T> completeCall(call: T, expectedTypeRef: FirTypeRef?): CompletionResult<T>
            where T : FirResolvable, T : FirStatement {
        val typeRef = typeFromCallee(call)

        val reference = call.calleeReference as? FirNamedReferenceWithCandidate ?: return CompletionResult(call, true)
        val candidate = reference.candidate
        val initialSubstitutor = candidate.substitutor

        val initialType = initialSubstitutor.substituteOrSelf(typeRef.type)

        if (call is FirExpression) {
            call.resultType = typeRef.resolvedTypeFromPrototype(initialType)
        }

        if (expectedTypeRef is FirResolvedTypeRef) {
            candidate.system.addSubtypeConstraint(initialType, expectedTypeRef.type, SimpleConstraintSystemConstraintPosition)
        }

        val completionMode = candidate.computeCompletionMode(inferenceComponents, expectedTypeRef, initialType)

        val analyzer = createPostponedArgumentsAnalyzer()
        call.transformSingle(InvocationKindTransformer, null)

        return when (completionMode) {
            ConstraintSystemCompletionMode.FULL -> {
                if (inferenceSession.shouldRunCompletion(call)) {
                    completer.complete(candidate.system.asConstraintSystemCompleterContext(), completionMode, listOf(call), initialType) {
                        analyzer.analyze(candidate.system.asPostponedArgumentsAnalyzerContext(), it, candidate)
                    }
                    val finalSubstitutor =
                        candidate.system.asReadOnlyStorage().buildAbstractResultingSubstitutor(inferenceComponents.ctx) as ConeSubstitutor
                    val completedCall = call.transformSingle(
                        FirCallCompletionResultsWriterTransformer(
                            session, finalSubstitutor, returnTypeCalculator,
                            inferenceComponents.approximator,
                            integerOperatorsTypeUpdater,
                            integerLiteralTypeApproximator
                        ),
                        null
                    )
                    inferenceSession.addCompetedCall(completedCall, candidate)
                    CompletionResult(completedCall, true)
                } else {
                    inferenceSession.addPartiallyResolvedCall(call)
                    CompletionResult(call, false)
                }
            }

            ConstraintSystemCompletionMode.PARTIAL -> {
                completer.complete(candidate.system.asConstraintSystemCompleterContext(), completionMode, listOf(call), initialType) {
                    analyzer.analyze(candidate.system.asPostponedArgumentsAnalyzerContext(), it, candidate)
                }
                val approximatedCall = call.transformSingle(integerOperatorsTypeUpdater, null)
                inferenceSession.addPartiallyResolvedCall(approximatedCall)
                CompletionResult(approximatedCall, false)
            }

            ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA -> throw IllegalStateException()
        }
    }

    fun createCompletionResultsWriter(
        substitutor: ConeSubstitutor,
        mode: FirCallCompletionResultsWriterTransformer.Mode = FirCallCompletionResultsWriterTransformer.Mode.Normal
    ): FirCallCompletionResultsWriterTransformer {
        return FirCallCompletionResultsWriterTransformer(
            session, substitutor, returnTypeCalculator,
            inferenceComponents.approximator,
            integerOperatorsTypeUpdater,
            integerLiteralTypeApproximator,
            mode
        )
    }

    fun createPostponedArgumentsAnalyzer(): PostponedArgumentsAnalyzer {
        val lambdaAnalyzer = LambdaAnalyzerImpl()
        return PostponedArgumentsAnalyzer(
            lambdaAnalyzer, inferenceComponents,
            transformer.components.callResolver
        )
    }

    private inner class LambdaAnalyzerImpl : LambdaAnalyzer {
        override fun analyzeAndGetLambdaReturnArguments(
            lambdaAtom: ResolvedLambdaAtom,
            receiverType: ConeKotlinType?,
            parameters: List<ConeKotlinType>,
            expectedReturnType: ConeKotlinType?,
            rawReturnType: ConeKotlinType,
            stubsForPostponedVariables: Map<TypeVariableMarker, StubTypeMarker>
        ): ReturnArgumentsAnalysisResult {
            val lambdaArgument: FirAnonymousFunction = lambdaAtom.atom
            val needItParam = lambdaArgument.valueParameters.isEmpty() && parameters.size == 1

            val itParam = when {
                needItParam -> {
                    val name = Name.identifier("it")
                    val itType = parameters.single()
                    buildValueParameter {
                        session = this@FirCallCompleter.session
                        origin = FirDeclarationOrigin.Source
                        returnTypeRef = buildResolvedTypeRef { type = itType.approximateLambdaInputType() }
                        this.name = name
                        symbol = FirVariableSymbol(name)
                        defaultValue = null
                        isCrossinline = false
                        isNoinline = false
                        isVararg = false
                    }
                }
                else -> null
            }

            val expectedReturnTypeRef = expectedReturnType?.let { lambdaArgument.returnTypeRef.resolvedTypeFromPrototype(it) }

            lambdaArgument.replaceReceiverTypeRef(
                receiverType?.approximateLambdaInputType()?.let {
                    lambdaArgument.receiverTypeRef?.resolvedTypeFromPrototype(it)
                }
            )

            lambdaArgument.valueParameters.forEachIndexed { index, parameter ->
                parameter.replaceReturnTypeRef(
                    parameter.returnTypeRef.resolvedTypeFromPrototype(parameters[index].approximateLambdaInputType())
                )
            }

            lambdaArgument.replaceValueParameters(lambdaArgument.valueParameters + listOfNotNull(itParam))
            lambdaArgument.replaceReturnTypeRef(expectedReturnTypeRef ?: noExpectedType)

            val builderInferenceSession = runIf(stubsForPostponedVariables.isNotEmpty()) {
                @Suppress("UNCHECKED_CAST")
                FirBuilderInferenceSession(components, stubsForPostponedVariables as Map<ConeTypeVariable, ConeStubType>)
            }

            val localContext = towerDataContextForAnonymousFunctions.get(lambdaArgument.symbol) ?: error(
                ""
            )
            transformer.context.withTowerDataContext(localContext) {
                if (builderInferenceSession != null) {
                    components.inferenceComponents.withInferenceSession(builderInferenceSession) {
                        lambdaArgument.transformSingle(transformer, ResolutionMode.LambdaResolution(expectedReturnTypeRef))
                    }
                } else {
                    lambdaArgument.transformSingle(transformer, ResolutionMode.LambdaResolution(expectedReturnTypeRef))
                }
            }
            transformer.context.dropContextForAnonymousFunction(lambdaArgument)

            val returnArguments = dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(lambdaArgument)

            return ReturnArgumentsAnalysisResult(returnArguments, builderInferenceSession)
        }
    }

    private fun ConeKotlinType.approximateLambdaInputType(): ConeKotlinType =
        inferenceComponents.approximator.approximateToSuperType(
            this, TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
        ) as ConeKotlinType? ?: this
}
