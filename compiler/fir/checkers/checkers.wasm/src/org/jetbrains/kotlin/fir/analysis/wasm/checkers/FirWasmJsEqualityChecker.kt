/*
 * Copyright 2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformSpecificEqualityChecker
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirEqualityCompatibilityChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirEqualityCompatibilityChecker.Applicability
import org.jetbrains.kotlin.fir.analysis.checkers.toTypeInfo
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
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

    private val ConeIntersectionType.hasJsAnyOrJsReference: Boolean
        get() = intersectedTypes.any { isJsAnyOrJsReference(it) }

    private inline fun minApplicabilityAmongJsTypesComponents(
        type: ConeKotlinType,
        predicate: (ConeKotlinType) -> Applicability,
    ) = when {
        type is ConeIntersectionType && type.hasJsAnyOrJsReference -> type.intersectedTypes
            .filter { isJsAnyOrJsReference(it) }.minOfOrNull(predicate)
            ?: Applicability.APPLICABLE
        else -> predicate(type)
    }

    context(context: CheckerContext)
    override fun runApplicabilityCheck(
        expression: FirEqualityOperatorCall,
        leftType: ConeKotlinType,
        rightType: ConeKotlinType,
        checker: FirEqualityCompatibilityChecker,
    ): Applicability = minApplicabilityAmongJsTypesComponents(leftType) { leftType ->
        minApplicabilityAmongJsTypesComponents(rightType) { rightType ->
            runCheckForIntersectionComponents(expression.operation, leftType, rightType, checker)
        }
    }

    context(context: CheckerContext)
    private fun runCheckForIntersectionComponents(
        operation: FirOperation,
        leftType: ConeKotlinType,
        rightType: ConeKotlinType,
        checker: FirEqualityCompatibilityChecker,
    ): Applicability {
        val leftClassId = leftType.classId ?: return Applicability.GENERALLY_INAPPLICABLE
        val rightClassId = rightType.classId ?: return Applicability.GENERALLY_INAPPLICABLE
        // allow equality checks with JsAny, as it can contain any JsReference and thus any object
        if (leftClassId == JsStandardClassIds.JsAny || rightClassId == JsStandardClassIds.JsAny)
            return Applicability.APPLICABLE

        // allow equality checks between JsReference<C> and Kotlin types K if a corresponding check between K and C is allowed
        val unwrappedLeftType = tryUnwrapReferenceType(leftType) ?: return Applicability.APPLICABLE
        val unwrappedRightType = tryUnwrapReferenceType(rightType) ?: return Applicability.APPLICABLE

        return checker.checkApplicability(
            operation,
            unwrappedLeftType.toTypeInfo(context.session),
            unwrappedRightType.toTypeInfo(context.session),
        )
    }
}