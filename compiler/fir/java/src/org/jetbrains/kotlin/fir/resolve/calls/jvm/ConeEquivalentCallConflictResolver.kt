/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.jvm

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.overloads.ConeCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker

/**
 * Resolver that filters out equivalent calls, mainly to deduplicate multiples of the same declaration coming from different versions
 * of the same dependency, e.g., multiple stdlibs.
 *
 * Currently, it will also consider a declaration from source and one from binary equivalent if all conditions are met for backward
 * compatibility with K1.
 */
class ConeEquivalentCallConflictResolver(
    private val inferenceComponents: InferenceComponents,
) : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateAbstracts: Boolean
    ): Set<Candidate> {
        return filterOutEquivalentCalls(candidates)
    }

    private fun filterOutEquivalentCalls(candidates: Collection<Candidate>): Set<Candidate> {
        // Since we can consider a declaration from source and one from binary equivalent, we need to make sure we favor the one from
        // source, otherwise we might get a behavior change to K1.
        // See org.jetbrains.kotlin.resolve.calls.results.OverloadingConflictResolver.filterOutEquivalentCalls.
        val fromSourceFirst = candidates.sortedBy { it.symbol.fir.moduleData.session.kind != FirSession.Kind.Source }

        val result = mutableSetOf<Candidate>()
        outerLoop@ for (myCandidate in fromSourceFirst) {
            val me = myCandidate.symbol.fir
            if (me is FirCallableDeclaration && me.symbol.containingClassLookupTag() == null) {
                val resultIterator = result.iterator()
                while (resultIterator.hasNext()) {
                    val otherCandidate = resultIterator.next()
                    val other = otherCandidate.symbol.fir
                    if (other is FirCallableDeclaration && other.symbol.containingClassLookupTag() == null) {
                        if (areEquivalentTopLevelCallables(me, myCandidate, other, otherCandidate)) {
                            /**
                             * If we have an expect function in the result set and encounter a non-expect function among non-processed
                             * candidates, then we need to prefer this new function to the original expect one
                             */
                            if (other.isExpect && !me.isExpect) {
                                resultIterator.remove()
                            } else {
                                continue@outerLoop
                            }
                        }
                    }
                }
            }
            result += myCandidate
        }
        return result
    }

    private fun areEquivalentTopLevelCallables(
        first: FirCallableDeclaration,
        firstCandidate: Candidate,
        second: FirCallableDeclaration,
        secondCandidate: Candidate
    ): Boolean {
        if (first.symbol.callableId != second.symbol.callableId) return false
        // Emulate behavior from K1 where declarations from the same source module are never equivalent.
        // We expect REDECLARATION or CONFLICTING_OVERLOADS to be reported in those cases.
        // See a.containingDeclaration == b.containingDeclaration check in
        // org.jetbrains.kotlin.resolve.DescriptorEquivalenceForOverrides.areCallableDescriptorsEquivalent.
        // We can't rely on the fact that library declarations will have different moduleData, e.g. in Native metadata compilation,
        // multiple stdlib declarations with the same moduleData can be present, see KT-61461.
        if (first.moduleData == second.moduleData && first.moduleData.session.kind == FirSession.Kind.Source) return false
        if (first is FirVariable != second is FirVariable) {
            return false
        }
        if (!firstCandidate.mappedArgumentsOrderRepresentation.contentEquals(secondCandidate.mappedArgumentsOrderRepresentation)) {
            return false
        }

        val overrideChecker = FirStandardOverrideChecker(inferenceComponents.session)
        return if (first is FirProperty && second is FirProperty) {
            overrideChecker.isOverriddenProperty(first, second, ignoreVisibility = true) &&
                    overrideChecker.isOverriddenProperty(second, first, ignoreVisibility = true)
        } else if (first is FirSimpleFunction && second is FirSimpleFunction) {
            overrideChecker.isOverriddenFunction(first, second, ignoreVisibility = true) &&
                    overrideChecker.isOverriddenFunction(second, first, ignoreVisibility = true)
        } else {
            false
        }
    }

    /**
     * If the candidate is a function, then the arguments
     * order representation is an array containing the
     * parameters count and the indices of the parameters
     * that the call arguments correspond to in the order
     * the call arguments happen to be.
     *
     * Otherwise, null.
     */
    private val Candidate.mappedArgumentsOrderRepresentation: IntArray?
        get() {
            val function = symbol.fir as? FirFunction ?: return null
            val parametersToIndices = function.valueParameters.mapIndexed { index, it -> it to index }.toMap()
            if (!argumentMappingInitialized) return null
            val mapping = argumentMapping
            val result = IntArray(mapping.size + 1) { function.valueParameters.size }
            for ((index, parameter) in mapping.values.withIndex()) {
                result[index + 1] = parametersToIndices[parameter] ?: error("Unmapped argument in arguments mapping")
            }
            return result
        }
}
