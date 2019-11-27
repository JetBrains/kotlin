/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.resolve.OverloadabilitySpecificityCallbacks
import org.jetbrains.kotlin.resolve.calls.results.FlatSignature
import org.jetbrains.kotlin.resolve.calls.results.SimpleConstraintSystem
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.calls.results.isSignatureNotLessSpecific

abstract class AbstractConeCallConflictResolver(
    private val specificityComparator: TypeSpecificityComparator,
    protected val inferenceComponents: InferenceComponents
) : ConeCallConflictResolver {
    protected fun Collection<Candidate>.setIfOneOrEmpty(): Set<Candidate>? = when (size) {
        0 -> emptySet()
        1 -> setOf(single())
        else -> null
    }

    /**
     * Returns `true` if [call1] is definitely more or equally specific [call2],
     * `false` otherwise.
     */
    protected fun compareCallsByUsedArguments(
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

    protected fun createFlatSignature(call: Candidate): FlatSignature<Candidate> {
        return when (val declaration = call.symbol.fir) {
            is FirSimpleFunction -> createFlatSignature(call, declaration)
            is FirConstructor -> createFlatSignature(call, declaration)
            is FirVariable<*> -> createFlatSignature(call, declaration)
            is FirClass<*> -> createFlatSignature(call, declaration)
            else -> error("Not supported: $declaration")
        }
    }

    protected fun createFlatSignature(call: Candidate, variable: FirVariable<*>): FlatSignature<Candidate> {
        return FlatSignature(
            call,
            (variable as? FirProperty)?.typeParameters?.map { it.symbol }.orEmpty(),
            listOfNotNull<ConeKotlinType>(variable.receiverTypeRef?.coneTypeUnsafe()),
            variable.receiverTypeRef != null,
            false,
            0,
            (variable as? FirProperty)?.isExpect == true,
            false // TODO
        )
    }

    protected fun createFlatSignature(call: Candidate, constructor: FirConstructor): FlatSignature<Candidate> {
        return FlatSignature(
            call,
            constructor.typeParameters.map { it.symbol },
            computeParameterTypes(call, constructor),
            //constructor.receiverTypeRef != null,
            false,
            constructor.valueParameters.any { it.isVararg },
            constructor.valueParameters.count { it.defaultValue != null },
            constructor.isExpect,
            false // TODO
        )
    }

    protected fun createFlatSignature(call: Candidate, function: FirSimpleFunction): FlatSignature<Candidate> {
        return FlatSignature(
            call,
            function.typeParameters.map { it.symbol },
            computeParameterTypes(call, function),
            function.receiverTypeRef != null,
            function.valueParameters.any { it.isVararg },
            function.valueParameters.count { it.defaultValue != null },
            function.isExpect,
            false // TODO
        )
    }

    private fun FirValueParameter.argumentType(): ConeKotlinType {
        val type = returnTypeRef.coneTypeUnsafe<ConeKotlinType>()
        if (isVararg) return type.arrayElementType(inferenceComponents.session)!!
        return type
    }

    private fun computeParameterTypes(
        call: Candidate,
        function: FirFunction<*>
    ): List<ConeKotlinType> {
        return (call.resultingTypeForCallableReference?.typeArguments?.map { it as ConeKotlinType }
            ?: (listOfNotNull(function.receiverTypeRef?.coneTypeUnsafe()) +
                    call.argumentMapping?.map { it.value.argumentType() }.orEmpty()))
    }

    private fun createFlatSignature(call: Candidate, klass: FirClass<*>): FlatSignature<Candidate> {
        return FlatSignature(
            call,
            (klass as? FirRegularClass)?.typeParameters?.map { it.symbol }.orEmpty(),
            valueParameterTypes = emptyList(),
            hasExtensionReceiver = false,
            hasVarargs = false,
            numDefaults = 0,
            isExpect = (klass as? FirRegularClass)?.isExpect == true,
            isSyntheticMember = false
        )
    }

    private fun createEmptyConstraintSystem(): SimpleConstraintSystem {
        return ConeSimpleConstraintSystemImpl(inferenceComponents.createConstraintSystem())
    }
}