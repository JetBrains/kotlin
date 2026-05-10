/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.overloads

import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
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
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.utils.addToStdlib.same

// Get rid of it once explicit context parameters support is available in stable IJ
fun runEagerLambdaAnalysisAndFilterOutInapplicableCandidates(
    candidates: Set<Candidate>,
    components: BodyResolveComponents,
): Set<Candidate> = context(components) {
    runEagerLambdaAnalysisAndFilterOutInapplicableCandidates(candidates)
}

context(components: BodyResolveComponents)
private tailrec fun runEagerLambdaAnalysisAndFilterOutInapplicableCandidates(
    candidates: Set<Candidate>,
): Set<Candidate> {
    check(candidates.isNotEmpty())
    if (candidates.size == 1) return candidates

    // No lambda can be analyzed
    if (!runEagerLambdaAnalysisForFirstReadyLambda(candidates)) return candidates

    val remainingSuccessfulCandidates =
        candidates.filterTo(mutableSetOf()) { it.isSuccessful }

    // Some candidates are still successful and there might be other ready lambdas, so repeat ELA again
    if (remainingSuccessfulCandidates.isNotEmpty()) {
        return runEagerLambdaAnalysisAndFilterOutInapplicableCandidates(remainingSuccessfulCandidates)
    }

    // If only unsuccessful candidates remain, return the first one.
    // We might also return all of them to report OVERLOAD_RESOLUTION_AMBIGUITY, but we preserve the current test data behavior
    // where we report RETURN_TYPE_MISMATCH on the first candidate.
    return setOf(candidates.first())
}

/**
 * @returns false if no lambda has been analyzed
 */

context(components: BodyResolveComponents)
private fun runEagerLambdaAnalysisForFirstReadyLambda(
    candidates: Set<Candidate>,
): Boolean {
    if (candidates.any { !it.isSuccessful || it.hasNonTrivialContracts() || it.callInfo.isCollectionLiteralCall }) return false
    val call = candidates.first().callInfo.callSite as? FirFunctionCall ?: return false

    // NB: for each `lambdaAtomGroup` all the atoms refer to the same FirAnonymousFunction
    val lambdaAtomGroups = candidates.lambdaAtomGroups()
    if (lambdaAtomGroups.isEmpty()) return false

    val originalCalleeReference = call.calleeReference

    try {
        for (lambdaAtomGroup in lambdaAtomGroups) {
            if (!lambdaAtomGroup.same { it.atom.parameterTypes.size }) continue
            if (!lambdaAtomGroup.all { it.atom.expectedType?.isSomeFunctionType(components.session) == true }) continue
            if (!runEagerLambdaAnalysisForLambdaAtomGroup(lambdaAtomGroup, call)) continue
            return true
        }
    } finally {
        call.replaceCalleeReference(originalCalleeReference)
    }

    return false
}

/**
 * @return false if it's impossible to run ELA because of different input types
 */
@OptIn(ConstraintSystemCompletionMode.ExclusiveForOverloadResolutionByLambdaReturnType::class)
context(components: BodyResolveComponents)
private fun runEagerLambdaAnalysisForLambdaAtomGroup(
    // All the atoms refer to the same FirAnonymousFunction
    lambdaAtomGroup: Collection<LambdaAtomWithCandidate>,
    call: FirFunctionCall,
): Boolean {
    val inferenceSession = components.context.inferenceSession
    val callCompleter = components.callCompleter

    for ((candidate, atom) in lambdaAtomGroup) {
        call.replaceCalleeReference(candidate.temporaryNamedReference())
        callCompleter.runCompletionForCall(
            candidate,
            ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA,
            call,
            components.initialTypeOfCandidate(candidate)
        )
        for (inputType in atom.inputTypes) {
            inferenceSession.semiFixTypeVariablesAllowingFixationToOtherOnes(inputType, myCs = candidate.system)
        }
    }

    val semiFixedVariables = inferenceSession.semiFixedVariables
    if (!lambdaAtomGroup.inputTypesAreTheSame(semiFixedVariables)) return false

    val iterator = lambdaAtomGroup.iterator()
    val (firstCandidate, firstAtom) = iterator.next()

    val postponedArgumentsAnalyzer = callCompleter.createPostponedArgumentsAnalyzer(
        components.resolutionContext
    )

    call.replaceCalleeReference(firstCandidate.temporaryNamedReference())
    val results = postponedArgumentsAnalyzer.analyzeLambda(
        firstCandidate.system,
        firstAtom,
        firstCandidate,
        forOverloadByLambdaReturnType = true,
        // we explicitly decided not to use PCLA in that case because this case didn't work before in K1
        withPCLASession = false,
        allowFixationToOtherTypeVariables = semiFixedVariables.isNotEmpty()
    )

    // NB: Results from the first atom have been already applied to the candidate
    while (iterator.hasNext()) {
        val (candidate, atom) = iterator.next()
        call.replaceCalleeReference(candidate.temporaryNamedReference())
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
            forEagerLambdaAnalysis = true,
        ) { substitutor.substituteOrSelf(it) }
    }

    return true
}

private fun Collection<LambdaAtomWithCandidate>.inputTypesAreTheSame(
    // PCLA-only
    semiFixedVariables: Map<TypeConstructorMarker, KotlinTypeMarker>,
): Boolean = same { (candidate, lambda) ->
    val substitutor = candidate.system.buildCurrentSubstitutor(semiFixedVariables).asCone()
    lambda.inputTypes.map { substitutor.substituteOrSelf(it) }
}

private fun Candidate.temporaryNamedReference() = FirNamedReferenceWithCandidate(null, callInfo.name, this)

private class LambdaAtomWithCandidate(
    val candidate: Candidate,
    val atom: ConeResolvedLambdaAtom,
) {
    operator fun component1(): Candidate = candidate
    operator fun component2(): ConeResolvedLambdaAtom = atom
}

/**
 * Returns a collection of `LambdaAtomsGroupForSpecificAnonymousFunction`, where each group contains
 * lambda atoms and their associated candidates for a specific anonymous function.
 */
private fun Collection<Candidate>.lambdaAtomGroups(): Collection<Collection<LambdaAtomWithCandidate>> {
    val lambdaAtomsGroupedByAnonymousFunction: Map<FirAnonymousFunction, MutableList<LambdaAtomWithCandidate>> = buildMap {
        for (candidate in this@lambdaAtomGroups) {
            for (atom in candidate.postponedAtoms) {
                if (atom is ConeResolvedLambdaAtom && !atom.analyzed) {
                    this.getOrPut(atom.anonymousFunction, ::mutableListOf).add(
                        LambdaAtomWithCandidate(candidate, atom)
                    )
                }
            }
        }
    }

    return lambdaAtomsGroupedByAnonymousFunction.values
}

private fun Candidate.hasNonTrivialContracts(): Boolean = (symbol.fir as? FirContractDescriptionOwner)?.contractDescription != null
