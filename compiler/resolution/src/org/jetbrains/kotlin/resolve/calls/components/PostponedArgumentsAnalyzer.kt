/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.resolve.calls.inference.addSubsystemFromArgument
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.BuilderInferencePosition
import org.jetbrains.kotlin.resolve.calls.inference.model.LambdaArgumentConstraintPositionImpl
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.utils.addToStdlib.cast

class PostponedArgumentsAnalyzer(
    private val callableReferenceArgumentResolver: CallableReferenceArgumentResolver,
    private val languageVersionSettings: LanguageVersionSettings
) {

    fun analyze(
        c: PostponedArgumentsAnalyzerContext,
        resolutionCallbacks: KotlinResolutionCallbacks,
        argument: ResolvedAtom,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ) {
        when (argument) {
            is ResolvedLambdaAtom ->
                analyzeLambda(c, resolutionCallbacks, argument, completionMode, diagnosticsHolder)

            is LambdaWithTypeVariableAsExpectedTypeAtom ->
                analyzeLambda(
                    c,
                    resolutionCallbacks,
                    argument.transformToResolvedLambda(c.getBuilder(), diagnosticsHolder),
                    completionMode,
                    diagnosticsHolder
                )

            is ResolvedCallableReferenceArgumentAtom ->
                callableReferenceArgumentResolver.processCallableReferenceArgument(
                    c.getBuilder(), argument, diagnosticsHolder, resolutionCallbacks
                )

            is ResolvedCollectionLiteralAtom -> TODO("Not supported")

            else -> error("Unexpected resolved primitive: ${argument.javaClass.canonicalName}")
        }
    }

    data class SubstitutorAndStubsForLambdaAnalysis(
        val stubsForPostponedVariables: Map<TypeVariableMarker, StubTypeMarker>,
        val substitute: (KotlinType) -> UnwrappedType
    )

    fun PostponedArgumentsAnalyzerContext.createSubstituteFunctorForLambdaAnalysis(): SubstitutorAndStubsForLambdaAnalysis {
        val stubsForPostponedVariables = bindingStubsForPostponedVariables()
        val currentSubstitutor = buildCurrentSubstitutor(stubsForPostponedVariables.mapKeys { it.key.freshTypeConstructor(this) })
        return SubstitutorAndStubsForLambdaAnalysis(stubsForPostponedVariables) {
            currentSubstitutor.safeSubstitute(this, it) as UnwrappedType
        }
    }

    fun analyzeLambda(
        c: PostponedArgumentsAnalyzerContext,
        resolutionCallbacks: KotlinResolutionCallbacks,
        lambda: ResolvedLambdaAtom,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticHolder: KotlinDiagnosticsHolder,
    ): ReturnArgumentsAnalysisResult {
        val substitutorAndStubsForLambdaAnalysis = c.createSubstituteFunctorForLambdaAnalysis()
        val substitute = substitutorAndStubsForLambdaAnalysis.substitute

        // Expected type has a higher priority against which lambda should be analyzed
        // Mostly, this is needed to report more specific diagnostics on lambda parameters
        fun expectedOrActualType(expected: UnwrappedType?, actual: UnwrappedType?): UnwrappedType? {
            val expectedSubstituted = expected?.let(substitute)
            return if (expectedSubstituted != null && c.canBeProper(expectedSubstituted)) expectedSubstituted else actual?.let(substitute)
        }

        val builtIns = c.getBuilder().builtIns

        val expectedParameters = lambda.expectedType.valueParameters()
        val expectedReceiver = lambda.expectedType.receiver()
        val expectedContextReceivers = lambda.expectedType.contextReceivers()

        val receiver = lambda.receiver?.let {
            expectedOrActualType(expectedReceiver ?: expectedParameters?.getOrNull(0), lambda.receiver)
        }
        val contextReceivers = lambda.contextReceivers.mapIndexedNotNull { i, contextReceiver ->
            expectedOrActualType(expectedContextReceivers?.getOrNull(i), contextReceiver)
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
            } ?: lambda.parameters.map(substitute)

        val rawReturnType = lambda.returnType

        val expectedTypeForReturnArguments = when {
            c.canBeProper(rawReturnType) -> substitute(rawReturnType)

            // For Unit-coercion
            !rawReturnType.isMarkedNullable && c.hasUpperOrEqualUnitConstraint(rawReturnType) -> builtIns.unitType

            else -> null
        }

        val convertedAnnotations = lambda.expectedType?.annotations?.let { annotations ->
            if (receiver != null || expectedReceiver == null) annotations
            else FilteredAnnotations(annotations, true) { it != StandardNames.FqNames.extensionFunctionType }
        }

        val returnArgumentsAnalysisResult = resolutionCallbacks.analyzeAndGetLambdaReturnArguments(
            lambda.atom,
            lambda.isSuspend,
            receiver,
            contextReceivers,
            parameters,
            expectedTypeForReturnArguments,
            convertedAnnotations ?: Annotations.EMPTY,
            substitutorAndStubsForLambdaAnalysis.stubsForPostponedVariables.cast(),
        )
        applyResultsOfAnalyzedLambdaToCandidateSystem(c, lambda, returnArgumentsAnalysisResult, completionMode, diagnosticHolder, substitute)
        return returnArgumentsAnalysisResult
    }

    fun applyResultsOfAnalyzedLambdaToCandidateSystem(
        c: PostponedArgumentsAnalyzerContext,
        lambda: ResolvedLambdaAtom,
        returnArgumentsAnalysisResult: ReturnArgumentsAnalysisResult,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticHolder: KotlinDiagnosticsHolder,
        substitute: (KotlinType) -> UnwrappedType = c.createSubstituteFunctorForLambdaAnalysis().substitute
    ) {
        val (returnArgumentsInfo, inferenceSession, hasInapplicableCallForBuilderInference) =
            returnArgumentsAnalysisResult

        if (hasInapplicableCallForBuilderInference) {
            inferenceSession?.initializeLambda(lambda)
            c.getBuilder().markCouldBeResolvedWithUnrestrictedBuilderInference()
            c.getBuilder().removePostponedVariables()
            return
        }

        val returnArguments = returnArgumentsInfo.nonErrorArguments
        returnArguments.forEach { c.addSubsystemFromArgument(it) }

        val lastExpression = returnArgumentsInfo.lastExpression
        val allReturnArguments =
            if (lastExpression != null && returnArgumentsInfo.lastExpressionCoercedToUnit && c.addSubsystemFromArgument(lastExpression)) {
                returnArguments + lastExpression
            } else {
                returnArguments
            }

        val subResolvedKtPrimitives = allReturnArguments.map {
            resolveKtPrimitive(
                c.getBuilder(), it, lambda.returnType.let(substitute),
                diagnosticHolder, ReceiverInfo.notReceiver, convertedType = null,
                inferenceSession
            )
        }

        if (!returnArgumentsInfo.returnArgumentsExist) {
            val unitType = lambda.returnType.builtIns.unitType
            val lambdaReturnType = lambda.returnType.let(substitute)
            c.getBuilder().addSubtypeConstraint(unitType, lambdaReturnType, LambdaArgumentConstraintPositionImpl(lambda))
        }

        lambda.setAnalyzedResults(returnArgumentsInfo, subResolvedKtPrimitives)

        val shouldUseBuilderInference = lambda.atom.hasBuilderInferenceAnnotation
                || languageVersionSettings.supportsFeature(LanguageFeature.UseBuilderInferenceWithoutAnnotation)

        if (inferenceSession != null && shouldUseBuilderInference) {
            val storageSnapshot = c.getBuilder().currentStorage()

            val postponedVariables = inferenceSession.inferPostponedVariables(
                lambda,
                storageSnapshot,
                completionMode,
                diagnosticHolder
            )
            if (postponedVariables == null) {
                c.getBuilder().removePostponedVariables()
                return
            }

            for ((constructor, resultType) in postponedVariables) {
                val variableWithConstraints = storageSnapshot.notFixedTypeVariables[constructor] ?: continue
                val variable = variableWithConstraints.typeVariable

                c.getBuilder().unmarkPostponedVariable(variable)

                // We add <inferred type> <: TypeVariable(T) to be able to contribute type info from several builder inference lambdas
                c.getBuilder().addSubtypeConstraint(resultType, variable.defaultType(c), BuilderInferencePosition)
            }

            c.removePostponedTypeVariablesFromConstraints(postponedVariables.keys)
        }
    }

    private fun UnwrappedType?.receiver(): UnwrappedType? {
        return forFunctionalType { getReceiverTypeFromFunctionType()?.unwrap() }
    }

    private fun UnwrappedType?.contextReceivers(): List<UnwrappedType>? {
        return forFunctionalType { getContextReceiverTypesFromFunctionType().map { it.unwrap() } }
    }

    private fun UnwrappedType?.valueParameters(): List<UnwrappedType>? {
        return forFunctionalType { getValueParameterTypesFromFunctionType().map { it.type.unwrap() } }
    }

    private inline fun <T> UnwrappedType?.forFunctionalType(f: UnwrappedType.() -> T?): T? {
        return if (this?.isBuiltinFunctionalType == true) f(this) else null
    }
}
