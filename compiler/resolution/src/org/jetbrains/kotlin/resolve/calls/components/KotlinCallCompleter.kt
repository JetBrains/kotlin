/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.TrivialConstraintTypeInferenceOracle
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage.Empty.hasContradiction
import org.jetbrains.kotlin.resolve.calls.inference.model.ExpectedTypeConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.forceResolution
import org.jetbrains.kotlin.resolve.constants.IntegerValueTypeConstructor
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.IntersectionTypeConstructor
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberType

class KotlinCallCompleter(
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    private val kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter,
    private val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle
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
        val returnType = candidate.returnTypeWithSmartCastInfo(resolutionCallbacks)

        candidate.addExpectedTypeConstraint(returnType, expectedType, resolutionCallbacks)
        candidate.addExpectedTypeFromCastConstraint(returnType, resolutionCallbacks)

        return if (resolutionCallbacks.inferenceSession.shouldRunCompletion(candidate))
            candidate.runCompletion(
                candidate.computeCompletionMode(expectedType, returnType),
                diagnosticHolder,
                resolutionCallbacks
            )
        else
            candidate.asCallResolutionResult(ConstraintSystemCompletionMode.PARTIAL, diagnosticHolder)
    }

    fun createAllCandidatesResult(
        candidates: Collection<KotlinResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): CallResolutionResult {
        val diagnosticsHolder = KotlinDiagnosticsHolder.SimpleHolder()
        for (candidate in candidates) {
            candidate.addExpectedTypeConstraint(
                candidate.returnTypeWithSmartCastInfo(resolutionCallbacks), expectedType, resolutionCallbacks
            )

            runCompletion(
                candidate.resolvedCall,
                ConstraintSystemCompletionMode.FULL,
                diagnosticsHolder,
                candidate.getSystem(),
                resolutionCallbacks,
                collectAllCandidatesMode = true
            )
        }
        return AllCandidatesResolutionResult(candidates)
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
        candidate?.resolvedCall?.let { resolutionCallbacks.bindStubResolvedCallForCandidate(it) }

        return candidate ?: factory.createErrorCandidate().forceResolution()
    }

    private fun KotlinResolutionCandidate.returnTypeWithSmartCastInfo(resolutionCallbacks: KotlinResolutionCallbacks): UnwrappedType? {
        val returnType = resolvedCall.candidateDescriptor.returnType?.unwrap() ?: return null
        val returnTypeWithSmartCastInfo = computeReturnTypeWithSmartCastInfo(returnType, resolutionCallbacks)
        return resolvedCall.substitutor.substituteKeepAnnotations(returnTypeWithSmartCastInfo)
    }

    private fun KotlinResolutionCandidate.addExpectedTypeConstraint(
        returnType: UnwrappedType?,
        expectedType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ) {
        if (returnType == null) return
        if (expectedType == null || TypeUtils.noExpectedType(expectedType)) return

        // We don't add expected type constraint for constant expression like "1 + 1" because of type coercion for numbers:
        // val a: Long = 1 + 1, note that result type of "1 + 1" will be Int and adding constraint with Long will produce type mismatch
        if (!resolutionCallbacks.isCompileTimeConstant(resolvedCall, expectedType)) {
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

            else -> ConstraintSystemCompletionMode.PARTIAL
        }
    }

    private fun KotlinResolutionCandidate.hasProperNonTrivialLowerConstraints(typeVariable: UnwrappedType): Boolean {
        assert(csBuilder.isTypeVariable(typeVariable)) { "$typeVariable is not a type variable" }

        val constructor = typeVariable.constructor
        val variableWithConstraints = csBuilder.currentStorage().notFixedTypeVariables[constructor] ?: return false
        return variableWithConstraints.constraints.all {
            !trivialConstraintTypeInferenceOracle.isTrivialConstraint(it) && !it.type.isIntegerValueType() &&
                    it.kind.isLower() && csBuilder.isProperType(it.type)
        }
    }

    private fun UnwrappedType.isIntegerValueType(): Boolean {
        if (constructor is IntegerValueTypeConstructor) return true
        if (constructor is IntersectionTypeConstructor)
            return constructor.supertypes.all { it.isPrimitiveNumberType() }

        return false
    }

    private fun KotlinResolutionCandidate.computeReturnTypeWithSmartCastInfo(
        returnType: UnwrappedType,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): UnwrappedType {
        if (resolvedCall.atom.callKind != KotlinCallKind.VARIABLE) return returnType
        return resolutionCallbacks.createReceiverWithSmartCastInfo(resolvedCall)?.stableType ?: returnType
    }

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
