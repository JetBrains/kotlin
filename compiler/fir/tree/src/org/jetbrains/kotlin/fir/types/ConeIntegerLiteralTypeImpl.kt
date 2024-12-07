/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.isLong
import org.jetbrains.kotlin.fir.isULong
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralTypeExtensions.approximateIntegerLiteralBounds
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralTypeExtensions.createClassLikeType
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralTypeExtensions.createSupertypeList
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralTypeExtensions.getApproximatedTypeImpl
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralTypeExtensions.withNullabilityAndAttributes
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.RigidTypeMarker

class ConeIntegerLiteralConstantTypeImpl(
    value: Long,
    override val possibleTypes: Collection<ConeClassLikeType>,
    isUnsigned: Boolean,
    isMarkedNullable: Boolean
) : ConeIntegerLiteralConstantType(value, isUnsigned, isMarkedNullable) {
    override val supertypes: List<ConeClassLikeType> by lazy {
        createSupertypeList(this)
    }

    override fun getApproximatedType(expectedType: ConeKotlinType?): ConeClassLikeType {
        return getApproximatedTypeImpl(expectedType)
    }

    companion object {
        fun create(
            value: Long,
            isUnsigned: Boolean,
            isTypePresent: (ConeClassLikeType) -> Boolean,
        ): ConeSimpleKotlinType {
            val possibleTypes = mutableListOf<ConeClassLikeType>()

            fun checkBoundsAndAddPossibleType(classId: ClassId, range: LongRange) {
                if (value in range) {
                    possibleTypes.add(createType(classId))
                }
            }

            fun addSignedPossibleTypes() {
                checkBoundsAndAddPossibleType(StandardClassIds.Int, INT_RANGE)
                possibleTypes += createType(StandardClassIds.Long)
                checkBoundsAndAddPossibleType(StandardClassIds.Byte, BYTE_RANGE)
                checkBoundsAndAddPossibleType(StandardClassIds.Short, SHORT_RANGE)
            }

            fun addUnsignedPossibleType() {
                checkBoundsAndAddPossibleType(StandardClassIds.UInt, UINT_RANGE)
                possibleTypes += createType(StandardClassIds.ULong)
                checkBoundsAndAddPossibleType(StandardClassIds.UByte, UBYTE_RANGE)
                checkBoundsAndAddPossibleType(StandardClassIds.UShort, USHORT_RANGE)
            }

            if (isUnsigned) {
                addUnsignedPossibleType()
                if (possibleTypes.any { !isTypePresent(it) }) {
                    return ConeErrorType(ConeSimpleDiagnostic("Unsigned integers need stdlib", DiagnosticKind.UnsignedNumbersAreNotPresent))
                }
            } else {
                addSignedPossibleTypes()
            }
            return if (possibleTypes.size == 1) {
                possibleTypes.single().withNullabilityAndAttributes(false, ConeAttributes.Empty).also {
                    if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                        assert(it.isLong() || it.isULong())
                    }
                }
            } else {
                ConeIntegerLiteralConstantTypeImpl(value, possibleTypes, isUnsigned, isMarkedNullable = false)
            }
        }

        private fun createType(classId: ClassId): ConeClassLikeType {
            return ConeClassLikeTypeImpl(classId.toLookupTag(), EMPTY_ARRAY, false)
        }

        private val INT_RANGE = Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
        private val BYTE_RANGE = Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()
        private val SHORT_RANGE = Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()

        private val UBYTE_RANGE = UByte.MIN_VALUE.toLong()..UByte.MAX_VALUE.toLong()
        private val USHORT_RANGE = UShort.MIN_VALUE.toLong()..UShort.MAX_VALUE.toLong()
        private val UINT_RANGE = UInt.MIN_VALUE.toLong()..UInt.MAX_VALUE.toLong()
    }
}

class ConeIntegerConstantOperatorTypeImpl(
    isUnsigned: Boolean,
    isMarkedNullable: Boolean,
) : ConeIntegerConstantOperatorType(isUnsigned, isMarkedNullable) {
    override val possibleTypes: Collection<ConeClassLikeType> = when (isUnsigned) {
        false -> setOf(
            createClassLikeType(StandardClassIds.Int),
            createClassLikeType(StandardClassIds.Long),
        )
        true -> setOf(
            createClassLikeType(StandardClassIds.UInt),
            createClassLikeType(StandardClassIds.ULong),
        )
    }

    override val supertypes: List<ConeClassLikeType> by lazy {
        createSupertypeList(this)
    }

    override fun getApproximatedType(expectedType: ConeKotlinType?): ConeClassLikeType {
        return getApproximatedTypeImpl(expectedType)
    }
}

/**
 * This methods detects common super type only for special rules for integer literal types
 * If it returns null then CST will be found by regular rules using real supertypes
 *   of integer literal types
 */
fun ConeIntegerLiteralType.Companion.findCommonSuperType(types: Collection<RigidTypeMarker>): RigidTypeMarker? {
    return ConeIntegerLiteralTypeExtensions.findCommonSuperType(types)
}

fun ConeKotlinType.approximateIntegerLiteralType(expectedType: ConeKotlinType? = null): ConeKotlinType {
    return when (this) {
        is ConeIntegerLiteralType -> getApproximatedType(expectedType)
        is ConeFlexibleType -> approximateIntegerLiteralBounds(expectedType)
        else -> this
    }
}

private object ConeIntegerLiteralTypeExtensions {
    fun createSupertypeList(type: ConeIntegerLiteralType): List<ConeClassLikeType> {
        val comparableSuperType =
            ConeClassLikeTypeImpl(StandardClassIds.Comparable.toLookupTag(), arrayOf(ConeKotlinTypeProjectionIn(type)), false)

        return if (type.possibleTypes.none { it.isUnsignedTypeOrNullableUnsignedType }) {
            listOf(
                createClassLikeType(StandardClassIds.Number),
                comparableSuperType,
            )
        } else {
            listOf(comparableSuperType)
        }
    }

    fun createClassLikeType(classId: ClassId): ConeClassLikeType {
        return ConeClassLikeTypeImpl(classId.toLookupTag(), ConeTypeProjection.EMPTY_ARRAY, false)
    }

    fun ConeIntegerLiteralType.getApproximatedTypeImpl(expectedType: ConeKotlinType?): ConeClassLikeType {
        val expectedTypeForApproximation = (expectedType?.lowerBoundIfFlexible() as? ConeClassLikeType)
            ?.withNullabilityAndAttributes(false, ConeAttributes.Empty)
        val approximatedType = when (expectedTypeForApproximation) {
            null, !in possibleTypes -> possibleTypes.first()
            else -> expectedTypeForApproximation
        }
        return approximatedType.withNullabilityAndAttributes(isMarkedNullable, attributes)
    }


    fun findCommonSuperType(types: Collection<RigidTypeMarker>): RigidTypeMarker? {
        if (types.isEmpty()) return null
        @Suppress("UNCHECKED_CAST")
        return types.reduce { left: RigidTypeMarker?, right: RigidTypeMarker? -> commonSuperType(left, right) }
    }

    private fun commonSuperType(left: RigidTypeMarker?, right: RigidTypeMarker?): RigidTypeMarker? {
        if (left == null || right == null) return null

        return when {
            left is ConeIntegerLiteralType && right !is ConeIntegerLiteralType -> {
                commonSuperTypeBetweenIntegerTypeAndRegularType(left, right)
            }

            right is ConeIntegerLiteralType && left !is ConeIntegerLiteralType -> {
                commonSuperTypeBetweenIntegerTypeAndRegularType(right, left)
            }

            left is ConeIntegerLiteralConstantType && right is ConeIntegerLiteralConstantType -> {
                commonSuperTypeBetweenTwoConstantTypes(left, right)
            }

            left is ConeIntegerConstantOperatorType -> left
            right is ConeIntegerConstantOperatorType -> right
            else -> null
        }
    }

    private fun commonSuperTypeBetweenIntegerTypeAndRegularType(
        integerLiteralType: ConeIntegerLiteralType,
        regularType: RigidTypeMarker
    ): RigidTypeMarker? {
        return when (regularType) {
            in integerLiteralType.possibleTypes -> regularType
            else -> null
        }
    }

    private fun commonSuperTypeBetweenTwoConstantTypes(
        left: ConeIntegerLiteralConstantType,
        right: ConeIntegerLiteralConstantType
    ): ConeIntegerLiteralConstantType {
        val possibleTypes = left.possibleTypes intersect right.possibleTypes
        return ConeIntegerLiteralConstantTypeImpl(left.value, possibleTypes, left.isUnsigned, left.isMarkedNullable)
    }

    fun ConeFlexibleType.approximateIntegerLiteralBounds(expectedType: ConeKotlinType? = null): ConeFlexibleType {
        val newLowerBound = lowerBound.approximateIntegerLiteralType(expectedType)
        val newUpperBound = upperBound.approximateIntegerLiteralType(expectedType)

        if (newLowerBound !== lowerBound || newUpperBound !== upperBound) {
            return ConeFlexibleType(
                newLowerBound.lowerBoundIfFlexible(),
                newUpperBound.upperBoundIfFlexible()
            )
        }

        return this
    }

    fun ConeClassLikeType.withNullabilityAndAttributes(isMarkedNullable: Boolean, attributes: ConeAttributes): ConeClassLikeType {
        if (isMarkedNullable == this.isMarkedNullable && attributes == this.attributes) return this

        return when (this) {
            is ConeErrorType -> this
            is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, isMarkedNullable, attributes)
            else -> error("sealed")
        }
    }
}
