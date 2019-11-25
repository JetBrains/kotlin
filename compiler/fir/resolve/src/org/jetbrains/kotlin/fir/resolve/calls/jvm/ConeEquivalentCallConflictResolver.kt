/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.jvm

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.AbstractConeCallConflictResolver
import org.jetbrains.kotlin.fir.resolve.calls.InferenceComponents
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.resolve.calls.results.FlatSignature
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

// This conflict resolver filters JVM equivalent top-level functions
// like emptyArray() from intrinsics and built-ins
class ConeEquivalentCallConflictResolver(
    specificityComparator: TypeSpecificityComparator,
    inferenceComponents: InferenceComponents
) : AbstractConeCallConflictResolver(specificityComparator, inferenceComponents) {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean
    ): Set<Candidate> {
        return filterOutEquivalentCalls(candidates)
    }

    private fun filterOutEquivalentCalls(candidates: Collection<Candidate>): Set<Candidate> {
        val result = mutableSetOf<Candidate>()
        outerLoop@ for (myCandidate in candidates) {
            val me = myCandidate.symbol.fir
            if (me is FirCallableMemberDeclaration<*> && me.symbol.callableId.className == null) {
                for (otherCandidate in result) {
                    val other = otherCandidate.symbol.fir
                    if (other is FirCallableMemberDeclaration<*> && other.symbol.callableId.className == null) {
                        if (areEquivalentTopLevelCallables(me, myCandidate, other, otherCandidate)) {
                            continue@outerLoop
                        }
                    }
                }
            }
            result += myCandidate
        }
        return result
    }

    private fun areEquivalentTopLevelCallables(
        first: FirCallableMemberDeclaration<*>,
        firstCandidate: Candidate,
        second: FirCallableMemberDeclaration<*>,
        secondCandidate: Candidate
    ): Boolean {
        if (first.symbol.callableId != second.symbol.callableId) return false
        if (first.isExpect != second.isExpect) return false
        if (first.receiverTypeRef?.coneTypeUnsafe<ConeKotlinType>() != second.receiverTypeRef?.coneTypeUnsafe()) {
            return false
        }
        val firstSignature = createFlatSignature(firstCandidate, first)
        val secondSignature = createFlatSignature(secondCandidate, second)
        return compareCallsByUsedArguments(firstSignature, secondSignature, false) &&
                compareCallsByUsedArguments(secondSignature, firstSignature, false)
    }

    private fun createFlatSignature(call: Candidate, declaration: FirCallableMemberDeclaration<*>): FlatSignature<Candidate> {
        return when (declaration) {
            is FirSimpleFunction -> createFlatSignature(call, declaration)
            is FirConstructor -> createFlatSignature(call, declaration)
            is FirVariable<*> -> createFlatSignature(call, declaration as FirVariable<*>)
            else -> error("Not supported: $declaration")
        }
    }
}