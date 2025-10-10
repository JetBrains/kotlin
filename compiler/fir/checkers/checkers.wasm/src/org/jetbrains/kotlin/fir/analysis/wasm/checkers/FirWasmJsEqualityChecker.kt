/*
 * Copyright 2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.fir.FirPlatformSpecificEqualityChecker
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirWasmJsEqualityChecker : FirPlatformSpecificEqualityChecker() {
    override fun shouldSuppressInapplicableEquality(leftType: ConeKotlinType, rightType: ConeKotlinType): Boolean {
        // allow equality checks between JsReference<C> (and thus JsAny) and Kotlin types
        return leftType.classId !== JsStandardClassIds.JsReference && rightType.classId !== JsStandardClassIds.JsReference
    }
}