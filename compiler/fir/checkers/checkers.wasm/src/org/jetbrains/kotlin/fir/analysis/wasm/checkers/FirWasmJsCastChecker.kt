/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.FirPlatformSpecificCastChecker
import org.jetbrains.kotlin.fir.analysis.checkers.TypeInfo
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCastOperatorsChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toTypeInfo
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirWasmJsCastChecker : FirPlatformSpecificCastChecker() {

    context(context: CheckerContext)
    override fun runApplicabilityCheck(
        expression: FirTypeOperatorCall,
        fromType: ConeKotlinType,
        toType: ConeKotlinType,
        checker: FirCastOperatorsChecker,
    ): FirCastOperatorsChecker.Applicability {
        val fromTypeInfo = fromType.toTypeInfo(context.session)
        val toTypeInfo = toType.toTypeInfo(context.session)
        return checker.checkGeneralApplicability(expression, fromTypeInfo, toTypeInfo).let {
            if (it.isImpossibleCastOrIsCheck() && shouldSuppressImpossibleCastOrIsCheck(expression, fromType, toTypeInfo, checker))
                FirCastOperatorsChecker.Applicability.APPLICABLE
            else it
        }
    }

    private fun FirCastOperatorsChecker.Applicability.isImpossibleCastOrIsCheck() =
        this == FirCastOperatorsChecker.Applicability.IMPOSSIBLE_CAST || this == FirCastOperatorsChecker.Applicability.IMPOSSIBLE_IS_CHECK

    context(context: CheckerContext)
    private fun shouldSuppressImpossibleCastOrIsCheck(
        expression: FirTypeOperatorCall,
        fromType: ConeKotlinType,
        toType: TypeInfo,
        checker: FirCastOperatorsChecker,
    ): Boolean {
        // checks from JsReference<C> to Kotlin types (compatible with `C`) are allowed as its implicit cast to Any gives
        // the "wrapped" object back
        if (fromType.classId == JsStandardClassIds.JsReference && fromType.typeArguments.size == 1) {
            val typeArg: ConeTypeProjection = fromType.typeArguments[0]
            val unwrappedFromType = when (typeArg) {
                is ConeKotlinTypeProjectionOut -> typeArg.type
                is ConeKotlinType -> typeArg
                else -> return true // e.g. star projection
            }
            val applicabilityForUnwrappedType = checker.checkGeneralApplicability(expression, unwrappedFromType.toTypeInfo(context.session), toType)
            return !applicabilityForUnwrappedType.isImpossibleCastOrIsCheck()
        }
        // checks from JsAny are allowed as it can hold JsReference object
        if (fromType.classId == JsStandardClassIds.JsAny) return true

        return false
    }
}