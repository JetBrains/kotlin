/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

class ConeIntegerLiteralTypeImpl : ConeIntegerLiteralType {
    override val possibleTypes: Collection<ConeClassLikeType>

    override val attributes: ConeAttributes
        get() = ConeAttributes.Empty

    constructor(value: Long, isUnsigned: Boolean, nullability: ConeNullability = ConeNullability.NOT_NULL) : super(value, isUnsigned, nullability) {
        possibleTypes = mutableListOf()

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
        } else {
            addSignedPossibleTypes()
        }
    }

    private constructor(
        value: Long,
        possibleTypes: Collection<ConeClassLikeType>,
        isUnsigned: Boolean,
        nullability: ConeNullability = ConeNullability.NOT_NULL
    ) : super(value, isUnsigned, nullability) {
        this.possibleTypes = possibleTypes
    }

    override val supertypes: List<ConeClassLikeType> by lazy {
        listOf(
            NUMBER_TYPE,
            ConeClassLikeTypeImpl(COMPARABLE_TAG, arrayOf(ConeKotlinTypeProjectionIn(this)), false)
        )
    }

    override fun getApproximatedType(expectedType: ConeKotlinType?): ConeClassLikeType {
        val approximatedType = when (val expectedTypeForApproximation = expectedType?.lowerBoundIfFlexible()) {
            null, !in possibleTypes -> possibleTypes.first()
            else -> expectedTypeForApproximation as ConeClassLikeType
        }
        return approximatedType.withNullability(nullability)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    companion object {
        fun createType(classId: ClassId): ConeClassLikeType {
            return ConeClassLikeTypeImpl(ConeClassLikeLookupTagImpl(classId), emptyArray(), false)
        }

        private val NUMBER_TYPE = createType(StandardClassIds.Number)

        private val INT_RANGE = Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
        private val BYTE_RANGE = Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()
        private val SHORT_RANGE = Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()

        private val UBYTE_RANGE = UByte.MIN_VALUE.toLong()..UByte.MAX_VALUE.toLong()
        private val USHORT_RANGE = UShort.MIN_VALUE.toLong()..UShort.MAX_VALUE.toLong()
        private val UINT_RANGE = UInt.MIN_VALUE.toLong()..UInt.MAX_VALUE.toLong()

        private val COMPARABLE_TAG = ConeClassLikeLookupTagImpl(StandardClassIds.Comparable)

        fun findCommonSuperType(types: Collection<SimpleTypeMarker>): SimpleTypeMarker? {
            return findCommonSuperTypeOrIntersectionType(types, Mode.COMMON_SUPER_TYPE)
        }

        fun findIntersectionType(types: Collection<SimpleTypeMarker>): SimpleTypeMarker? {
            return findCommonSuperTypeOrIntersectionType(types, Mode.INTERSECTION_TYPE)
        }

        private enum class Mode {
            COMMON_SUPER_TYPE, INTERSECTION_TYPE
        }

        /**
         * intersection(ILT(types), PrimitiveType) = commonSuperType(ILT(types), PrimitiveType) =
         *      PrimitiveType  in types  -> PrimitiveType
         *      PrimitiveType !in types -> null
         *
         * intersection(ILT(types_1), ILT(types_2)) = ILT(types_1 union types_2)
         *
         * commonSuperType(ILT(types_1), ILT(types_2)) = ILT(types_1 intersect types_2)
         */
        private fun findCommonSuperTypeOrIntersectionType(types: Collection<SimpleTypeMarker>, mode: Mode): SimpleTypeMarker? {
            if (types.isEmpty()) return null
            @Suppress("UNCHECKED_CAST")
            return types.reduce { left: SimpleTypeMarker?, right: SimpleTypeMarker? -> fold(left, right, mode) }
        }

        private fun fold(left: SimpleTypeMarker?, right: SimpleTypeMarker?, mode: Mode): SimpleTypeMarker? {
            if (left == null || right == null) return null
            return when {
                left is ConeIntegerLiteralType && right is ConeIntegerLiteralType ->
                    fold(left, right, mode)

                left is ConeIntegerLiteralType -> fold(left, right)
                right is ConeIntegerLiteralType -> fold(right, left)
                else -> null
            }
        }

        private fun fold(left: ConeIntegerLiteralType, right: ConeIntegerLiteralType, mode: Mode): ConeIntegerLiteralType? {
            val possibleTypes = when (mode) {
                Mode.COMMON_SUPER_TYPE -> left.possibleTypes intersect right.possibleTypes
                Mode.INTERSECTION_TYPE -> left.possibleTypes union right.possibleTypes
            }
            return ConeIntegerLiteralTypeImpl(left.value, possibleTypes, left.isUnsigned)
        }

        private fun fold(left: ConeIntegerLiteralType, right: SimpleTypeMarker): SimpleTypeMarker? =
            if (right in left.possibleTypes) right else null
    }
}

fun ConeKotlinType.approximateIntegerLiteralType(expectedType: ConeKotlinType? = null): ConeKotlinType =
    (this as? ConeIntegerLiteralType)?.getApproximatedType(expectedType) ?: this

fun ConeKotlinType.approximateIntegerLiteralTypeOrNull(expectedType: ConeKotlinType? = null): ConeKotlinType? =
    (this as? ConeIntegerLiteralType)?.getApproximatedType(expectedType)

private fun ConeClassLikeType.withNullability(nullability: ConeNullability): ConeClassLikeType {
    if (nullability == this.nullability) return this

    return when (this) {
        is ConeClassErrorType -> this
        is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, nullability.isNullable)
        else -> error("sealed")
    }
}