/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.CandidateChosenUsingOverloadResolutionByLambdaAnnotation
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceSession
import org.jetbrains.kotlin.fir.resolve.inference.ResolvedLambdaAtom
import org.jetbrains.kotlin.fir.resolve.initialTypeOfCandidate
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.descriptorUtil.OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.utils.addToStdlib.same

class FirOverloadByLambdaReturnTypeResolver(
    val components: FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents
) {
    private val session = components.session
    private val callCompleter: FirCallCompleter
        get() = components.callCompleter
    private val inferenceSession: FirInferenceSession
        get() = components.transformer.context.inferenceSession

    fun <T> reduceCandidates(
        qualifiedAccess: T,
        allCandidates: Collection<Candidate>,
        bestCandidates: Set<Candidate>
    ): Set<Candidate> where T : FirStatement, T : FirResolvable {
        if (bestCandidates.size <= 1) return bestCandidates

        return reduceCandidatesImpl(
            qualifiedAccess,
            bestCandidates,
            allCandidates
        ) ?: bestCandidates
    }

    private fun <T> reduceCandidatesImpl(
        call: T,
        reducedCandidates: Set<Candidate>,
        allCandidates: Collection<Candidate>
    ): Set<Candidate>? where T : FirResolvable, T : FirStatement {
        val candidatesWithAnnotation = allCandidates.filter { candidate ->
            (candidate.symbol.fir as FirAnnotationContainer).annotations.any {
                it.annotationTypeRef.coneType.classId == OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION_CLASS_ID
            }
        }
        if (candidatesWithAnnotation.isEmpty()) return null
        val candidatesWithoutAnnotation = reducedCandidates - candidatesWithAnnotation
        val newCandidates =
            analyzeLambdaAndReduceNumberOfCandidatesRegardingOverloadResolutionByLambdaReturnType(call, reducedCandidates) ?: return null

        var maximallySpecificCandidates = components.callResolver.conflictResolver.chooseMaximallySpecificCandidates(newCandidates)
        if (maximallySpecificCandidates.size > 1 && candidatesWithoutAnnotation.any { it in maximallySpecificCandidates }) {
            maximallySpecificCandidates = maximallySpecificCandidates.toMutableSet().apply { removeAll(candidatesWithAnnotation) }
            maximallySpecificCandidates.singleOrNull()?.addDiagnostic(CandidateChosenUsingOverloadResolutionByLambdaAnnotation)
        }
        return maximallySpecificCandidates
    }

    private fun <T> analyzeLambdaAndReduceNumberOfCandidatesRegardingOverloadResolutionByLambdaReturnType(
        call: T,
        candidates: Set<Candidate>,
    ): Set<Candidate>? where T : FirResolvable, T : FirStatement {
        if (candidates.any { !it.isSuccessful }) return candidates
        val lambdas = candidates.flatMap { candidate ->
            candidate.postponedAtoms
                .filter { it is ResolvedLambdaAtom && !it.analyzed }
                .map { candidate to it as ResolvedLambdaAtom }
        }.groupBy { (_, atom) -> atom.atom }
            .values.singleOrNull()?.toMap() ?: return null

        if (!lambdas.values.same { it.parameters.size }) return null
        if (!lambdas.values.all { it.expectedType?.isSomeFunctionType(session) == true }) return null

        val originalCalleeReference = call.calleeReference

        for (candidate in lambdas.keys) {
            call.replaceCalleeReference(FirNamedReferenceWithCandidate(null, candidate.callInfo.name, candidate))
            callCompleter.runCompletionForCall(
                candidate,
                ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA,
                call,
                components.initialTypeOfCandidate(candidate)
            )
        }

        try {
            val inputTypesAreSame = lambdas.entries.same { (candidate, lambda) ->
                val substitutor = candidate.system.buildCurrentSubstitutor() as ConeSubstitutor
                lambda.inputTypes.map { substitutor.substituteOrSelf(it) }
            }
            if (!inputTypesAreSame) return null
            lambdas.entries.forEach { (candidate, atom) ->
                callCompleter.prepareLambdaAtomForFactoryPattern(atom, candidate)
            }
            val iterator = lambdas.entries.iterator()
            val (firstCandidate, firstAtom) = iterator.next()

            val postponedArgumentsAnalyzer = callCompleter.createPostponedArgumentsAnalyzer(
                components.transformer.resolutionContext
            )

            call.replaceCalleeReference(FirNamedReferenceWithCandidate(null, firstCandidate.callInfo.name, firstCandidate))
            val results = postponedArgumentsAnalyzer.analyzeLambda(
                firstCandidate.system,
                firstAtom,
                firstCandidate,
                forOverloadByLambdaReturnType = true,
                // we explicitly decided not to use PCLA in that case because this case didn't work before in K1
                withPCLASession = false,
            )
            while (iterator.hasNext()) {
                val (candidate, atom) = iterator.next()
                call.replaceCalleeReference(FirNamedReferenceWithCandidate(null, candidate.callInfo.name, candidate))
                postponedArgumentsAnalyzer.applyResultsOfAnalyzedLambdaToCandidateSystem(
                    candidate.system,
                    atom,
                    candidate,
                    results,
                )
            }

            val errorCandidates = mutableSetOf<Candidate>()
            val successfulCandidates = mutableSetOf<Candidate>()

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
        } finally {
            call.replaceCalleeReference(originalCalleeReference)
        }
    }
}
