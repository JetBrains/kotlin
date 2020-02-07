/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.builtins.isBuiltinFunctionalType
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.addSubsystemFromArgument
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.addToStdlib.cast

class PostponedArgumentsAnalyzer(
    private val callableReferenceResolver: CallableReferenceResolver
) {
    interface Context : TypeSystemInferenceExtensionContext {
        fun buildCurrentSubstitutor(additionalBindings: Map<TypeConstructorMarker, StubTypeMarker>): TypeSubstitutorMarker
        fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
        fun bindingStubsForPostponedVariables(): Map<TypeVariableMarker, StubTypeMarker>

        // type can be proper if it not contains not fixed type variables
        fun canBeProper(type: KotlinTypeMarker): Boolean

        fun hasUpperOrEqualUnitConstraint(type: KotlinTypeMarker): Boolean

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
                analyzeLambda(
                    c, resolutionCallbacks, argument.transformToResolvedLambda(c.getBuilder(), diagnosticsHolder), diagnosticsHolder
                )

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
        val stubsForPostponedVariables = c.bindingStubsForPostponedVariables()
        val currentSubstitutor = c.buildCurrentSubstitutor(stubsForPostponedVariables.mapKeys { it.key.freshTypeConstructor(c) })

        fun substitute(type: UnwrappedType) = currentSubstitutor.safeSubstitute(c, type) as UnwrappedType

        // Expected type has a higher priority against which lambda should be analyzed
        // Mostly, this is needed to report more specific diagnostics on lambda parameters
        fun expectedOrActualType(expected: UnwrappedType?, actual: UnwrappedType?): UnwrappedType? {
            val expectedSubstituted = expected?.let(::substitute)
            return if (expectedSubstituted != null && c.canBeProper(expectedSubstituted)) expectedSubstituted else actual?.let(::substitute)
        }

        val builtIns = c.getBuilder().builtIns

        val expectedParameters = lambda.expectedType.valueParameters()
        val expectedReceiver = lambda.expectedType.receiver()

        val receiver = lambda.receiver?.let {
            expectedOrActualType(expectedReceiver ?: expectedParameters?.getOrNull(0), lambda.receiver)
        }

        val expectedParametersToMatchAgainst = when {
            receiver == null && expectedReceiver != null && expectedParameters != null -> listOf(expectedReceiver) + expectedParameters
            receiver == null && expectedReceiver != null -> listOf(expectedReceiver)
            receiver != null && expectedReceiver == null -> expectedParameters?.drop(1)
            else -> expectedParameters
        }

        val parameters =
            expectedParametersToMatchAgainst?.mapIndexed { index, expected ->
                expectedOrActualType(expected, lambda.parameters.getOrNull(index)) ?: builtIns.nothingType
            } ?: lambda.parameters.map(::substitute)

        val rawReturnType = lambda.returnType

        val expectedTypeForReturnArguments = when {
            c.canBeProper(rawReturnType) -> substitute(rawReturnType)

            // For Unit-coercion
            c.hasUpperOrEqualUnitConstraint(rawReturnType) -> builtIns.unitType

            else -> null
        }

        val convertedAnnotations = lambda.expectedType?.annotations?.let { annotations ->
            if (receiver != null || expectedReceiver == null) annotations
            else FilteredAnnotations(annotations, true) { it != KotlinBuiltIns.FQ_NAMES.extensionFunctionType }
        }

        val (returnArgumentsInfo, inferenceSession) = resolutionCallbacks.analyzeAndGetLambdaReturnArguments(
            lambda.atom,
            lambda.isSuspend,
            receiver,
            parameters,
            expectedTypeForReturnArguments,
            convertedAnnotations ?: Annotations.EMPTY,
            stubsForPostponedVariables.cast()
        )

        returnArgumentsInfo.nonErrorArguments.forEach { c.addSubsystemFromArgument(it) }

        val subResolvedKtPrimitives = returnArgumentsInfo.nonErrorArguments.map {
            resolveKtPrimitive(
                c.getBuilder(), it, lambda.returnType.let(::substitute), diagnosticHolder, ReceiverInfo.notReceiver, convertedType = null
            )
        }

        if (!returnArgumentsInfo.returnArgumentsExist) {
            val unitType = lambda.returnType.builtIns.unitType
            val lambdaReturnType = lambda.returnType.let(::substitute)
            c.getBuilder().addSubtypeConstraint(unitType, lambdaReturnType, LambdaArgumentConstraintPosition(lambda))
        }

        lambda.setAnalyzedResults(returnArgumentsInfo, subResolvedKtPrimitives)

        if (inferenceSession != null) {
            val storageSnapshot = c.getBuilder().currentStorage()

            val postponedVariables = inferenceSession.inferPostponedVariables(lambda, storageSnapshot, diagnosticHolder)

            for ((constructor, resultType) in postponedVariables) {
                val variableWithConstraints = storageSnapshot.notFixedTypeVariables[constructor] ?: continue
                val variable = variableWithConstraints.typeVariable

                c.getBuilder().unmarkPostponedVariable(variable)
                c.getBuilder().addEqualityConstraint(variable.defaultType(c), resultType, CoroutinePosition())
            }
        }
    }

    private fun UnwrappedType?.receiver(): UnwrappedType? {
        return forFunctionalType { getReceiverTypeFromFunctionType()?.unwrap() }
    }

    private fun UnwrappedType?.valueParameters(): List<UnwrappedType>? {
        return forFunctionalType { getValueParameterTypesFromFunctionType().map { it.type.unwrap() } }
    }

    private inline fun <T> UnwrappedType?.forFunctionalType(f: UnwrappedType.() -> T?): T? {
        return if (this?.isBuiltinFunctionalType == true) f(this) else null
    }
}