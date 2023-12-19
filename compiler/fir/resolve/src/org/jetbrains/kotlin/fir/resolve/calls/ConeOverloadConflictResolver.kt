/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.getSingleMatchedExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.inference.ResolvedCallableReferenceAtom
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.overrides
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.results.FlatSignature
import org.jetbrains.kotlin.resolve.calls.results.SimpleConstraintSystem
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

typealias CandidateSignature = FlatSignature<Candidate>

class ConeOverloadConflictResolver(
    specificityComparator: TypeSpecificityComparator,
    inferenceComponents: InferenceComponents,
    transformerComponents: BodyResolveComponents,
) : AbstractConeCallConflictResolver(
    specificityComparator,
    inferenceComponents,
    transformerComponents,
    considerMissingArgumentsInSignatures = false,
) {

    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateAbstracts: Boolean,
    ): Set<Candidate> = chooseMaximallySpecificCandidates(
        candidates,
        discriminateAbstracts,
        // We don't discriminate against generics for callable references because, other than in regular calls,
        // there is no syntax for specifying generic type arguments.
        discriminateGenerics = candidates.first().callInfo.callSite !is FirCallableReferenceAccess
    )

    /**
     * Partial mirror of [org.jetbrains.kotlin.resolve.calls.results.OverloadingConflictResolver.chooseMaximallySpecificCandidates]
     */
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

        // The same logic as at
        val candidatesWithoutOverrides = filterOverrides(fixedCandidates)
        val noCompatibilityMode = inferenceComponents.session.languageVersionSettings.supportsFeature(
            LanguageFeature.DisableCompatibilityModeForNewInference
        )
        return chooseMaximallySpecificCandidates(
            candidatesWithoutOverrides,
            DiscriminationFlags(
                // (in compatibility mode the next two are already filtered on tower resolver level)
                lowPrioritySAMs = noCompatibilityMode,
                adaptationsInPostponedAtoms = noCompatibilityMode,
                generics = discriminateGenerics,
                abstracts = discriminateAbstracts,
                SAMs = true,
                suspendConversions = true,
                byUnwrappedSmartCastOrigin = true,
            )
        )
    }

    /**
     * See K1 version at OverridingUtil.filterOverrides
     */
    private fun filterOverrides(
        candidateSet: Set<Candidate>,
    ): Set<Candidate> {
        if (candidateSet.size <= 1) return candidateSet

        val result = mutableSetOf<Candidate>()

        // Assuming `overrides` is a partial order, this loop leaves minimal elements of `candidateSet` in `result`.
        // Namely, it leaves in `result` only candidates, for any pair of them (x, y): !x.overrides(y) && !y.overrides(x)
        // And for any pair original candidates (x, y) if x.overrides(y) && !y.overrides(x) then `x` belongs `result`
        outerLoop@ for (me in candidateSet) {
            val iterator = result.iterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                if (me.overrides(other)) {
                    iterator.remove()
                } else if (other.overrides(me)) {
                    continue@outerLoop
                }
            }

            result.add(me)
        }

        require(result.isNotEmpty()) { "All candidates filtered out from $candidateSet" }
        return result
    }

    private fun Candidate.overrides(other: Candidate): Boolean {
        val symbol = symbol
        if (symbol !is FirCallableSymbol || other.symbol !is FirCallableSymbol) return false

        val otherOriginal = (other.symbol as FirCallableSymbol).unwrapSubstitutionOverrides()
        if (symbol.unwrapSubstitutionOverrides<FirCallableSymbol<*>>() == otherOriginal) return true

        val scope = originScope as? FirTypeScope ?: return false

        @Suppress("UNCHECKED_CAST")
        val overriddenProducer = when (symbol) {
            is FirNamedFunctionSymbol -> FirTypeScope::processOverriddenFunctions as ProcessAllOverridden<FirCallableSymbol<*>>
            is FirPropertySymbol -> FirTypeScope::processOverriddenProperties as ProcessAllOverridden<FirCallableSymbol<*>>
            else -> return false
        }

        return overrides(MemberWithBaseScope(symbol, scope), otherOriginal, overriddenProducer)
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

    private data class DiscriminationFlags(
        val lowPrioritySAMs: Boolean,
        val adaptationsInPostponedAtoms: Boolean,
        val generics: Boolean,
        val abstracts: Boolean,
        val SAMs: Boolean,
        val suspendConversions: Boolean,
        val byUnwrappedSmartCastOrigin: Boolean,
    )

    private fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminationFlags: DiscriminationFlags
    ): Set<Candidate> {
        if (discriminationFlags.lowPrioritySAMs) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.shouldHaveLowPriorityDueToSAM(transformerComponents) },
                { discriminationFlags.copy(lowPrioritySAMs = false) },
            )?.let { return it }
        }

        if (discriminationFlags.adaptationsInPostponedAtoms) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.hasPostponedAtomWithAdaptation() },
                { discriminationFlags.copy(adaptationsInPostponedAtoms = false) },
            )?.let { return it }
        }

        findMaximallySpecificCall(candidates, false)?.let { return setOf(it) }

        if (discriminationFlags.generics) {
            findMaximallySpecificCall(candidates, true)?.let { return setOf(it) }
        }

        if (discriminationFlags.SAMs) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.usesSAM },
                { discriminationFlags.copy(SAMs = false) },
            )?.let { return it }
        }

        if (discriminationFlags.suspendConversions) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.usesFunctionConversion },
                { discriminationFlags.copy(suspendConversions = false) },
            )?.let { return it }
        }

        if (discriminationFlags.abstracts) {
            filterCandidatesByDiscriminationFlag(
                candidates,
                { (it.symbol.fir as? FirMemberDeclaration)?.modality != Modality.ABSTRACT },
                { discriminationFlags.copy(abstracts = false) },
            )?.let { return it }
        }

        if (discriminationFlags.byUnwrappedSmartCastOrigin) {
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
            filterCandidatesByDiscriminationFlag(
                candidates,
                { !it.isFromOriginalTypeInPresenceOfSmartCast },
                { discriminationFlags.copy(byUnwrappedSmartCastOrigin = false) },
            )?.let { return it }
        }

        val filtered = candidates.filterTo(mutableSetOf()) { it.usesSAM }
        if (filtered.isNotEmpty()) {
            findMaximallySpecificCall(candidates, discriminateGenerics = false, useOriginalSamTypes = true)?.let { return setOf(it) }
        }

        return candidates
    }

    private inline fun filterCandidatesByDiscriminationFlag(
        candidates: Set<Candidate>,
        filter: (Candidate) -> Boolean,
        newFlags: () -> DiscriminationFlags,
    ): Set<Candidate>? {
        val filtered = candidates.filterTo(mutableSetOf()) { filter(it) }
        return when (filtered.size) {
            1 -> filtered
            0, candidates.size -> null
            else -> chooseMaximallySpecificCandidates(filtered, newFlags())
        }
    }

    private fun Candidate.hasPostponedAtomWithAdaptation(): Boolean {
        return postponedAtoms.any {
            it is ResolvedCallableReferenceAtom &&
                    (it.resultingReference as? FirNamedReferenceWithCandidate)?.candidate?.callableReferenceAdaptation != null
        }
    }

    private fun findMaximallySpecificCall(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean = false
    ): Candidate? {
        if (candidates.size <= 1) return candidates.singleOrNull()

        val candidatesWithoutActualizedExpects = filterOutActualizedExpectCandidates(candidates)

        val candidateSignatures = candidatesWithoutActualizedExpects.map { candidateCall ->
            createFlatSignature(candidateCall)
        }

        val bestCandidatesByParameterTypes = candidateSignatures.filter { signature ->
            candidateSignatures.all { other ->
                signature === other || isNotLessSpecificCallWithArgumentMapping(signature, other, discriminateGenerics, useOriginalSamTypes)
            }
        }

        return bestCandidatesByParameterTypes.exactMaxWith()?.origin
    }

    private fun filterOutActualizedExpectCandidates(candidates: Set<Candidate>): Set<Candidate> {
        val expectForActualSymbols = candidates
            .mapNotNullTo(mutableSetOf()) {
                val callableSymbol = it.symbol as? FirCallableSymbol<*> ?: return@mapNotNullTo null
                runIf(callableSymbol.isActual) { callableSymbol.getSingleMatchedExpectForActualOrNull() }
            }

        return if (expectForActualSymbols.isEmpty()) {
            candidates // Optimization: in most cases, there are no expectForActualSymbols that's why filtering and allocation are not performed
        } else {
            candidates.filterTo(mutableSetOf()) { candidate ->
                val symbol = candidate.symbol
                symbol is FirCallableSymbol<*> && (!symbol.isExpect || symbol !in expectForActualSymbols)
            }
        }
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
            if (result == null || checkExpectAndNotLessSpecificShape(candidate, result)) {
                result = candidate
            }
        }
        if (result == null) return null
        if (any { it != result && checkExpectAndNotLessSpecificShape(it, result) }) {
            return null
        }
        return result
    }

    private fun checkExpectAndNotLessSpecificShape(
        call1: FlatSignature<Candidate>,
        call2: FlatSignature<Candidate>
    ): Boolean {
        if (!call1.isExpect && call2.isExpect) return true
        if (call1.isExpect && !call2.isExpect) return false
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
                        ?: errorWithAttachment("No ${typeParameter.symbol.fir::class.java} in substitution map") {
                            withFirEntry("typeParameter", typeParameter.symbol.fir)
                        },
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
