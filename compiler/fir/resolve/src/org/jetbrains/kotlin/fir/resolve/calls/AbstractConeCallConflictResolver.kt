/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.results.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.requireOrDescribe
import org.jetbrains.kotlin.utils.addIfNotNull

abstract class AbstractConeCallConflictResolver(
    private val specificityComparator: TypeSpecificityComparator,
    protected val inferenceComponents: InferenceComponents
) : ConeCallConflictResolver() {
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

        if (call1.contextReceiverCount > call2.contextReceiverCount) return true
        if (call1.contextReceiverCount < call2.contextReceiverCount) return false

        return createEmptyConstraintSystem().isSignatureNotLessSpecific(
            call1,
            call2,
            SpecificityComparisonWithNumerics,
            specificityComparator
        )
    }

    @Suppress("PrivatePropertyName")
    private val SpecificityComparisonWithNumerics = object : SpecificityComparisonCallbacks {
        override fun isNonSubtypeNotLessSpecific(specific: KotlinTypeMarker, general: KotlinTypeMarker): Boolean {
            requireOrDescribe(specific is ConeKotlinType, specific)
            requireOrDescribe(general is ConeKotlinType, general)

            // TODO: support unsigned types
            // see OverloadingConflictResolver.kt:294

            val int = StandardClassIds.Int
            val long = StandardClassIds.Long
            val byte = StandardClassIds.Byte
            val short = StandardClassIds.Short

            val uInt = StandardClassIds.UInt
            val uLong = StandardClassIds.ULong
            val uByte = StandardClassIds.UByte
            val uShort = StandardClassIds.UShort

            val specificClassId = specific.lowerBoundIfFlexible().classId ?: return false
            val generalClassId = general.upperBoundIfFlexible().classId ?: return false


            // int >= long, int >= short, short >= byte

            when {
                //TypeUtils.equalTypes(specific, _double) && TypeUtils.equalTypes(general, _float) -> return true
                specificClassId == int -> {
                    when (generalClassId) {
                        long -> return true
                        byte -> return true
                        short -> return true
                    }
                }
                specificClassId == short && generalClassId == byte -> return true
                specificClassId == uInt -> {
                    when (generalClassId) {
                        uLong -> return true
                        uByte -> return true
                        uShort -> return true
                    }
                }
                specificClassId == uShort && generalClassId == uByte -> return true
            }
            return false
        }
    }

    protected fun createFlatSignature(call: Candidate): FlatSignature<Candidate> {
        return when (val declaration = call.symbol.fir) {
            is FirSimpleFunction -> createFlatSignature(call, declaration)
            is FirConstructor -> createFlatSignature(call, declaration)
            is FirVariable -> createFlatSignature(call, declaration)
            is FirClass -> createFlatSignature(call, declaration)
            is FirTypeAlias -> createFlatSignature(call, declaration)
            else -> error("Not supported: $declaration")
        }
    }

    protected fun createFlatSignature(call: Candidate, variable: FirVariable): FlatSignature<Candidate> {
        return FlatSignature(
            call,
            (variable as? FirProperty)?.typeParameters?.map { it.symbol.toLookupTag() }.orEmpty(),
            computeSignatureTypes(call, variable),
            variable.receiverTypeRef != null,
            variable.contextReceivers.size,
            false,
            0,
            (variable as? FirProperty)?.isExpect == true,
            false // TODO
        )
    }

    protected fun createFlatSignature(call: Candidate, constructor: FirConstructor): FlatSignature<Candidate> {
        return FlatSignature(
            call,
            constructor.typeParameters.map { it.symbol.toLookupTag() },
            computeSignatureTypes(call, constructor),
            //constructor.receiverTypeRef != null,
            false,
            constructor.contextReceivers.size,
            constructor.valueParameters.any { it.isVararg },
            call.numDefaults,
            constructor.isExpect,
            false // TODO
        )
    }

    protected fun createFlatSignature(call: Candidate, function: FirSimpleFunction): FlatSignature<Candidate> {
        return FlatSignature(
            call,
            function.typeParameters.map { it.symbol.toLookupTag() },
            computeSignatureTypes(call, function),
            function.receiverTypeRef != null,
            function.contextReceivers.size,
            function.valueParameters.any { it.isVararg },
            call.numDefaults,
            function.isExpect,
            false // TODO
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
    ): List<ConeKotlinType> {
        return buildList {
            addIfNotNull(called.receiverTypeRef?.coneType)
            val typeForCallableReference = call.resultingTypeForCallableReference
            if (typeForCallableReference != null) {
                // Return type isn't needed here       v
                typeForCallableReference.typeArguments.dropLast(1)
                    .mapTo(this) {
                        (it as ConeKotlinType).removeTypeVariableTypes(inferenceComponents.session.typeContext)
                    }
            } else {
                called.contextReceivers.mapTo(this) { it.typeRef.coneType }
                call.argumentMapping?.mapTo(this) { it.value.argumentType() }
            }
        }
    }

    private fun createFlatSignature(call: Candidate, klass: FirClassLikeDeclaration): FlatSignature<Candidate> {
        return FlatSignature(
            call,
            (klass as? FirTypeParameterRefsOwner)?.typeParameters?.map { it.symbol.toLookupTag() }.orEmpty(),
            emptyList(),
            hasExtensionReceiver = false,
            0, // TODO
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
