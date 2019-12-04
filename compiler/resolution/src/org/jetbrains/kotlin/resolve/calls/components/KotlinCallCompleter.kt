/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.synthetic.SyntheticMemberDescriptor
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage.Empty.hasContradiction
import org.jetbrains.kotlin.resolve.calls.inference.model.ExpectedTypeConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.forceResolution
import org.jetbrains.kotlin.resolve.descriptorUtil.hasExactAnnotation
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.types.model.isIntegerLiteralTypeConstructor
import org.jetbrains.kotlin.types.model.typeConstructor
import org.jetbrains.kotlin.types.typeUtil.contains

class KotlinCallCompleter(
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    private val kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter
) {

    fun runCompletion(
        factory: SimpleCandidateFactory,
        candidates: Collection<KotlinResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): CallResolutionResult {
        val diagnosticHolder = KotlinDiagnosticsHolder.SimpleHolder()
        when {
            candidates.isEmpty() -> diagnosticHolder.addDiagnostic(NoneCandidatesCallDiagnostic(factory.kotlinCall))
            candidates.size > 1 -> diagnosticHolder.addDiagnostic(ManyCandidatesCallDiagnostic(factory.kotlinCall, candidates))
        }

        val candidate = prepareCandidateForCompletion(factory, candidates, resolutionCallbacks)
        val returnType = candidate.substitutedReturnType()

        candidate.addExpectedTypeConstraint(returnType, expectedType)
        candidate.addExpectedTypeFromCastConstraint(returnType, resolutionCallbacks)
        candidate.checkSamWithVararg(diagnosticHolder)

        return if (resolutionCallbacks.inferenceSession.shouldRunCompletion(candidate))
            candidate.runCompletion(
                candidate.computeCompletionMode(expectedType, returnType),
                diagnosticHolder,
                resolutionCallbacks
            )
        else
            candidate.asCallResolutionResult(ConstraintSystemCompletionMode.PARTIAL, diagnosticHolder)
    }

    private fun KotlinResolutionCandidate.checkSamWithVararg(diagnosticHolder: KotlinDiagnosticsHolder.SimpleHolder) {
        val samConversionPerArgumentWithWarningsForVarargAfterSam =
            callComponents.languageVersionSettings.supportsFeature(LanguageFeature.SamConversionPerArgument) &&
                    !callComponents.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitVarargAsArrayAfterSamArgument)

        if (samConversionPerArgumentWithWarningsForVarargAfterSam && resolvedCall.candidateDescriptor is SyntheticMemberDescriptor<*>) {
            val declarationDescriptor = resolvedCall.candidateDescriptor.baseDescriptorForSynthetic as? FunctionDescriptor ?: return

            if (declarationDescriptor.valueParameters.lastOrNull()?.isVararg == true) {
                diagnosticHolder.addDiagnostic(
                    ResolvedToSamWithVarargDiagnostic(resolvedCall.atom.argumentsInParenthesis.lastOrNull() ?: return)
                )
            }
        }
    }

    fun createAllCandidatesResult(
        candidates: Collection<KotlinResolutionCandidate>,
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

            CandidateWithDiagnostics(candidate, diagnosticsHolder.getDiagnostics() + candidate.diagnosticsFromResolutionParts)
        }
        return AllCandidatesResolutionResult(completedCandidates)
    }

    private fun KotlinResolutionCandidate.runCompletion(
        completionType: ConstraintSystemCompletionMode,
        diagnosticHolder: KotlinDiagnosticsHolder.SimpleHolder,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): CallResolutionResult {
        if (isErrorCandidate()) {
            runCompletion(resolvedCall, ConstraintSystemCompletionMode.FULL, diagnosticHolder, getSystem(), resolutionCallbacks)
            return asCallResolutionResult(completionType, diagnosticHolder)
        }

        runCompletion(resolvedCall, completionType, diagnosticHolder, getSystem(), resolutionCallbacks)

        return asCallResolutionResult(completionType, diagnosticHolder)
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
            returnType
        ) {
            if (collectAllCandidatesMode) {
                it.setEmptyAnalyzedResults()
            } else {
                postponedArgumentsAnalyzer.analyze(
                    constraintSystem.asPostponedArgumentsAnalyzerContext(),
                    resolutionCallbacks,
                    it,
                    diagnosticsHolder
                )
            }
        }

        constraintSystem.diagnostics.forEach(diagnosticsHolder::addDiagnostic)
    }

    private fun prepareCandidateForCompletion(
        factory: SimpleCandidateFactory,
        candidates: Collection<KotlinResolutionCandidate>,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): KotlinResolutionCandidate {
        val candidate = candidates.singleOrNull()

        // this is needed at least for non-local return checker, because when we analyze lambda we should already bind descriptor for outer call
        candidate?.resolvedCall?.let {
            resolutionCallbacks.bindStubResolvedCallForCandidate(it)
            resolutionCallbacks.disableContractsIfNecessary(it)
        }

        return candidate ?: factory.createErrorCandidate().forceResolution()
    }

    private fun KotlinResolutionCandidate.substitutedReturnType(): UnwrappedType? {
        val returnType = resolvedCall.candidateDescriptor.returnType?.unwrap() ?: return null
        return resolvedCall.freshVariablesSubstitutor.safeSubstitute(returnType)
    }

    private fun KotlinResolutionCandidate.addExpectedTypeConstraint(
        returnType: UnwrappedType?,
        expectedType: UnwrappedType?
    ) {
        if (returnType == null) return
        if (expectedType == null || (TypeUtils.noExpectedType(expectedType) && expectedType !== TypeUtils.UNIT_EXPECTED_TYPE)) return

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
                csBuilder.addSubtypeConstraintIfCompatible(
                    returnType, csBuilder.builtIns.unitType, ExpectedTypeConstraintPosition(resolvedCall.atom)
                )

            else ->
                csBuilder.addSubtypeConstraint(returnType, expectedType, ExpectedTypeConstraintPosition(resolvedCall.atom))
        }
    }

    private fun KotlinResolutionCandidate.addExpectedTypeFromCastConstraint(
        returnType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ) {
        if (!callComponents.languageVersionSettings.supportsFeature(LanguageFeature.ExpectedTypeFromCast)) return
        if (returnType == null) return
        val expectedType = resolutionCallbacks.getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedCall) ?: return
        csBuilder.addSubtypeConstraint(returnType, expectedType, ExpectedTypeConstraintPosition(resolvedCall.atom))
    }

    private fun KotlinResolutionCandidate.computeCompletionMode(
        expectedType: UnwrappedType?,
        currentReturnType: UnwrappedType?
    ): ConstraintSystemCompletionMode {
        // Presence of expected type means that we trying to complete outermost call => completion mode should be full
        if (expectedType != null) return ConstraintSystemCompletionMode.FULL

        // This is questionable as null return type can be only for error call
        if (currentReturnType == null) return ConstraintSystemCompletionMode.PARTIAL

        return when {
            // Consider call foo(bar(x)), if return type of bar is a proper one, then we can complete resolve for bar => full completion mode
            // Otherwise, we shouldn't complete bar until we process call foo
            csBuilder.isProperType(currentReturnType) -> ConstraintSystemCompletionMode.FULL

            // Nested call is connected with the outer one through the UPPER constraint (returnType <: expectedOuterType)
            // This means that there will be no new LOWER constraints =>
            //   it's possible to complete call now if there are proper LOWER constraints
            csBuilder.isTypeVariable(currentReturnType) ->
                if (hasProperNonTrivialLowerConstraints(currentReturnType))
                    ConstraintSystemCompletionMode.FULL
                else
                    ConstraintSystemCompletionMode.PARTIAL

            // Return type has proper equal constraints => there is no need in the outer call
            containsTypeVariablesWithProperEqualConstraints(currentReturnType) -> ConstraintSystemCompletionMode.FULL

            else -> ConstraintSystemCompletionMode.PARTIAL
        }
    }

    private fun KotlinResolutionCandidate.containsTypeVariablesWithProperEqualConstraints(type: UnwrappedType): Boolean {
        for ((variableConstructor, variableWithConstraints) in csBuilder.currentStorage().notFixedTypeVariables) {
            if (!type.contains { it.constructor == variableConstructor }) continue

            val constraints = variableWithConstraints.constraints
            val onlyProperEqualConstraints =
                constraints.isNotEmpty() && constraints.any { it.kind.isEqual() && csBuilder.isProperType(it.type) }

            if (!onlyProperEqualConstraints) return false
        }

        return true
    }

    private fun KotlinResolutionCandidate.hasProperNonTrivialLowerConstraints(typeVariable: UnwrappedType): Boolean {
        assert(csBuilder.isTypeVariable(typeVariable)) { "$typeVariable is not a type variable" }

        val context = getSystem() as TypeSystemInferenceExtensionContext
        val constructor = typeVariable.constructor
        val variableWithConstraints = csBuilder.currentStorage().notFixedTypeVariables[constructor] ?: return false
        val constraints = variableWithConstraints.constraints
        return constraints.isNotEmpty() && constraints.anyOrAll(requireAll = typeVariable.hasExactAnnotation()) {
            !it.type.typeConstructor(context).isIntegerLiteralTypeConstructor(context) &&
                    (it.kind.isLower() || it.kind.isEqual()) &&
                    csBuilder.isProperType(it.type)
        }
    }

    private inline fun <T> Iterable<T>.anyOrAll(requireAll: Boolean, p: (T) -> Boolean): Boolean =
        if (requireAll) all(p) else any(p)

    fun KotlinResolutionCandidate.asCallResolutionResult(
        type: ConstraintSystemCompletionMode,
        diagnosticsHolder: KotlinDiagnosticsHolder.SimpleHolder
    ): CallResolutionResult {
        val systemStorage = getSystem().asReadOnlyStorage()
        val allDiagnostics = diagnosticsHolder.getDiagnostics() + this.diagnosticsFromResolutionParts

        if (isErrorCandidate()) {
            return ErrorCallResolutionResult(resolvedCall, allDiagnostics, systemStorage)
        }

        return if (type == ConstraintSystemCompletionMode.FULL) {
            CompletedCallResolutionResult(resolvedCall, allDiagnostics, systemStorage)
        } else {
            PartialCallResolutionResult(resolvedCall, allDiagnostics, systemStorage)
        }
    }

    private fun KotlinResolutionCandidate.isErrorCandidate(): Boolean {
        return ErrorUtils.isError(resolvedCall.candidateDescriptor) || hasContradiction
    }
}
