/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubsystemFromArgument
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.LambdaArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.builtIns

class PostponedArgumentsAnalyzer(
    private val callableReferenceResolver: CallableReferenceResolver
) {
    interface Context {
        fun buildCurrentSubstitutor(): NewTypeSubstitutor

        // type can be proper if it not contains not fixed type variables
        fun canBeProper(type: UnwrappedType): Boolean

        fun hasUpperUnitConstraint(type: UnwrappedType): Boolean

        // mutable operations
        fun addOtherSystem(otherSystem: ConstraintStorage)

        fun getBuilder(): ConstraintSystemBuilder
    }

    fun analyze(
        c: Context,
        resolutionCallbacks: KotlinResolutionCallbacks,
        argument: ResolvedAtom,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        when (argument) {
            is ResolvedLambdaAtom ->
                analyzeLambda(c, resolutionCallbacks, argument, diagnosticsHolder)

            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                analyzeLambda(c, resolutionCallbacks, argument.transformToResolvedLambda(c.getBuilder()), diagnosticsHolder)

            is ResolvedCallableReferenceAtom ->
                callableReferenceResolver.processCallableReferenceArgument(c.getBuilder(), argument, diagnosticsHolder)

            is ResolvedCollectionLiteralAtom -> TODO("Not supported")

            else -> error("Unexpected resolved primitive: ${argument.javaClass.canonicalName}")
        }
    }

    private fun analyzeLambda(
        c: Context,
        resolutionCallbacks: KotlinResolutionCallbacks,
        lambda: ResolvedLambdaAtom,
        diagnosticHolder: KotlinDiagnosticsHolder
    ) {
        val currentSubstitutor = c.buildCurrentSubstitutor()
        fun substitute(type: UnwrappedType) = currentSubstitutor.safeSubstitute(type)

        val receiver = lambda.receiver?.let(::substitute)
        val parameters = lambda.parameters.map(::substitute)
        val rawReturnType = lambda.returnType

        val expectedTypeForReturnArguments = when {
            c.canBeProper(rawReturnType) -> substitute(rawReturnType)

            // For Unit-coercion
            c.hasUpperUnitConstraint(rawReturnType) -> lambda.returnType.builtIns.unitType

            else -> null
        }

        val returnArguments = resolutionCallbacks.analyzeAndGetLambdaReturnArguments(
            lambda.atom,
            lambda.isSuspend,
            receiver,
            parameters,
            expectedTypeForReturnArguments
        )

        returnArguments.forEach { c.addSubsystemFromArgument(it) }

        val subResolvedKtPrimitives = returnArguments.map {
            resolveKtPrimitive(c.getBuilder(), it, lambda.returnType.let(::substitute), diagnosticHolder, isReceiver = false)
        }

        if (returnArguments.isEmpty()) {
            val unitType = lambda.returnType.builtIns.unitType
            c.getBuilder().addSubtypeConstraint(lambda.returnType.let(::substitute), unitType, LambdaArgumentConstraintPosition(lambda))
        }

        lambda.setAnalyzedResults(returnArguments, subResolvedKtPrimitives)
    }
}