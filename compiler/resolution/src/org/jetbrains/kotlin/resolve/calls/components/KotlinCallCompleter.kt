/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.ExpectedTypeConstraintPosition
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.forceResolution
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType

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
        if (candidates.isEmpty()) {
            diagnosticHolder.addDiagnostic(NoneCandidatesCallDiagnostic(factory.kotlinCall))
        }
        if (candidates.size > 1) {
            diagnosticHolder.addDiagnostic(ManyCandidatesCallDiagnostic(factory.kotlinCall, candidates))
        }
        val candidate = candidates.singleOrNull()

        // this is needed at least for non-local return checker, because when we analyze lambda we should already bind descriptor for outer call
        candidate?.resolvedCall?.let { resolutionCallbacks.bindStubResolvedCallForCandidate(it) }

        if (candidate == null || candidate.csBuilder.hasContradiction) {
            val candidateForCompletion = candidate ?: factory.createErrorCandidate().forceResolution()
            candidateForCompletion.prepareForCompletion(expectedType, resolutionCallbacks)
            runCompletion(
                candidateForCompletion.resolvedCall,
                ConstraintSystemCompletionMode.FULL,
                diagnosticHolder,
                candidateForCompletion.getSystem(),
                resolutionCallbacks
            )

            return candidate.asCallResolutionResult(CallResolutionResult.Type.ERROR, diagnosticHolder)
        }

        val completionType = candidate.prepareForCompletion(expectedType, resolutionCallbacks)
        val constraintSystem = candidate.getSystem()
        runCompletion(candidate.resolvedCall, completionType, diagnosticHolder, constraintSystem, resolutionCallbacks)

        val callResolutionType = if (completionType == ConstraintSystemCompletionMode.FULL)
            CallResolutionResult.Type.COMPLETED
        else
            CallResolutionResult.Type.PARTIAL

        return candidate.asCallResolutionResult(callResolutionType, diagnosticHolder)
    }

    fun createAllCandidatesResult(
        candidates: Collection<KotlinResolutionCandidate>,
        expectedType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): CallResolutionResult {
        val diagnosticsHolder = KotlinDiagnosticsHolder.SimpleHolder()
        for (candidate in candidates) {
            candidate.prepareForCompletion(expectedType, resolutionCallbacks)
            runCompletion(
                candidate.resolvedCall,
                ConstraintSystemCompletionMode.FULL,
                diagnosticsHolder,
                candidate.getSystem(),
                resolutionCallbacks,
                collectAllCandidatesMode = true
            )
        }
        return CallResolutionResult(CallResolutionResult.Type.ALL_CANDIDATES, null, emptyList(), ConstraintStorage.Empty, candidates)
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
            resolvedCallAtom,
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


    // true if we should complete this call
    private fun KotlinResolutionCandidate.prepareForCompletion(
        expectedType: UnwrappedType?,
        resolutionCallbacks: KotlinResolutionCallbacks
    ): ConstraintSystemCompletionMode {
        val unsubstitutedReturnType = resolvedCall.candidateDescriptor.returnType?.unwrap() ?: return ConstraintSystemCompletionMode.PARTIAL
        val withSmartCastInfo = resolutionCallbacks.createReceiverWithSmartCastInfo(resolvedCall)

        val actualType = withSmartCastInfo?.stableType ?: unsubstitutedReturnType

        val returnType = resolvedCall.substitutor.substituteKeepAnnotations(actualType)
        if (expectedType != null && !TypeUtils.noExpectedType(expectedType) && !resolutionCallbacks.isCompileTimeConstant(
                resolvedCall,
                expectedType
            )) {
            csBuilder.addSubtypeConstraint(returnType, expectedType, ExpectedTypeConstraintPosition(resolvedCall.atom))
        }

        return if (expectedType != null || csBuilder.isProperType(returnType)) {
            ConstraintSystemCompletionMode.FULL
        } else {
            ConstraintSystemCompletionMode.PARTIAL
        }
    }

    private fun KotlinResolutionCandidate?.asCallResolutionResult(
        type: CallResolutionResult.Type,
        diagnosticsHolder: KotlinDiagnosticsHolder.SimpleHolder
    ): CallResolutionResult {
        val diagnosticsFromResolutionParts = this?.diagnosticsFromResolutionParts ?: emptyList<KotlinCallDiagnostic>()
        val systemStorage = this?.getSystem()?.asReadOnlyStorage() ?: ConstraintStorage.Empty

        return CallResolutionResult(
            type,
            this?.resolvedCall,
            diagnosticsHolder.getDiagnostics() + diagnosticsFromResolutionParts,
            systemStorage
        )
    }
}