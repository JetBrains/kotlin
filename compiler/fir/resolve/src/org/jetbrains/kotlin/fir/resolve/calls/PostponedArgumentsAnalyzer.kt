/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.StandardClassIds.Unit
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.fir.symbols.invoke

interface LambdaAnalyzer {
    fun analyzeAndGetLambdaReturnArguments(
        lambdaArgument: FirAnonymousFunction,
        isSuspend: Boolean,
        receiverType: ConeKotlinType?,
        parameters: List<ConeKotlinType>,
        expectedReturnType: ConeKotlinType?, // null means, that return type is not proper i.e. it depends on some type variables
        stubsForPostponedVariables: Map<TypeVariableMarker, StubTypeMarker>
    ): Pair<List<FirExpression>, InferenceSession>
}


class PostponedArgumentsAnalyzer(
    val lambdaAnalyzer: LambdaAnalyzer,
    val typeProvider: (FirExpression) -> FirTypeRef?,
    val components: InferenceComponents
) {

    fun analyze(
        c: PostponedArgumentsAnalyzer.Context,
//        resolutionCallbacks: KotlinResolutionCallbacks,
        argument: PostponedResolvedAtomMarker
        //diagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        when (argument) {
            is ResolvedLambdaAtom ->
                analyzeLambda(c, argument/*, diagnosticsHolder*/)

//            is LambdaWithTypeVariableAsExpectedTypeAtom ->
//                analyzeLambda(
//                    c, resolutionCallbacks, argument.transformToResolvedLambda(c.getBuilder()), diagnosticsHolder
//                )

//            is ResolvedCallableReferenceAtom ->
//                callableReferenceResolver.processCallableReferenceArgument(c.getBuilder(), argument, diagnosticsHolder)
//
//            is ResolvedCollectionLiteralAtom -> TODO("Not supported")

            else -> error("Unexpected resolved primitive: ${argument.javaClass.canonicalName}")
        }
    }


    private fun analyzeLambda(
        c: PostponedArgumentsAnalyzer.Context,
        lambda: ResolvedLambdaAtom//,
        //diagnosticHolder: KotlinDiagnosticsHolder
    ) {
        val unitType = Unit(components.session.service()).constructType(emptyArray(), false)
        val stubsForPostponedVariables = c.bindingStubsForPostponedVariables()
        val currentSubstitutor = c.buildCurrentSubstitutor(stubsForPostponedVariables.mapKeys { it.key.freshTypeConstructor(c) })

        fun substitute(type: ConeKotlinType) = currentSubstitutor.safeSubstitute(c, type) as ConeKotlinType

        val receiver = lambda.receiver?.let(::substitute)
        val parameters = lambda.parameters.map(::substitute)
        val rawReturnType = lambda.returnType

        val expectedTypeForReturnArguments = when {
            c.canBeProper(rawReturnType) -> substitute(rawReturnType)

            // For Unit-coercion
            c.hasUpperOrEqualUnitConstraint(rawReturnType) -> unitType

            else -> null
        }

        val (returnArguments, inferenceSession) = lambdaAnalyzer.analyzeAndGetLambdaReturnArguments(
            lambda.atom,
            lambda.isSuspend,
            receiver,
            parameters,
            expectedTypeForReturnArguments,
            stubsForPostponedVariables
        )

        returnArguments.forEach { c.addSubsystemFromExpression(it) }

        val checkerSink: CheckerSink = CheckerSinkImpl(components)

        val subResolvedKtPrimitives = returnArguments.map {
            var atom: PostponedResolvedAtomMarker? = null
            resolveArgumentExpression(
                c.getBuilder(),
                it,
                lambda.returnType.let(::substitute),
                lambda.atom.returnTypeRef, // TODO: proper ref
                checkerSink,
                false,
                false,
                { atom = it },
                typeProvider
            )
//            resolveKtPrimitive(
//                c.getBuilder(), it, lambda.returnType.let(::substitute), diagnosticHolder, isReceiver = false
//            )
        }

        if (returnArguments.isEmpty()) {
//            val unitType =
            val lambdaReturnType = lambda.returnType.let(::substitute)
            c.getBuilder().addSubtypeConstraint(
                lambdaReturnType,
                unitType, /*LambdaArgumentConstraintPosition(lambda)*/
                SimpleConstraintSystemConstraintPosition
            )
            c.getBuilder().addSubtypeConstraint(
                unitType,
                lambdaReturnType, /*LambdaArgumentConstraintPosition(lambda)*/
                SimpleConstraintSystemConstraintPosition
            )
        }

        lambda.analyzed = true
        //lambda.setAnalyzedResults(returnArguments, subResolvedKtPrimitives)

//        if (inferenceSession != null) {
//            val storageSnapshot = c.getBuilder().currentStorage()
//
//            val postponedVariables = inferenceSession.inferPostponedVariables(lambda, storageSnapshot)
//
//            for ((constructor, resultType) in postponedVariables) {
//                val variableWithConstraints = storageSnapshot.notFixedTypeVariables[constructor] ?: continue
//                val variable = variableWithConstraints.typeVariable
//
//                c.getBuilder().unmarkPostponedVariable(variable)
//                c.getBuilder().addEqualityConstraint(variable.defaultType(c), resultType, CoroutinePosition())
//            }
//        }
    }
}


