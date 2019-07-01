/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.resolve.OverloadabilitySpecificityCallbacks
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.results.FlatSignature
import org.jetbrains.kotlin.resolve.calls.results.SimpleConstraintSystem
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.results.isSignatureNotLessSpecific
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContext
import org.jetbrains.kotlin.utils.addToStdlib.cast

class ConeOverloadConflictResolver(
    val specificityComparator: TypeSpecificityComparator,
    val inferenceComponents: InferenceComponents

) {

    fun chooseMaximallySpecificCandidates(
        candidates: Collection<Candidate>,
        //checkArgumentsMode: CheckArgumentTypesMode,
        discriminateGenerics: Boolean//,
        //isDebuggerContext: Boolean
    ): Set<Candidate> {

        val candidatesSet = candidates.toSet()

        val maximallySpecific = findMaximallySpecificCall(candidatesSet, false/*, isDebuggerContext*/)
        if (maximallySpecific != null) {
            return setOf(maximallySpecific)
        }

        return candidatesSet

    }

    private fun createFlatSignature(call: Candidate): FlatSignature<Candidate> {
        val declaration = call.symbol.firUnsafe<FirCallableDeclaration>()
        return when (declaration) {
            is FirNamedFunction -> createFlatSignature(call, declaration)
            is FirConstructor -> createFlatSignature(call, declaration)
            else -> error("Not supported: $declaration")
        }
    }

    private fun createFlatSignature(call: Candidate, constructor: FirConstructor): FlatSignature<Candidate> {
        return FlatSignature(
            call,
            constructor.typeParameters.map { it.symbol },
            call.argumentMapping!!.map { it.value.returnTypeRef.coneTypeUnsafe() },
            //constructor.receiverTypeRef != null,
            false,
            constructor.valueParameters.any { it.isVararg },
            constructor.valueParameters.count { it.defaultValue != null },
            constructor.isExpect,
            false // TODO
        )
    }

    private fun createFlatSignature(call: Candidate, function: FirNamedFunction): FlatSignature<Candidate> {
        return FlatSignature(
            call,
            function.typeParameters.map { it.symbol },
            listOfNotNull<ConeKotlinType>(function.receiverTypeRef?.coneTypeUnsafe()) +
                    call.argumentMapping!!.map { it.value.returnTypeRef.coneTypeUnsafe() },
            function.receiverTypeRef != null,
            function.valueParameters.any { it.isVararg },
            function.valueParameters.count { it.defaultValue != null },
            function.isExpect,
            false // TODO
        )
    }

    private fun createEmptyConstraintSystem(): SimpleConstraintSystem {
        return ConeSimpleConstraintSystemImpl(inferenceComponents.createConstraintSystem())
    }

    private fun findMaximallySpecificCall(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean//,
        //isDebuggerContext: Boolean
    ): Candidate? {
        val filteredCandidates = candidates//uniquifyCandidatesSet(candidates)

        if (filteredCandidates.size <= 1) return filteredCandidates.singleOrNull()

        val conflictingCandidates = filteredCandidates.map { candidateCall ->
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
        if (any { it != result && isNotWorse(it, result!!) }) {
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



    /**
     * Returns `true` if [call1] is definitely more or equally specific [call2],
     * `false` otherwise.
     */
    private fun compareCallsByUsedArguments(
        call1: FlatSignature<Candidate>,
        call2: FlatSignature<Candidate>,
        discriminateGenerics: Boolean
    ): Boolean {
        if (discriminateGenerics) {
            val isGeneric1 = call1.isGeneric
            val isGeneric2 = call2.isGeneric
            // generic loses to non-generic
            if (isGeneric1 && !isGeneric2) return false
            if (!isGeneric1 && isGeneric2) return true
            // two generics are non-comparable
            if (isGeneric1 && isGeneric2) return false
        }

        if (!call1.isExpect && call2.isExpect) return true
        if (call1.isExpect && !call2.isExpect) return false

        return createEmptyConstraintSystem().isSignatureNotLessSpecific(
            call1,
            call2,
            OverloadabilitySpecificityCallbacks,
            specificityComparator
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
        val substitutionMap = typeParameters.associate {
            require(it is FirTypeParameterSymbol)
            val variable = TypeParameterBasedTypeVariable(it)
            csBuilder.registerVariable(variable)


            it to variable.defaultType
        }
        val substitutor = ConeSubstitutorByMap(substitutionMap.cast())
        for (typeParameter in typeParameters) {
            require(typeParameter is FirTypeParameterSymbol)
            for (upperBound in typeParameter.fir.bounds) {
                addSubtypeConstraint(substitutionMap[typeParameter]!!, substitutor.substituteOrSelf(upperBound.coneTypeUnsafe()))
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