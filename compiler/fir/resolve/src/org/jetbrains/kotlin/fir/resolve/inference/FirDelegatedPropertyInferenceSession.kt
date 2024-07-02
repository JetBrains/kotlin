/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.processPostponedAtoms
import org.jetbrains.kotlin.fir.resolve.initialTypeOfCandidate
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.FirCallCompletionResultsWriterTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull

class FirDelegatedPropertyInferenceSession(
    private val resolutionContext: ResolutionContext,
    private val callCompleter: FirCallCompleter,
    private val delegateExpression: FirExpression,
) : FirInferenceSession() {

    private val partiallyResolvedCalls: MutableList<Pair<FirResolvable, Candidate>> = mutableListOf()

    private val components: BodyResolveComponents
        get() = resolutionContext.bodyResolveComponents

    private val FirResolvable.candidate: Candidate
        get() = candidate()!!

    private val nonTrivialParentSession: FirInferenceSession? =
        resolutionContext.bodyResolveContext.inferenceSession.takeIf { it !== DEFAULT }

    val parentSessionIsNonTrivial: Boolean
        get() = nonTrivialParentSession != null

    private val delegateCandidate = (delegateExpression as? FirResolvable)?.candidate()
    private val parentConstraintSystem =
        delegateCandidate?.system
            ?: (resolutionContext.bodyResolveContext.inferenceSession as? FirPCLAInferenceSession)?.currentCommonSystem
            ?: components.session.inferenceComponents.createConstraintSystem()

    private val currentConstraintSystem =
        prepareSharedBaseSystem(parentConstraintSystem, components.session.inferenceComponents)

    val currentConstraintStorage: ConstraintStorage get() = currentConstraintSystem.currentStorage()

    private val unitType: ConeClassLikeType = components.session.builtinTypes.unitType.type

    private var wasCompletionRun = false

    override fun baseConstraintStorageForCandidate(candidate: Candidate, bodyResolveContext: BodyResolveContext): ConstraintStorage? {
        if (wasCompletionRun || !candidate.callInfo.callSite.isAnyOfDelegateOperators()) return null
        return currentConstraintStorage
    }

    override fun customCompletionModeInsteadOfFull(call: FirResolvable): ConstraintSystemCompletionMode? = when {
        call.isAnyOfDelegateOperators() && !wasCompletionRun -> ConstraintSystemCompletionMode.PARTIAL
        else -> null
    }

    override fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: ResolutionMode,
        completionMode: ConstraintSystemCompletionMode,
    ) where T : FirResolvable, T : FirStatement {
        if (wasCompletionRun || !call.isAnyOfDelegateOperators()) return

        requireCallIsDelegateOperator(call)

        val candidate = call.candidate

        // Ignore unsuccessful `provideDelegate` candidates
        // This behavior is aligned with a relevant part at FirDeclarationsResolveTransformer.transformWrappedDelegateExpression
        if (call.isProvideDelegate() && !candidate.isSuccessful) return

        val candidateSystem = candidate.system

        partiallyResolvedCalls.add(call to candidate)
        currentConstraintSystem.addOtherSystem(candidateSystem.currentStorage())
    }

    private fun <T> requireCallIsDelegateOperator(call: T) where T : FirResolvable, T : FirStatement {
        require(call.isAnyOfDelegateOperators()) {
            "Unexpected ${call.render()} call"
        }
    }

    private fun <T> T.isProvideDelegate() where T : FirResolvable, T : FirStatement =
        isAnyOfDelegateOperators() && (this as FirResolvable).candidate()?.callInfo?.name == OperatorNameConventions.PROVIDE_DELEGATE

    fun completeSessionOrPostponeIfNonRoot(onCompletionResultsWriting: (ConeSubstitutor) -> Unit) {
        check(!wasCompletionRun)
        wasCompletionRun = true

        parentConstraintSystem.addOtherSystem(currentConstraintStorage)

        (nonTrivialParentSession as? FirPCLAInferenceSession)?.apply {
            if (delegateCandidate != null) {
                require(delegateExpression is FirResolvable)
                callCompleter.runCompletionForCall(
                    delegateCandidate,
                    ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL,
                    delegateExpression,
                    components.initialTypeOfCandidate(delegateCandidate)
                )
            }

            integrateChildSession(
                buildList {
                    addIfNotNull(ConeResolutionAtom.createRawAtom(delegateExpression))
                    partiallyResolvedCalls.mapTo(this) { (expression, candidate) ->
                        ConeAtomWithCandidate(expression, candidate)
                    }
                },
                parentConstraintSystem.currentStorage(),
                onCompletionResultsWriting,
            )
            return
        }

        val completedCalls = completeCandidatesForRootSession()

        val finalSubstitutor = parentConstraintSystem.asReadOnlyStorage()
            .buildAbstractResultingSubstitutor(components.session.typeContext) as ConeSubstitutor

        val callCompletionResultsWriter = callCompleter.createCompletionResultsWriter(
            finalSubstitutor,
            // TODO: Get rid of the mode
            mode = FirCallCompletionResultsWriterTransformer.Mode.DelegatedPropertyCompletion
        )
        completedCalls.forEach {
            it.transformSingle(callCompletionResultsWriter, null)
        }

        onCompletionResultsWriting(finalSubstitutor)
    }

    private fun completeCandidatesForRootSession(): List<FirResolvable> {
        val parentSystem = parentConstraintSystem.apply { prepareForGlobalCompletion() }

        val notCompletedCalls = buildList {
            if (delegateExpression is FirResolvable) {
                val delegateCandidate = delegateExpression.candidate()
                if (delegateCandidate != null) {
                    add(ConeAtomWithCandidate(delegateExpression, delegateCandidate))
                }
            }
            partiallyResolvedCalls.mapNotNullTo(this) { (partiallyResolvedCall, _) ->
                val candidate = partiallyResolvedCall.candidate() ?: return@mapNotNullTo null
                ConeAtomWithCandidate(partiallyResolvedCall, candidate)
            }
        }

        resolutionContext.bodyResolveContext.withInferenceSession(DEFAULT) {
            components.callCompleter.completer.complete(
                parentSystem.asConstraintSystemCompleterContext(),
                ConstraintSystemCompletionMode.FULL,
                notCompletedCalls,
                unitType, resolutionContext
            ) { lambdaAtom, withPCLASession ->
                // Reversed here bc we want top-most call to avoid exponential visit
                val containingCandidateForLambda = notCompletedCalls.asReversed().first {
                    var found = false
                    it.processPostponedAtoms { postponedAtom ->
                        found = found || postponedAtom == lambdaAtom
                    }
                    found
                }.candidate
                callCompleter.createPostponedArgumentsAnalyzer(resolutionContext).analyze(
                    parentSystem,
                    lambdaAtom,
                    containingCandidateForLambda,
                    withPCLASession
                )
            }
        }

        for (candidate in notCompletedCalls.mapNotNull { (it.expression as FirResolvable).candidate() }) {
            for (error in parentSystem.errors) {
                candidate.addDiagnostic(InferenceError(error))
            }
        }

        return notCompletedCalls.map { it.fir }
    }
}

fun FirElement.isAnyOfDelegateOperators(): Boolean {
    if (this is FirPropertyAccessExpression) {
        val originalCall = this.candidate()?.callInfo?.callSite as? FirFunctionCall ?: return false
        return originalCall.isAnyOfDelegateOperators()
    }

    if (this !is FirFunctionCall || origin != FirFunctionCallOrigin.Operator) return false
    val name = calleeReference.name
    return name == OperatorNameConventions.PROVIDE_DELEGATE || name == OperatorNameConventions.GET_VALUE || name == OperatorNameConventions.SET_VALUE
}
