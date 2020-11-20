/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

object ConeKotlinTypeComparator : Comparator<ConeKotlinType> {
    private val ConeKotlinType.priority : Int
        get() = when (this) {
            is ConeKotlinErrorType -> 8
            is ConeLookupTagBasedType -> 7
            is ConeFlexibleType -> 6
            is ConeCapturedType -> 5
            is ConeDefinitelyNotNullType -> 4
            is ConeIntersectionType -> 3
            is ConeStubType -> 2
            is ConeIntegerLiteralType -> 1
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
            is ConeKotlinErrorType -> {
                require(b is ConeKotlinErrorType) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                return a.hashCode() - b.hashCode()
            }
            is ConeLookupTagBasedType -> {
                require(b is ConeLookupTagBasedType) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
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
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                val lowerBoundDiff = compare(a.lowerBound, b.lowerBound)
                if (lowerBoundDiff != 0) {
                    return lowerBoundDiff
                }
                return compare(a.upperBound, b.upperBound)
            }
            is ConeCapturedType -> {
                require(b is ConeCapturedType) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
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
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                return compare(a.original, b.original)
            }
            is ConeIntersectionType -> {
                require(b is ConeIntersectionType) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                val sizeDiff = a.intersectedTypes.size - b.intersectedTypes.size
                if (sizeDiff != 0) {
                    return 0
                }
                // Can't compare individual types from each side, since their orders are not guaranteed.
                return a.hashCode() - b.hashCode()
            }
            is ConeStubType -> {
                require(b is ConeStubType) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
                }
                val nameDiff = a.variable.typeConstructor.name.compareTo(b.variable.typeConstructor.name)
                if (nameDiff != 0) {
                    return nameDiff
                }
                return compare(a.nullability, b.nullability)
            }
            is ConeIntegerLiteralType -> {
                require(b is ConeIntegerLiteralType) {
                    "priority is inconsistent: ${a.render()} v.s. ${b.render()}"
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
            else ->
                error("Unsupported type comparison: ${a.render()} v.s. ${b.render()}")
        }
    }
}
