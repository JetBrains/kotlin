/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.overloads

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.unwrapArgument
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.ConeResolvedCallableReferenceAtom
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.removeTypeVariableTypes
import org.jetbrains.kotlin.fir.resolve.calls.stages.shouldHaveLowPriorityDueToSAM
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.overrides
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.Byte
import org.jetbrains.kotlin.name.StandardClassIds.Double
import org.jetbrains.kotlin.name.StandardClassIds.Float
import org.jetbrains.kotlin.name.StandardClassIds.Int
import org.jetbrains.kotlin.name.StandardClassIds.Long
import org.jetbrains.kotlin.name.StandardClassIds.Short
import org.jetbrains.kotlin.name.StandardClassIds.UByte
import org.jetbrains.kotlin.name.StandardClassIds.UInt
import org.jetbrains.kotlin.name.StandardClassIds.ULong
import org.jetbrains.kotlin.name.StandardClassIds.UShort
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.results.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

typealias CandidateSignature = FlatSignature<Candidate>

class ConeOverloadConflictResolver(
    private val specificityComparator: TypeSpecificityComparator,
    private val inferenceComponents: InferenceComponents,
    private val transformerComponents: BodyResolveComponents,
) : ConeCallConflictResolver() {

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
                { !it.usesSamConversionOrSamConstructor },
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

        val filtered = candidates.filterTo(mutableSetOf()) { it.usesSamConversionOrSamConstructor }
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
            it is ConeResolvedCallableReferenceAtom &&
                    (it.resultingReference as? FirNamedReferenceWithCandidate)?.candidate?.callableReferenceAdaptation != null
        }
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

    /**
     * call1.expect
     */
    private fun checkExpectAndNotLessSpecificShape(
        call1: FlatSignature<Candidate>,
        call2: FlatSignature<Candidate>
    ): Boolean {
        // !false && false
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

    /**
     * Returns `true` if [call1] is definitely more or equally specific [call2],
     * `false` otherwise.
     */
    private fun compareCallsByUsedArguments(
        call1: FlatSignature<Candidate>,
        call2: FlatSignature<Candidate>,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean
    ): Boolean {
        if (discriminateGenerics) {
            val isGeneric1 = call1.isGeneric
            val isGeneric2 = call2.isGeneric

            when {
                // non-generic wins over generic
                !isGeneric1 && isGeneric2 -> return true
                // generic loses to non-generic and incomparable with another generic,
                // thus doesn't matter what is `isGeneric2`
                isGeneric1 -> return false
                // !isGeneric1 && !isGeneric2 -> continue as usual
                else -> {}
            }
        }

        if (call1.contextReceiverCount > call2.contextReceiverCount) return true
        if (call1.contextReceiverCount < call2.contextReceiverCount) return false

        return createEmptyConstraintSystem().isSignatureNotLessSpecific(
            call1,
            call2,
            SpecificityComparisonWithNumerics,
            specificityComparator,
            useOriginalSamTypes
        )
    }

    @Suppress("PrivatePropertyName")
    private val SpecificityComparisonWithNumerics = object : SpecificityComparisonCallbacks {
        override fun isNonSubtypeNotLessSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker): Boolean {
            requireOrDescribe(specific is ConeKotlinType, specific)
            requireOrDescribe(general is ConeKotlinType, general)

            val specificClassId = specific.lowerBoundIfFlexible().classId ?: return false
            val generalClassId = general.upperBoundIfFlexible().classId ?: return false

            // any signed >= any unsigned

            if (specificClassId.isSignedIntegerType && generalClassId.isUnsigned) {
                return true
            }

            // int >= long, int >= short, short >= byte

            if (specificClassId == Int) {
                return generalClassId == Long || generalClassId == Short || generalClassId == Byte
            } else if (specificClassId == Short && generalClassId == Byte) {
                return true
            }

            // uint >= ulong, uint >= ushort, ushort >= ubyte

            if (specificClassId == UInt) {
                return generalClassId == ULong || generalClassId == UShort || generalClassId == UByte
            } else if (specificClassId == UShort && generalClassId == UByte) {
                return true
            }

            // double >= float

            return specificClassId == Double && generalClassId == Float
        }

        private val ClassId.isUnsigned: Boolean get() = this in StandardClassIds.unsignedTypes

        private val useCorrectSignedCheck: Boolean =
            inferenceComponents.session.languageVersionSettings.supportsFeature(LanguageFeature.CorrectSpecificityCheckForSignedAndUnsigned)

        private val ClassId.isSignedIntegerType: Boolean
            get() = if (useCorrectSignedCheck) {
                this in StandardClassIds.signedIntegerTypes
            } else {
                !isUnsigned
            }
    }

    private fun createFlatSignature(call: Candidate): FlatSignature<Candidate> {
        return when (val declaration = call.symbol.fir) {
            is FirSimpleFunction -> createFlatSignature(call, declaration)
            is FirConstructor -> createFlatSignature(call, declaration)
            is FirVariable -> createFlatSignature(call, declaration)
            is FirClass -> createFlatSignature(call, declaration)
            is FirTypeAlias -> createFlatSignature(call, declaration)
            else -> errorWithAttachment("Not supported: ${declaration::class.java}") {
                withFirEntry("declaration", declaration)
            }
        }
    }

    private fun createFlatSignature(call: Candidate, variable: FirVariable): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = (variable as? FirProperty)?.typeParameters?.map { it.symbol.toLookupTag() }.orEmpty(),
            valueParameterTypes = computeSignatureTypes(call, variable),
            hasExtensionReceiver = variable.receiverParameter != null,
            contextReceiverCount = variable.contextReceivers.size,
            hasVarargs = false,
            numDefaults = 0,
            isExpect = (variable as? FirProperty)?.isExpect == true,
            isSyntheticMember = false
        )
    }

    private fun createFlatSignature(call: Candidate, constructor: FirConstructor): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = constructor.typeParameters.map { it.symbol.toLookupTag() },
            valueParameterTypes = computeSignatureTypes(call, constructor),
            //constructor.receiverParameter != null,
            hasExtensionReceiver = false,
            contextReceiverCount = constructor.contextReceivers.size,
            hasVarargs = constructor.valueParameters.any { it.isVararg },
            numDefaults = call.numDefaults,
            isExpect = constructor.isExpect,
            isSyntheticMember = false
        )
    }

    private fun createFlatSignature(call: Candidate, function: FirSimpleFunction): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = function.typeParameters.map { it.symbol.toLookupTag() },
            valueParameterTypes = computeSignatureTypes(call, function),
            hasExtensionReceiver = function.receiverParameter != null,
            contextReceiverCount = function.contextReceivers.size,
            hasVarargs = function.valueParameters.any { it.isVararg },
            numDefaults = call.numDefaults,
            isExpect = function.isExpect,
            isSyntheticMember = false
        )
    }

    private fun FirValueParameter.argumentType(): ConeKotlinType {
        val type = returnTypeRef.coneType
        if (isVararg) return type.arrayElementType()!!
        return type
    }

    private fun computeSignatureTypes(
        call: Candidate,
        called: FirCallableDeclaration
    ): List<TypeWithConversion> {
        return buildList {
            val session = inferenceComponents.session
            addIfNotNull(called.receiverParameter?.typeRef?.coneType?.prepareType(session, call)?.let { TypeWithConversion(it) })
            val typeForCallableReference = call.resultingTypeForCallableReference
            if (typeForCallableReference != null) {
                // Return type isn't needed here       v
                typeForCallableReference.typeArguments.dropLast(1)
                    .mapTo(this) {
                        TypeWithConversion((it as ConeKotlinType).prepareType(session, call).removeTypeVariableTypes(session.typeContext))
                    }
            } else {
                called.contextReceivers.mapTo(this) { TypeWithConversion(it.typeRef.coneType.prepareType(session, call)) }
                if (call.argumentMappingInitialized) {
                    call.argumentMapping.mapTo(this) { (_, parameter) ->
                        parameter.toTypeWithConversion(session, call)
                    }
                }
            }
        }
    }

    private fun FirValueParameter.toTypeWithConversion(session: FirSession, call: Candidate): TypeWithConversion {
        val argumentType = argumentType().prepareType(session, call)
        val functionTypeForSam = toFunctionTypeForSamOrNull(call)
        return if (functionTypeForSam == null) {
            TypeWithConversion(argumentType)
        } else {
            TypeWithConversion(functionTypeForSam, argumentType)
        }
    }

    private fun ConeKotlinType.prepareType(session: FirSession, candidate: Candidate): ConeKotlinType {
        val expanded = fullyExpandedType(session)
        if (!candidate.system.usesOuterCs) return expanded
        // For resolving overloads in PCLA of the following form:
        //  fun foo(vararg values: Tv)
        //  fun foo(x: A<Tv>)
        // In K1, all Tv variables have been replaced with relevant stub types
        // Thus, both of the overloads were considered as not less specific than other (stubTypesAreEqualToAnything=true)
        // And after that the one with A<Tv> is chosen because it was discriminated via [ConeOverloadConflictResolver.exactMaxWith]
        // as not containing varargs.
        // Thus we reproduce K1 behavior with stub types (even though we don't like then much, but it's very local)
        //
        // But this behavior looks quite hacky because it seems that the second overload should win even without varargs
        // on the first one.
        // TODO: Get rid of hacky K1 behavior (KT-67947)
        return candidate.system.buildNotFixedVariablesToStubTypesSubstitutor()
            .safeSubstitute(session.typeContext, expanded) as ConeKotlinType
    }

    private fun FirValueParameter.toFunctionTypeForSamOrNull(call: Candidate): ConeKotlinType? {
        val functionTypesOfSamConversions = call.functionTypesOfSamConversions ?: return null
        return call.argumentMapping.entries.firstNotNullOfOrNull {
            runIf(it.value == this) { functionTypesOfSamConversions[it.key.unwrapArgument()]?.functionalType }
        }
    }

    private fun createFlatSignature(call: Candidate, klass: FirClassLikeDeclaration): FlatSignature<Candidate> {
        return FlatSignature(
            call,
            (klass as? FirTypeParameterRefsOwner)?.typeParameters?.map { it.symbol.toLookupTag() }.orEmpty(),
            emptyList(),
            hasExtensionReceiver = false,
            0,
            hasVarargs = false,
            numDefaults = 0,
            isExpect = (klass as? FirRegularClass)?.isExpect == true,
            isSyntheticMember = false
        )
    }

    private fun createEmptyConstraintSystem(): SimpleConstraintSystem {
        return ConeSimpleConstraintSystemImpl(inferenceComponents.createConstraintSystem(), inferenceComponents.session)
    }
}

class ConeSimpleConstraintSystemImpl(val system: NewConstraintSystemImpl, val session: FirSession) : SimpleConstraintSystem {
    override fun registerTypeVariables(typeParameters: Collection<TypeParameterMarker>): TypeSubstitutorMarker {
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
