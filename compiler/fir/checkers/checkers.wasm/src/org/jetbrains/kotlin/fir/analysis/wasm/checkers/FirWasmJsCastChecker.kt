/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.FirPlatformSpecificCastChecker
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirWasmJsCastChecker : FirPlatformSpecificCastChecker() {
    override fun shouldSuppressImpossibleCast(session: FirSession, fromType: ConeKotlinType, toType: ConeKotlinType): Boolean {
        return false
    }

    override fun shouldSuppressImpossibleIsCheck(session: FirSession, fromType: ConeKotlinType, toType: ConeKotlinType): Boolean {
        // checks from JsReference<C> to Kotlin types are allowed as its implicit cast to Any gives the "wrapped" object back
        // also, it can contain not only `C` due to unsafe casts, so we do not check toType here
        return fromType.classId !== JsStandardClassIds.JsReference
    }
}