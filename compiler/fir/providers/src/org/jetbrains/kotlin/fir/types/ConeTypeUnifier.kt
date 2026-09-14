/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

object ConeTypeUnifier {
    fun unify(
        primaryType: ConeKotlinType,
        richErrorTypes: List<ConeKotlinType>,
        attributes: ConeAttributes,
        typeContext: ConeTypeContext,
    ): ConeKotlinType {
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
    ): ConeKotlinType {
        val lowerBound = primaryType.lowerBound
        if (lowerBound is ConeUnionType) {
            val upperBound = primaryType.upperBound
            check(upperBound is ConeUnionType) { "Lower bound is a union type but upper bound isn't: $primaryType" }
            val flexiblePrimaryType =
                coneFlexibleOrSimpleType(typeContext, lowerBound.primaryType, upperBound.primaryType, primaryType.isTrivial)
            return unify(flexiblePrimaryType, lowerBound.richErrorTypes, attributes, typeContext)
        }

        return unifyWithoutFlatteningPrimary(primaryType, attributes, richErrorTypes, typeContext)
    }

    private fun unify(
        primaryType: ConeRigidType,
        richErrorTypes: List<ConeKotlinType>,
        attributes: ConeAttributes,
        typeContext: ConeTypeContext
    ): ConeKotlinType {
        require(richErrorTypes.isNotEmpty()) { "Empty list of rich error types" }

        if (primaryType is ConeUnionType) {
            return unify(primaryType.primaryType, primaryType.richErrorTypes + richErrorTypes, attributes, typeContext)
        }

        return unifyWithoutFlatteningPrimary(primaryType, attributes, richErrorTypes, typeContext)
    }

    @OptIn(DelicateUnionConstructor::class)
    private fun unifyWithoutFlatteningPrimary(
        primaryType: ConeKotlinType,
        attributes: ConeAttributes,
        richErrorTypes: List<ConeKotlinType>,
        typeContext: ConeTypeContext,
    ): ConeKotlinType {
        require(primaryType !is ConeUnionType) { "Primary type must not be a union type" }
        var isNullable = false

        fun ConeKotlinType.flattenRecursively(): List<ConeKotlinType> {
            return if (this is ConeUnionType) {
                buildList {
                    addIfNotNull(this@flattenRecursively.primaryType.takeUnless {
                        if (it.isNullableNothing) isNullable = true
                        it.isNothingOrNullableNothing
                    })
                    this@flattenRecursively.richErrorTypes.flatMapTo(this) { it.flattenRecursively() }
                }
            } else {
                [this]
            }
        }

        val flattenedErrorTypes = richErrorTypes.flatMap { it.flattenRecursively() }
        val newPrimaryType = primaryType.applyIf(isNullable) { withNullability(true, typeContext) }

        if (flattenedErrorTypes.size == 1 && newPrimaryType.isNothingOrNullableNothing) {
            return flattenedErrorTypes.single().withNullability(newPrimaryType.isMarkedNullable, typeContext, attributes)
        }

        return ConeUnionType(newPrimaryType, flattenedErrorTypes.distinctBy { it.lookupTagIfAny }, attributes)
    }

}
