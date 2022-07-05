/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

object ConeKotlinTypeComparator : Comparator<ConeKotlinType> {
    private val ConeKotlinType.priority : Int
        get() = when (this) {
            is ConeErrorType -> 9
            is ConeLookupTagBasedType -> 8
            is ConeFlexibleType -> 7
            is ConeCapturedType -> 6
            is ConeDefinitelyNotNullType -> 5
            is ConeIntersectionType -> 4
            is ConeStubType -> 3
            is ConeIntegerLiteralConstantType -> 2
            is ConeIntegerConstantOperatorType -> 1
            else -> 0
        }

    private fun compare(a: ConeTypeProjection, b: ConeTypeProjection): Int {
        val kindDiff = a.kind.ordinal - b.kind.ordinal
        if (kindDiff != 0) {
            return kindDiff
        }
        when (a) {
            is ConeStarProjection -> return 0
            is ConeKotlinTypeProjectionIn -> {
                require(b is ConeKotlinTypeProjectionIn) {
                    "ordinal is inconsistent: $a v.s. $b"
                }
                return compare(a.type, b.type)
            }
            is ConeKotlinTypeProjectionOut -> {
                require(b is ConeKotlinTypeProjectionOut) {
                    "ordinal is inconsistent: $a v.s. $b"
                }
                return compare(a.type, b.type)
            }
            else -> {
                assert(a is ConeKotlinType && b is ConeKotlinType) {
                    "Expect INVARIANT: $a v.s. $b"
                }
                return compare(a as ConeKotlinType, b as ConeKotlinType)
            }
        }
    }

    private fun compare(a: Array<out ConeTypeProjection>, b: Array<out ConeTypeProjection>): Int {
        val sizeDiff = a.size - b.size
        if (sizeDiff != 0) {
            return sizeDiff
        }
        for ((aTypeProjection, bTypeProjection) in a.zip(b)) {
            val typeProjectionDiff = compare(aTypeProjection, bTypeProjection)
            if (typeProjectionDiff != 0) {
                return typeProjectionDiff
            }
        }
        return 0
    }

    private fun compare(a: ConeNullability, b: ConeNullability): Int {
        return a.ordinal - b.ordinal
    }

    override fun compare(a: ConeKotlinType, b: ConeKotlinType): Int {
        val priorityDiff = a.priority - b.priority
        if (priorityDiff != 0) {
            return priorityDiff
        }

        when (a) {
            is ConeErrorType -> {
                require(b is ConeErrorType) {
                    "priority is inconsistent: ${a.renderForDebugging()} v.s. ${b.renderForDebugging()}"
                }
                return a.hashCode() - b.hashCode()
            }
            is ConeLookupTagBasedType -> {
                require(b is ConeLookupTagBasedType) {
                    "priority is inconsistent: ${a.renderForDebugging()} v.s. ${b.renderForDebugging()}"
                }
                val nameDiff = a.lookupTag.name.compareTo(b.lookupTag.name)
                if (nameDiff != 0) {
                    return nameDiff
                }
                val nullabilityDiff = compare(a.nullability, b.nullability)
                if (nullabilityDiff != 0) {
                    return nullabilityDiff
                }
                return compare(a.typeArguments, b.typeArguments)
            }
            is ConeFlexibleType -> {
                require(b is ConeFlexibleType) {
                    "priority is inconsistent: ${a.renderForDebugging()} v.s. ${b.renderForDebugging()}"
                }
                val lowerBoundDiff = compare(a.lowerBound, b.lowerBound)
                if (lowerBoundDiff != 0) {
                    return lowerBoundDiff
                }
                return compare(a.upperBound, b.upperBound)
            }
            is ConeCapturedType -> {
                require(b is ConeCapturedType) {
                    "priority is inconsistent: ${a.renderForDebugging()} v.s. ${b.renderForDebugging()}"
                }
                val aHasLowerType = if (a.lowerType != null) 1 else 0
                val bHasLowerType = if (b.lowerType != null) 1 else 0
                val hasLowerTypeDiff = aHasLowerType - bHasLowerType
                if (hasLowerTypeDiff != 0) {
                    return hasLowerTypeDiff
                }
                if (a.lowerType != null && b.lowerType != null) {
                    val lowerTypeDiff = compare(a.lowerType!!, b.lowerType!!)
                    if (lowerTypeDiff != 0) {
                        return lowerTypeDiff
                    }
                }
                val nullabilityDiff = compare(a.nullability, b.nullability)
                if (nullabilityDiff != 0) {
                    return nullabilityDiff
                }
                return a.constructor.hashCode() - b.constructor.hashCode()
            }
            is ConeDefinitelyNotNullType -> {
                require(b is ConeDefinitelyNotNullType) {
                    "priority is inconsistent: ${a.renderForDebugging()} v.s. ${b.renderForDebugging()}"
                }
                return compare(a.original, b.original)
            }
            is ConeIntersectionType -> {
                require(b is ConeIntersectionType) {
                    "priority is inconsistent: ${a.renderForDebugging()} v.s. ${b.renderForDebugging()}"
                }
                val sizeDiff = a.intersectedTypes.size - b.intersectedTypes.size
                if (sizeDiff != 0) {
                    return sizeDiff
                }
                // Can't compare individual types from each side, since their orders are not guaranteed.
                return a.hashCode() - b.hashCode()
            }
            is ConeStubType -> {
                require(b is ConeStubType) {
                    "priority is inconsistent: ${a.renderForDebugging()} v.s. ${b.renderForDebugging()}"
                }
                val nameDiff = a.constructor.variable.typeConstructor.name.compareTo(b.constructor.variable.typeConstructor.name)
                if (nameDiff != 0) {
                    return nameDiff
                }
                return compare(a.nullability, b.nullability)
            }
            is ConeIntegerLiteralConstantType -> {
                require(b is ConeIntegerLiteralConstantType) {
                    "priority is inconsistent: ${a.renderForDebugging()} v.s. ${b.renderForDebugging()}"
                }
                val valueDiff = a.value - b.value
                if (valueDiff != 0L) {
                    return valueDiff.toInt()
                }
                val nullabilityDiff = compare(a.nullability, b.nullability)
                if (nullabilityDiff != 0) {
                    return nullabilityDiff
                }
                // Can't compare individual types from each side, since their orders are not guaranteed.
                return a.hashCode() - b.hashCode()
            }
            is ConeIntegerConstantOperatorType -> {
                return compare(a.nullability, b.nullability)
            }
            else ->
                error("Unsupported type comparison: ${a.renderForDebugging()} v.s. ${b.renderForDebugging()}")
        }
    }
}
