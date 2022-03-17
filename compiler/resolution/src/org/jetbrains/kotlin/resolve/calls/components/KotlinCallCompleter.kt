/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.builtins.isFunctionTypeOrSubtype
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.resolve.calls.components.candidate.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.components.candidate.CallableReferenceResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.components.candidate.SimpleResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.addEqualityConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage.Empty.hasContradiction
import org.jetbrains.kotlin.resolve.calls.inference.model.ExpectedTypeConstraintPositionImpl
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.CandidateFactory
import org.jetbrains.kotlin.resolve.calls.tower.forceResolution
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.model.safeSubstitute
import org.jetbrains.kotlin.utils.addToStdlib.same

class KotlinCallCompleter(
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    private val kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter,
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    private val languageVersionSettings: LanguageVersionSettings
) {

    fun runCompletion(
        factory: CandidateFactory<ResolutionCandidate>,
        candidates: Collection<ResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): CallResolutionResult {
        val diagnosticHolder = KotlinDiagnosticsHolder.SimpleHolder()

        when {
            candidates.isEmpty() -> diagnosticHolder.addDiagnostic(NoneCandidatesCallDiagnostic())
            candidates.size > 1 -> diagnosticHolder.addDiagnostic(ManyCandidatesCallDiagnostic(candidates))
        }

        val candidate = prepareCandidateForCompletion(factory, candidates, resolutionCallbacks)
        val resultType = when (candidate) {
            is SimpleResolutionCandidate -> {
                candidate.checkSamWithVararg(diagnosticHolder)
                candidate.substitutedReturnType().also {
                    candidate.addExpectedTypeConstraint(it, expectedType)
                    candidate.addExpectedTypeFromCastConstraint(it, resolutionCallbacks)
                }
            }
            is CallableReferenceResolutionCandidate -> candidate.substitutedReflectionType()
        }

        val completionMode = CompletionModeCalculator.computeCompletionMode(
            candidate, expectedType, resultType, trivialConstraintTypeInferenceOracle, resolutionCallbacks.inferenceSession
        )

        return when (completionMode) {
            ConstraintSystemCompletionMode.FULL -> {
                if (resolutionCallbacks.inferenceSession.shouldRunCompletion(candidate)) {
                    candidate.runCompletion(completionMode, diagnosticHolder, resolutionCallbacks)
                    candidate.asCallResolutionResult(completionMode, diagnosticHolder)
                } else {
                    candidate.asCallResolutionResult(
                        ConstraintSystemCompletionMode.PARTIAL, diagnosticHolder, forwardToInferenceSession = true
                    )
                }
            }
            ConstraintSystemCompletionMode.PARTIAL -> {
                candidate.runCompletion(completionMode, diagnosticHolder, resolutionCallbacks)
                candidate.asCallResolutionResult(completionMode, diagnosticHolder)
            }
            ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA -> throw IllegalStateException("Should not be here")

        }
    }

    fun chooseCandidateRegardingOverloadResolutionByLambdaReturnType(
        candidates: Set<SimpleResolutionCandidate>,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): Set<SimpleResolutionCandidate> {
        val lambdas = candidates.flatMap { candidate ->
            candidate.getSubResolvedAtoms()
                .filter { it is ResolvedLambdaAtom && !it.analyzed }
                .map { candidate to it as ResolvedLambdaAtom }
        }.groupBy { (_, atom) -> atom.atom }
            .values
            .singleOrNull()
            ?.toMap() ?: return candidates

        if (!lambdas.values.same { it.parameters.size }) return candidates
        if (!lambdas.values.all { it.expectedType?.isFunctionTypeOrSubtype == true }) return candidates

        for (candidate in lambdas.keys) {
            candidate.runCompletion(
                ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA,
                candidate,
                resolutionCallbacks
            )
        }
        if (!lambdas.entries.same { (candidate, atom) -> candidate.getInputTypesOfLambdaAtom(atom) }) {
            return candidates
        }

        val newAtoms = lambdas.mapValues { (candidate, atom) ->
            kotlinConstraintSystemCompleter.prepareLambdaAtomForFactoryPattern(atom, candidate, candidate)
        }

        val diagnosticHolderForLambda = KotlinDiagnosticsHolder.SimpleHolder()
        val iterator = newAtoms.entries.iterator()
        val (firstCandidate, firstAtom) = iterator.next()

        resolutionCallbacks.recordInlinabilityOfLambda(lambdas.entries)

        val results = postponedArgumentsAnalyzer.analyzeLambda(
            firstCandidate.getSystem().asPostponedArgumentsAnalyzerContext(),
            resolutionCallbacks,
            firstAtom,
            ConstraintSystemCompletionMode.FULL,
            diagnosticHolderForLambda,
        )
        propagateLambdaAnalysisDiagnostics(diagnosticHolderForLambda, firstCandidate)

        while (iterator.hasNext()) {
            val (candidate, atom) = iterator.next()
            postponedArgumentsAnalyzer.applyResultsOfAnalyzedLambdaToCandidateSystem(
                candidate.getSystem().asPostponedArgumentsAnalyzerContext(),
                atom,
                results,
                ConstraintSystemCompletionMode.FULL,
                diagnosticHolderForLambda
            )
            propagateLambdaAnalysisDiagnostics(diagnosticHolderForLambda, candidate)
        }

        val errorCandidates = mutableSetOf<SimpleResolutionCandidate>()
        val successfulCandidates = mutableSetOf<SimpleResolutionCandidate>()

        for (candidate in candidates) {
            if (candidate.isSuccessful) {
                successfulCandidates += candidate
            } else {
                errorCandidates += candidate
            }
        }
        return when {
            successfulCandidates.isNotEmpty() -> successfulCandidates
            else -> errorCandidates
        }
    }

    private fun propagateLambdaAnalysisDiagnostics(
        diagnosticHolder: KotlinDiagnosticsHolder.SimpleHolder,
        candidate: SimpleResolutionCandidate
    ) {
        val dontLoseDiagnosticsDuringOverloadResolutionByReturnType =
            languageVersionSettings.supportsFeature(LanguageFeature.DontLoseDiagnosticsDuringOverloadResolutionByReturnType)

        for (diagnostic in diagnosticHolder.getDiagnostics()) {
            if (diagnostic is TransformableToWarning<*>) {
                val transformedDiagnostic =
                    if (dontLoseDiagnosticsDuringOverloadResolutionByReturnType) diagnostic else diagnostic.transformToWarning()
                if (transformedDiagnostic != null) {
                    candidate.addDiagnostic(transformedDiagnostic)
                }
            }
        }
    }

    private fun SimpleResolutionCandidate.getInputTypesOfLambdaAtom(atom: ResolvedLambdaAtom): List<UnwrappedType> {
        val result = mutableListOf<UnwrappedType>()
        val substitutor = getSystem().getBuilder().buildCurrentSubstitutor()
        val ctx = getSystem().asConstraintSystemCompleterContext()
        for (inputType in atom.inputTypes) {
            result += substitutor.safeSubstitute(ctx, inputType) as UnwrappedType
        }
        return result
    }


    private fun ResolutionCandidate.checkSamWithVararg(diagnosticHolder: KotlinDiagnosticsHolder.SimpleHolder) {
        val samConversionPerArgumentWithWarningsForVarargAfterSam =
            callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument) &&
                    !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitVarargAsArrayAfterSamArgument)

        val candidateDescriptor = resolvedCall.candidateDescriptor
        if (samConversionPerArgumentWithWarningsForVarargAfterSam && candidateDescriptor is SyntheticMemberDescriptor<*>) {
            val declarationDescriptor = candidateDescriptor.baseDescriptorForSynthetic as? FunctionDescriptor ?: return

            if (declarationDescriptor.valueParameters.lastOrNull()?.isVararg == true) {
                diagnosticHolder.addDiagnostic(
                    ResolvedToSamWithVarargDiagnostic(resolvedCall.atom.argumentsInParenthesis.lastOrNull() ?: return)
                )
            }
        }
    }

    fun createAllCandidatesResult(
        candidates: Collection<ResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): CallResolutionResult {
        val completedCandidates = candidates.map { candidate ->
            val diagnosticsHolder = KotlinDiagnosticsHolder.SimpleHolder()

            candidate.addExpectedTypeConstraint(
                candidate.substitutedReturnType(), expectedType
            )

            runCompletion(
                candidate.resolvedCall,
                ConstraintSystemCompletionMode.FULL,
                diagnosticsHolder,
                candidate.getSystem(),
                resolutionCallbacks,
                collectAllCandidatesMode = true
            )

            CandidateWithDiagnostics(candidate, diagnosticsHolder.getDiagnostics() + candidate.diagnostics)
        }
        return AllCandidatesResolutionResult(completedCandidates, resolutionCallbacks.createEmptyConstraintSystem())
    }

    private fun ResolutionCandidate.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        diagnosticHolder: KotlinDiagnosticsHolder,
        resolutionCallbacks: KotlinResolutionCallbacks,
    ) {
        runCompletion(resolvedCall, completionMode, diagnosticHolder, getSystem(), resolutionCallbacks)
    }

    private fun runCompletion(
        resolvedCallAtom: ResolvedCallAtom,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: KotlinDiagnosticsHolder,
        constraintSystem: NewConstraintSystem,
        resolutionCallbacks: KotlinResolutionCallbacks,
        collectAllCandidatesMode: Boolean = false
    ) {
        val returnType = resolvedCallAtom.freshReturnType ?: constraintSystem.builtIns.unitType
        kotlinConstraintSystemCompleter.runCompletion(
            constraintSystem.asConstraintSystemCompleterContext(),
            completionMode,
            listOf(resolvedCallAtom),
            returnType,
            diagnosticsHolder
        ) {
            if (collectAllCandidatesMode) {
                it.setEmptyAnalyzedResults()
            } else {
                postponedArgumentsAnalyzer.analyze(
                    constraintSystem.asPostponedArgumentsAnalyzerContext(),
                    resolutionCallbacks,
                    it,
                    completionMode,
                    diagnosticsHolder
                )
            }
        }

        constraintSystem.errors.forEach(diagnosticsHolder::addError)

        if (returnType is ErrorType && returnType.kind == ErrorTypeKind.RECURSIVE_TYPE) {
            diagnosticsHolder.addDiagnostic(TypeCheckerHasRanIntoRecursion(resolvedCallAtom))
        }
    }

    private fun prepareCandidateForCompletion(
        factory: CandidateFactory<ResolutionCandidate>,
        candidates: Collection<ResolutionCandidate>,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): ResolutionCandidate {
        val candidate = candidates.singleOrNull()

        // this is needed at least for non-local return checker, because when we analyze lambda we should already bind descriptor for outer call
        candidate?.resolvedCall?.let {
            val mayNeedDescriptor = it.argumentToCandidateParameter.keys.any { arg ->
                arg is LambdaKotlinCallArgument
            }
            if (mayNeedDescriptor) {
                resolutionCallbacks.bindStubResolvedCallForCandidate(it)
            }
            resolutionCallbacks.disableContractsIfNecessary(it)
        }

        return candidate ?: factory.createErrorCandidate().forceResolution()
    }

    private fun CallableReferenceResolutionCandidate.substitutedReflectionType(): UnwrappedType {
        return resolvedCall.freshVariablesSubstitutor.safeSubstitute(this.reflectionCandidateType)
    }

    private fun ResolutionCandidate.substitutedReturnType(): UnwrappedType? {
        val returnType = resolvedCall.candidateDescriptor.returnType?.unwrap() ?: return null
        return resolvedCall.freshVariablesSubstitutor.safeSubstitute(returnType)
    }

    private fun ResolutionCandidate.addExpectedTypeConstraint(
        returnType: UnwrappedType?,
        expectedType: UnwrappedType?
    ) {
        if (returnType == null) return
        if (expectedType == null || (TypeUtils.noExpectedType(expectedType) && expectedType !== TypeUtils.UNIT_EXPECTED_TYPE)) return

        val csBuilder = getSystem().getBuilder()

        when {
            csBuilder.currentStorage().notFixedTypeVariables.isEmpty() -> {
                // This is needed to avoid multiple mismatch errors as we type check resulting type against expected one later
                // Plus, it helps with IDE-tests where it's important to have particular diagnostics.
                // Note that it aligns with the old inference, see CallCompleter.completeResolvedCallAndArguments

                // Another point is to avoid adding constraint from expected type for constant expressions like `1 + 1` because of
                // type coercion for numbers:
                // val a: Long = 1 + 1, result type of "1 + 1" will be Int and adding constraint with Long will produce type mismatch
                return
            }

            expectedType === TypeUtils.UNIT_EXPECTED_TYPE ->
                csBuilder.addEqualityConstraintIfCompatible(
                    returnType, csBuilder.builtIns.unitType, ExpectedTypeConstraintPositionImpl(resolvedCall.atom)
                )

            else ->
                csBuilder.addSubtypeConstraint(returnType, expectedType, ExpectedTypeConstraintPositionImpl(resolvedCall.atom))
        }
    }

    private fun ResolutionCandidate.addExpectedTypeFromCastConstraint(
        returnType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ) {
        if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.ExpectedTypeFromCast)) return
        if (returnType == null) return

        val expectedType = resolutionCallbacks.getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedCall) ?: return
        val csBuilder = getSystem().getBuilder()

        csBuilder.addSubtypeConstraint(returnType, expectedType, ExpectedTypeConstraintPositionImpl(resolvedCall.atom))
    }

    fun ResolutionCandidate.asCallResolutionResult(
        type: ConstraintSystemCompletionMode,
        diagnosticsHolder: KotlinDiagnosticsHolder.SimpleHolder,
        forwardToInferenceSession: Boolean = false
    ): CallResolutionResult {
        val constraintSystem = getSystem()
        val allDiagnostics = diagnosticsHolder.getDiagnostics() + diagnostics

        if (isErrorCandidate()) {
            return ErrorCallResolutionResult(resolvedCall, allDiagnostics, constraintSystem)
        }

        return if (type == ConstraintSystemCompletionMode.FULL) {
            CompletedCallResolutionResult(resolvedCall, allDiagnostics, constraintSystem)
        } else {
            PartialCallResolutionResult(resolvedCall, allDiagnostics, constraintSystem, forwardToInferenceSession)
        }
    }
}

internal fun ResolutionCandidate.isErrorCandidate(): Boolean {
    return ErrorUtils.isError(resolvedCall.candidateDescriptor) || hasContradiction
}
