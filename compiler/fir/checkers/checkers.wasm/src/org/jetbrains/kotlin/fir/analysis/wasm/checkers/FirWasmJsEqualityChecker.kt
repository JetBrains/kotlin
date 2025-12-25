/*
 * Copyright 2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.FirPlatformSpecificEqualityChecker
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirWasmJsEqualityChecker : FirPlatformSpecificEqualityChecker() {
    // if the given type is not JsReference, returns that type
    // if the given type is JsReference, returns unwrapped upper bound (if any)
    // returns null if there is no known upper bound, so it can be any type
    fun tryUnwrapReferenceType(type: ConeKotlinType): ConeKotlinType? {
        if (type.classId != JsStandardClassIds.JsReference) return type
        val typeArg: ConeTypeProjection? = type.typeArguments.firstOrNull()
        return when (typeArg) {
            is ConeKotlinTypeProjectionOut -> typeArg.type
            is ConeKotlinType -> typeArg
            else -> null
        }
    }

    private fun isJsAnyOrJsReference(type: ConeKotlinType): Boolean {
        val classId = type.classId ?: return false
        return classId == JsStandardClassIds.JsAny || classId == JsStandardClassIds.JsReference
    }

    private fun isIntersectionWithJsAnyOrJsReference(type: ConeKotlinType): Boolean {
        return type is ConeIntersectionType && type.intersectedTypes.any { isJsAnyOrJsReference(it) }
    }

    override fun shouldSuppressInapplicableEquality(
        leftType: ConeKotlinType,
        rightType: ConeKotlinType,
        generalApplicabilityChecker: (fromType: ConeKotlinType, toType: ConeKotlinType) -> Boolean,
    ): Boolean {
        if (isIntersectionWithJsAnyOrJsReference(leftType)) {
            return (leftType as ConeIntersectionType).intersectedTypes
                .filter { isJsAnyOrJsReference(it) }
                .all { shouldSuppressInapplicableEquality(it, rightType, generalApplicabilityChecker) }
        }

        if (isIntersectionWithJsAnyOrJsReference(rightType)) {
            return (rightType as ConeIntersectionType).intersectedTypes
                .filter { isJsAnyOrJsReference(it) }
                .all { shouldSuppressInapplicableEquality(leftType, it, generalApplicabilityChecker) }
        }

        val leftClassId = leftType.classId ?: return false
        val rightClassId = rightType.classId ?: return false
        // allow equality checks with JsAny, as it can contain any JsReference and thus any object
        if (leftClassId == JsStandardClassIds.JsAny || rightClassId == JsStandardClassIds.JsAny)
            return true

        // allow equality checks between JsReference<C> and Kotlin types K if a corresponding check between K and C is allowed
        if (leftClassId == JsStandardClassIds.JsReference || rightClassId == JsStandardClassIds.JsReference) {
            val unwrappedLeftType = tryUnwrapReferenceType(leftType) ?: return true
            val unwrappedRightType = tryUnwrapReferenceType(rightType) ?: return true
            return generalApplicabilityChecker.invoke(unwrappedLeftType, unwrappedRightType)
        }

        return false
    }
}