/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.FirPlatformSpecificCastChecker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirWasmJsCastChecker : FirPlatformSpecificCastChecker() {
    override fun shouldSuppressImpossibleCast(
        session: FirSession,
        fromType: ConeKotlinType,
        toType: ConeKotlinType,
        generalApplicabilityChecker: (fromType: ConeKotlinType, toType: ConeKotlinType) -> Boolean
    ): Boolean = shouldSuppressImpossibleCastOrIsCheck(fromType, toType, generalApplicabilityChecker)

    override fun shouldSuppressImpossibleIsCheck(
        session: FirSession,
        fromType: ConeKotlinType,
        toType: ConeKotlinType,
        generalApplicabilityChecker: (fromType: ConeKotlinType, toType: ConeKotlinType) -> Boolean
    ): Boolean = shouldSuppressImpossibleCastOrIsCheck(fromType, toType, generalApplicabilityChecker)

    private fun shouldSuppressImpossibleCastOrIsCheck(
        fromType: ConeKotlinType,
        toType: ConeKotlinType,
        generalApplicabilityChecker: (fromType: ConeKotlinType, toType: ConeKotlinType) -> Boolean
    ): Boolean {
        // checks from JsReference<C> to Kotlin types (compatible with `C`) are allowed as its implicit cast to Any gives
        // the "wrapped" object back
        if (fromType.classId == JsStandardClassIds.JsReference && fromType.typeArguments.size == 1) {
            val typeArg: ConeTypeProjection = fromType.typeArguments[0]
            return when (typeArg) {
                is ConeKotlinTypeProjectionOut -> generalApplicabilityChecker.invoke(typeArg.type, toType)
                is ConeKotlinType -> generalApplicabilityChecker.invoke(typeArg, toType)
                else -> true // e.g. star projection
            }
        }
        // checks from JsAny are allowed as it can hold JsReference object
        if (fromType.classId == JsStandardClassIds.JsAny) return true

        return false
    }
}