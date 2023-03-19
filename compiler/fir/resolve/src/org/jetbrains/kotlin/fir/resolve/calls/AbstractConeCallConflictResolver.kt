/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.fir.types.*
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
import org.jetbrains.kotlin.resolve.calls.results.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.requireOrDescribe
import org.jetbrains.kotlin.utils.addIfNotNull

abstract class AbstractConeCallConflictResolver(
    private val specificityComparator: TypeSpecificityComparator,
    protected val inferenceComponents: InferenceComponents,
    private val transformerComponents: BodyResolveComponents
) : ConeCallConflictResolver() {

    private val samResolver: FirSamResolver get() = transformerComponents.samResolver

    /**
     * Returns `true` if [call1] is definitely more or equally specific [call2],
     * `false` otherwise.
     */
    protected fun compareCallsByUsedArguments(
        call1: FlatSignature<Candidate>,
        call2: FlatSignature<Candidate>,
        discriminateGenerics: Boolean,
        useOriginalSamTypes: Boolean
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
            origin = call,
            typeParameters = (variable as? FirProperty)?.typeParameters?.map { it.symbol.toLookupTag() }.orEmpty(),
            valueParameterTypes = computeSignatureTypes(call, variable),
            hasExtensionReceiver = variable.receiverParameter != null,
            contextReceiverCount = variable.contextReceivers.size,
            hasVarargs = false,
            numDefaults = 0,
            isExpect = (variable as? FirProperty)?.isExpect == true,
            isSyntheticMember = false // TODO
        )
    }

    protected fun createFlatSignature(call: Candidate, constructor: FirConstructor): FlatSignature<Candidate> {
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
            isSyntheticMember = false // TODO
        )
    }

    protected fun createFlatSignature(call: Candidate, function: FirSimpleFunction): FlatSignature<Candidate> {
        return FlatSignature(
            origin = call,
            typeParameters = function.typeParameters.map { it.symbol.toLookupTag() },
            valueParameterTypes = computeSignatureTypes(call, function),
            hasExtensionReceiver = function.receiverParameter != null,
            contextReceiverCount = function.contextReceivers.size,
            hasVarargs = function.valueParameters.any { it.isVararg },
            numDefaults = call.numDefaults,
            isExpect = function.isExpect,
            isSyntheticMember = false // TODO
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
            addIfNotNull(called.receiverParameter?.typeRef?.coneType?.fullyExpandedType(session)?.let { TypeWithConversion(it) })
            val typeForCallableReference = call.resultingTypeForCallableReference
            if (typeForCallableReference != null) {
                // Return type isn't needed here       v
                typeForCallableReference.typeArguments.dropLast(1)
                    .mapTo(this) {
                        TypeWithConversion((it as ConeKotlinType).fullyExpandedType(session).removeTypeVariableTypes(session.typeContext))
                    }
            } else {
                called.contextReceivers.mapTo(this) { TypeWithConversion(it.typeRef.coneType.fullyExpandedType(session)) }
                call.argumentMapping?.mapTo(this) { (_, parameter) ->
                    val argumentType = parameter.argumentType().fullyExpandedType(session)
                    if (!call.usesSAM) {
                        TypeWithConversion(argumentType)
                    } else {
                        val functionType = samResolver.getFunctionTypeForPossibleSamType(argumentType)
                        if (functionType == null) TypeWithConversion(argumentType)
                        else TypeWithConversion(functionType, argumentType)
                    }
                }
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
