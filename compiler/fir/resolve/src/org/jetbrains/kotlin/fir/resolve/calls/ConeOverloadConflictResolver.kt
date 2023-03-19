/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.results.FlatSignature
import org.jetbrains.kotlin.resolve.calls.results.SimpleConstraintSystem
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext

typealias CandidateSignature = FlatSignature<Candidate>

class ConeOverloadConflictResolver(
    specificityComparator: TypeSpecificityComparator,
    inferenceComponents: InferenceComponents,
    transformerComponents: BodyResolveComponents
) : AbstractConeCallConflictResolver(specificityComparator, inferenceComponents, transformerComponents) {

    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateAbstracts: Boolean
    ): Set<Candidate> = chooseMaximallySpecificCandidates(candidates, discriminateAbstracts, discriminateGenerics = true)

    private fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateAbstracts: Boolean,
        // Set to 'false' only for property-for-invoke case
        discriminateGenerics: Boolean,
    ): Set<Candidate> {
        if (candidates.size == 1) return candidates
        val fixedCandidates =
            if (candidates.first().callInfo.candidateForCommonInvokeReceiver != null)
                chooseCandidatesWithMostSpecificInvokeReceiver(candidates)
            else
                candidates

        return chooseMaximallySpecificCandidates(
            fixedCandidates,
            discriminateGenerics,
            discriminateAbstracts,
            discriminateSAMs = true,
            discriminateSuspendConversions = true,
            discriminateByUnwrappedSmartCastOrigin = true,
        )
    }

    private fun chooseCandidatesWithMostSpecificInvokeReceiver(candidates: Set<Candidate>): Set<Candidate> {
        val propertyReceiverCandidates = candidates.mapTo(mutableSetOf()) {
            it.callInfo.candidateForCommonInvokeReceiver
                ?: error("If one candidate within a group is property+invoke, other should be the same, but $it found")
        }

        val bestInvokeReceiver =
            chooseMaximallySpecificCandidates(propertyReceiverCandidates, discriminateGenerics = false, discriminateAbstracts = false)
                .singleOrNull() ?: return candidates

        return candidates.filterTo(mutableSetOf()) { it.callInfo.candidateForCommonInvokeReceiver == bestInvokeReceiver }
    }

    private fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        discriminateAbstracts: Boolean,
        // Only set to 'false' by recursive calls when the relevant discrimination kind has been already applied
        discriminateSAMs: Boolean,
        discriminateSuspendConversions: Boolean,
        discriminateByUnwrappedSmartCastOrigin: Boolean,
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
                    filtered, discriminateGenerics,
                    discriminateAbstracts,
                    discriminateSAMs = false,
                    discriminateSuspendConversions,
                    discriminateByUnwrappedSmartCastOrigin,
                )
            }
        }

        if (discriminateSuspendConversions) {
            val filtered = candidates.filterTo(mutableSetOf()) { !it.usesFunctionConversion }
            when (filtered.size) {
                1 -> return filtered
                0, candidates.size -> {
                }
                else -> return chooseMaximallySpecificCandidates(
                    filtered,
                    discriminateGenerics,
                    discriminateAbstracts,
                    discriminateSAMs,
                    discriminateSuspendConversions = false,
                    discriminateByUnwrappedSmartCastOrigin,
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
                    filtered,
                    discriminateGenerics,
                    discriminateAbstracts = false,
                    discriminateSAMs,
                    discriminateSuspendConversions,
                    discriminateByUnwrappedSmartCastOrigin,
                )
            }
        }

        if (discriminateByUnwrappedSmartCastOrigin) {
            // In case of MemberScopeTowerLevel with smart cast dispatch receiver, we may create candidates both from smart cast type and
            // from the member scope of original expression's type (without smart cast).
            // It might be necessary because the ones from smart cast might be invisible (e.g., because they are protected in other class).
            // open class A {
            //      open protected fun foo(a: Derived) {}
            //      fun f(a: A, d: Derived) {
            //          when (a) {
            //              is B -> {
            //                  a.foo(d) // should be resolved to A::foo, not the public B::foo
            //              }
            //          }
            //      }
            // }
            //
            // class B : A() {
            //      override fun foo(a: Derived) {}
            //      public fun foo(a: Base) {}
            // }
            // If we would just resolve a.foo(d) if a had a type B, then we would choose a public B::foo, because the other
            // one foo is protected in B, so we can't call it outside the B subclasses.
            // But that resolution result would be less precise result that the one before smart-cast applied (A::foo has more specific parameters),
            // so at MemberScopeTowerLevel we create candidates both from A's and B's scopes on the same level.
            // But in case when there would be successful candidates from both types, we discriminate ones from original type,
            // thus sticking to the candidates from smart cast type.
            // See more details at KT-51460, KT-55722, KT-56310 and relevant tests
            //    testData/diagnostics/tests/visibility/moreSpecificProtectedSimple.kt
            //    testData/diagnostics/tests/smartCasts/kt51460.kt
            val filtered = candidates.filterTo(mutableSetOf()) { !it.isFromOriginalTypeInPresenceOfSmartCast }
            when (filtered.size) {
                1 -> return filtered
                0, candidates.size -> {
                }
                else -> return chooseMaximallySpecificCandidates(
                    filtered,
                    discriminateGenerics,
                    discriminateAbstracts,
                    discriminateSAMs,
                    discriminateSuspendConversions,
                    discriminateByUnwrappedSmartCastOrigin = false,
                )
            }
        }

        val filtered = candidates.filterTo(mutableSetOf()) { it.usesSAM }
        if (filtered.isNotEmpty()) {
            findMaximallySpecificCall(candidates, discriminateGenerics = false, useOriginalSamTypes = true)?.let { return setOf(it) }
        }

        return candidates
    }

    private fun findMaximallySpecificCall(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean = false
    ): Candidate? {
        if (candidates.size <= 1) return candidates.singleOrNull()

        val candidateSignatures = candidates.map { candidateCall ->
            createFlatSignature(candidateCall)
        }

        val bestCandidatesByParameterTypes = candidateSignatures.filter { signature ->
            candidateSignatures.all { other ->
                signature === other || isNotLessSpecificCallWithArgumentMapping(signature, other, discriminateGenerics, useOriginalSamTypes)
            }
        }

        return bestCandidatesByParameterTypes.exactMaxWith()?.origin
    }

    /**
     * `call1` is not less specific than `call2`
     */
    private fun isNotLessSpecificCallWithArgumentMapping(
        call1: CandidateSignature,
        call2: CandidateSignature,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean = false
    ): Boolean {
        return compareCallsByUsedArguments(call1, call2, discriminateGenerics, useOriginalSamTypes)
    }

    private fun List<CandidateSignature>.exactMaxWith(): CandidateSignature? {
        var result: CandidateSignature? = null
        for (candidate in this) {
            if (result == null || isOfNotLessSpecificShape(candidate, result)) {
                result = candidate
            }
        }
        if (result == null) return null
        if (any { it != result && isOfNotLessSpecificShape(it, result) }) {
            return null
        }
        return result
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

class ConeSimpleConstraintSystemImpl(val system: NewConstraintSystemImpl, val session: FirSession) : SimpleConstraintSystem {
    override fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): TypeSubstitutorMarker = with(context) {
        val csBuilder = system.getBuilder()
        val substitutionMap = typeParameters.associateBy({ (it as ConeTypeParameterLookupTag).typeParameterSymbol }) {
            require(it is ConeTypeParameterLookupTag)
            val variable = ConeTypeParameterBasedTypeVariable(it.typeParameterSymbol)
            csBuilder.registerVariable(variable)

            variable.defaultType
        }
        val substitutor = substitutorByMap(substitutionMap, session)
        for (typeParameter in typeParameters) {
            require(typeParameter is ConeTypeParameterLookupTag)
            for (upperBound in typeParameter.symbol.resolvedBounds) {
                addSubtypeConstraint(
                    substitutionMap[typeParameter.typeParameterSymbol]
                        ?: error("No ${typeParameter.symbol.fir.render()} in substitution map"),
                    substitutor.substituteOrSelf(upperBound.coneType)
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
