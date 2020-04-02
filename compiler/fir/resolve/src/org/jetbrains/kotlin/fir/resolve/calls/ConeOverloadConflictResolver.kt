/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.modality
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.TypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.results.FlatSignature
import org.jetbrains.kotlin.resolve.calls.results.SimpleConstraintSystem
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.utils.addToStdlib.cast

class ConeOverloadConflictResolver(
    specificityComparator: TypeSpecificityComparator,
    inferenceComponents: InferenceComponents
) : AbstractConeCallConflictResolver(specificityComparator, inferenceComponents) {

    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        discriminateAbstracts: Boolean
    ): Set<Candidate> {
        if (candidates.size == 1) return candidates
        val fixedCandidates =
            if (candidates.first().callInfo.candidateForCommonInvokeReceiver != null)
                chooseCandidatesWithMostSpecificInvokeReceiver(candidates)
            else
                candidates

        return chooseMaximallySpecificCandidates(
            fixedCandidates, discriminateGenerics, discriminateAbstracts, discriminateSAMs = true
        )
    }

    private fun chooseCandidatesWithMostSpecificInvokeReceiver(candidates: Set<Candidate>): Set<Candidate> {
        val propertyReceiverCandidates = candidates.mapTo(mutableSetOf()) {
            it.callInfo.candidateForCommonInvokeReceiver
                ?: error("If one candidate within a group is property+invoke, other should be the same, but $it found")
        }

        val bestInvokeReceiver =
            chooseMaximallySpecificCandidates(propertyReceiverCandidates, discriminateGenerics = false)
                .singleOrNull() ?: return candidates

        return candidates.filterTo(mutableSetOf()) { it.callInfo.candidateForCommonInvokeReceiver == bestInvokeReceiver }
    }

    private fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        discriminateAbstracts: Boolean,
        discriminateSAMs: Boolean
    ): Set<Candidate> {
        findMaximallySpecificCall(candidates, false)?.let { return setOf(it) }

        if (discriminateGenerics) {
            findMaximallySpecificCall(candidates, true)?.let { return setOf(it) }
        }

        if (discriminateSAMs) {
            val filtered = candidates.filterTo(mutableSetOf()) { !it.usesSAM }
            when (filtered.size) {
                1 -> return filtered
                0, candidates.size -> {
                }
                else -> return chooseMaximallySpecificCandidates(
                    filtered, discriminateGenerics, discriminateAbstracts, discriminateSAMs = false
                )
            }
        }

        if (discriminateAbstracts) {
            val filtered = candidates.filterTo(mutableSetOf()) { (it.symbol.fir as? FirMemberDeclaration)?.modality != Modality.ABSTRACT }
            when (filtered.size) {
                1 -> return filtered
                0, candidates.size -> {
                }
                else -> return chooseMaximallySpecificCandidates(
                    filtered, discriminateGenerics, discriminateAbstracts = false, discriminateSAMs = false
                )
            }
        }

        return candidates
    }

    private fun findMaximallySpecificCall(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean//,
        //isDebuggerContext: Boolean
    ): Candidate? {
        if (candidates.size <= 1) return candidates.singleOrNull()

        val conflictingCandidates = candidates.map { candidateCall ->
            createFlatSignature(candidateCall)
        }

        val bestCandidatesByParameterTypes = conflictingCandidates.filter { candidate ->
            isMostSpecific(candidate, conflictingCandidates) { call1, call2 ->
                isNotLessSpecificCallWithArgumentMapping(call1, call2, discriminateGenerics)
            }
        }

        return bestCandidatesByParameterTypes.exactMaxWith { call1, call2 ->
            isOfNotLessSpecificShape(call1, call2)// && isOfNotLessSpecificVisibilityForDebugger(call1, call2, isDebuggerContext)
        }?.origin
    }


    private inline fun <C : Any> Collection<C>.exactMaxWith(isNotWorse: (C, C) -> Boolean): C? {
        var result: C? = null
        for (candidate in this) {
            if (result == null || isNotWorse(candidate, result)) {
                result = candidate
            }
        }
        if (result == null) return null
        if (any { it != result && isNotWorse(it, result) }) {
            return null
        }
        return result
    }

    private inline fun <C> isMostSpecific(candidate: C, candidates: Collection<C>, isNotLessSpecific: (C, C) -> Boolean): Boolean =
        candidates.all { other ->
            candidate === other ||
                    isNotLessSpecific(candidate, other)
        }

    /**
     * `call1` is not less specific than `call2`
     */
    private fun isNotLessSpecificCallWithArgumentMapping(
        call1: FlatSignature<Candidate>,
        call2: FlatSignature<Candidate>,
        discriminateGenerics: Boolean
    ): Boolean {
        return compareCallsByUsedArguments(
            call1,
            call2,
            discriminateGenerics
        )
    }

    private fun isOfNotLessSpecificShape(
        call1: FlatSignature<Candidate>,
        call2: FlatSignature<Candidate>
    ): Boolean {
        val hasVarargs1 = call1.hasVarargs
        val hasVarargs2 = call2.hasVarargs
        if (hasVarargs1 && !hasVarargs2) return false
        if (!hasVarargs1 && hasVarargs2) return true

        if (call1.numDefaults > call2.numDefaults) {
            return false
        }

        return true
    }
}

object NoSubstitutor : TypeSubstitutorMarker

class ConeSimpleConstraintSystemImpl(val system: NewConstraintSystemImpl) : SimpleConstraintSystem {
    override fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): TypeSubstitutorMarker = with(context) {
        val csBuilder = system.getBuilder()
        val substitutionMap = typeParameters.associateWith {
            require(it is FirTypeParameterSymbol)
            val variable = TypeParameterBasedTypeVariable(it)
            csBuilder.registerVariable(variable)

            variable.defaultType
        }
        val substitutor = substitutorByMap(substitutionMap.cast())
        for (typeParameter in typeParameters) {
            require(typeParameter is FirTypeParameterSymbol)
            for (upperBound in typeParameter.fir.bounds) {
                addSubtypeConstraint(
                    substitutionMap[typeParameter] ?: error("No ${typeParameter.fir.render()} in substitution map"),
                    substitutor.substituteOrSelf(upperBound.coneTypeUnsafe())
                )
            }
        }
        return substitutor
    }

    override fun addSubtypeConstraint(subType: KotlinTypeMarker, superType: KotlinTypeMarker) {
        system.addSubtypeConstraint(subType, superType, SimpleConstraintSystemConstraintPosition)
    }

    override fun hasContradiction(): Boolean = system.hasContradiction

    override val captureFromArgument: Boolean
        get() = true

    override val context: TypeSystemInferenceExtensionContext
        get() = system

}
