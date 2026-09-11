/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.utils.addIfNotNull

object ConeTypeUnifier {
    fun unify(
        primaryType: ConeKotlinType?,
        richErrorTypes: List<ConeKotlinType>,
        attributes: ConeAttributes,
        typeContext: ConeTypeContext,
    ): ConeUnionType {
        return when (primaryType) {
            is ConeRigidType? -> unify(primaryType, richErrorTypes, attributes, typeContext)
            is ConeFlexibleType -> unify(primaryType, richErrorTypes, attributes, typeContext)
        }
    }

    private fun unify(
        primaryType: ConeFlexibleType,
        richErrorTypes: List<ConeKotlinType>,
        attributes: ConeAttributes,
        typeContext: ConeTypeContext
    ): ConeUnionType {
        val lowerBound = primaryType.lowerBound
        if (lowerBound is ConeUnionType) {
            val upperBound = primaryType.upperBound
            check(upperBound is ConeUnionType) { "Lower bound is a union type but upper bound isn't: $primaryType" }
            val flexiblePrimaryType = lowerBound.primaryType?.let {
                val upperBoundPrimaryType = upperBound.primaryType
                checkNotNull(upperBoundPrimaryType) { "Lower bound has a primary type but upper bound doesn't: $primaryType" }
                coneFlexibleOrSimpleType(typeContext, it, upperBoundPrimaryType, primaryType.isTrivial)
            }
            return unify(flexiblePrimaryType, lowerBound.richErrorTypes, attributes, typeContext)
        }

        return unifyWithoutFlatteningPrimary(primaryType, attributes, richErrorTypes)
    }

    private fun unify(
        primaryType: ConeRigidType?,
        richErrorTypes: List<ConeKotlinType>,
        attributes: ConeAttributes,
        typeContext: ConeTypeContext
    ): ConeUnionType {
        require(richErrorTypes.isNotEmpty()) { "Empty list of rich error types" }

        if (primaryType is ConeUnionType) {
            return unify(primaryType.primaryType, primaryType.richErrorTypes + richErrorTypes, attributes, typeContext)
        }

        return unifyWithoutFlatteningPrimary(primaryType, attributes, richErrorTypes)
    }

    @OptIn(DelicateUnionConstructor::class)
    private fun unifyWithoutFlatteningPrimary(
        primaryType: ConeKotlinType?,
        attributes: ConeAttributes,
        richErrorTypes: List<ConeKotlinType>,
    ): ConeUnionType {
        require(primaryType !is ConeUnionType) { "Primary type must not be a union type" }

        val flattenedErrorTypes = richErrorTypes.flatMap { it.flattenRecursively() }
        return ConeUnionType(primaryType, flattenedErrorTypes.distinctBy { it.lookupTagIfAny }, attributes)
    }

    private fun ConeKotlinType.flattenRecursively(): List<ConeKotlinType> {
        return if (this is ConeUnionType) {
            buildList {
                addIfNotNull(primaryType)
                richErrorTypes.flatMapTo(this) { it.flattenRecursively() }
            }
        } else {
            [this]
        }
    }
}
