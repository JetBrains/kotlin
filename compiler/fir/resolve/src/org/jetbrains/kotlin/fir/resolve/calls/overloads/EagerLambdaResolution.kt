/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.overloads

import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.ConeLambdaWithTypeVariableAsExpectedTypeAtom
import org.jetbrains.kotlin.fir.resolve.calls.ConeResolutionAtomWithPostponedChild
import org.jetbrains.kotlin.fir.resolve.calls.ConeResolvedLambdaAtom
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.initialTypeOfCandidate
import org.jetbrains.kotlin.fir.resolve.substitution.asCone
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.utils.addToStdlib.same

@OptIn(ConstraintSystemCompletionMode.ExclusiveForOverloadResolutionByLambdaReturnType::class)
fun runEagerLambdaAnalysisAndFilterOutInapplicableCandidates(
    candidates: Set<Candidate>,
    components: BodyResolveComponents,
): Set<Candidate>? {
    if (candidates.any { !it.isSuccessful }) return null
    val call = candidates.first().callInfo.callSite as? FirFunctionCall ?: return null

    val lambdas = candidates.flatMap { candidate ->
        candidate.postponedAtoms
            .filter { it is ConeResolvedLambdaAtom && !it.analyzed }
            .map { candidate to it as ConeResolvedLambdaAtom }
    }.groupBy { (_, atom) -> atom.anonymousFunction }
        .values.singleOrNull()?.toMap() ?: return null

    if (!lambdas.values.same { it.parameterTypes.size }) return null
    if (!lambdas.values.all { it.expectedType?.isSomeFunctionType(components.session) == true }) return null

    val callCompleter = components.callCompleter
    val originalCalleeReference = call.calleeReference

    try {
        val inferenceSession = components.context.inferenceSession
        for ((candidate, lambda) in lambdas) {
            call.replaceCalleeReference(FirNamedReferenceWithCandidate(null, candidate.callInfo.name, candidate))
            callCompleter.runCompletionForCall(
                candidate,
                ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA,
                call,
                components.initialTypeOfCandidate(candidate)
            )
            for (inputType in lambda.inputTypes) {
                inferenceSession.semiFixTypeVariablesAllowingFixationToOtherOnes(inputType, myCs = candidate.system)
            }
        }

        val semiFixedVariables = inferenceSession.semiFixedVariables
        val inputTypesAreSame = lambdas.entries.same { (candidate, lambda) ->
            val substitutor = candidate.system.buildCurrentSubstitutor(semiFixedVariables).asCone()
            lambda.inputTypes.map { substitutor.substituteOrSelf(it) }
        }
        if (!inputTypesAreSame) return null
        val iterator = lambdas.entries.iterator()
        val (firstCandidate, firstAtom) = iterator.next()

        val postponedArgumentsAnalyzer = callCompleter.createPostponedArgumentsAnalyzer(
            components.resolutionContext
        )

        call.replaceCalleeReference(FirNamedReferenceWithCandidate(null, firstCandidate.callInfo.name, firstCandidate))
        val results = postponedArgumentsAnalyzer.analyzeLambda(
            firstCandidate.system,
            firstAtom,
            firstCandidate,
            forOverloadByLambdaReturnType = true,
            // we explicitly decided not to use PCLA in that case because this case didn't work before in K1
            withPCLASession = false,
            allowFixationToOtherTypeVariables = semiFixedVariables.isNotEmpty()
        )
        while (iterator.hasNext()) {
            val (candidate, atom) = iterator.next()
            call.replaceCalleeReference(FirNamedReferenceWithCandidate(null, candidate.callInfo.name, candidate))
            val substitutor = candidate.system.buildCurrentSubstitutor(semiFixedVariables).asCone()
            postponedArgumentsAnalyzer.applyResultsOfAnalyzedLambdaToCandidateSystem(
                candidate.system,
                atom,
                candidate,
                results.copy(
                    returnArguments = results.returnArguments.map {
                        if (it !is ConeResolutionAtomWithPostponedChild) it
                        /**
                         * This atom may already have a sub-atom bound to a previous candidate,
                         * like [ConeLambdaWithTypeVariableAsExpectedTypeAtom] (we don't know yet a return type of final candidate),
                         * and in this state we cannot validly start resolve of a current candidate.
                         * For this reason we copy these atoms and reset their sub-atoms.
                         */
                        else it.makeFreshCopy()
                    }
                ),
            ) { substitutor.substituteOrSelf(it) }
        }

        val errorCandidates = mutableSetOf<Candidate>()
        val successfulCandidates = mutableSetOf<Candidate>()

        for (candidate in candidates) {
            if (candidate.isSuccessful) {
                successfulCandidates += candidate
            } else {
                // TODO: Use for reporting RETURN_TYPE_MISMATCH to avoid test data changes
                if (errorCandidates.isEmpty()) {
                    errorCandidates += candidate
                }
            }
        }
        return when {
            successfulCandidates.isNotEmpty() -> successfulCandidates
            else -> errorCandidates
        }
    } finally {
        call.replaceCalleeReference(originalCalleeReference)
    }
}
