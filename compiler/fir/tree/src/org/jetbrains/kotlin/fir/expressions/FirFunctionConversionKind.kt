/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.fir.types.ConeClassLikeType

sealed class FirFunctionConversionKind {
    data object Sam : FirFunctionConversionKind()
    class BetweenFunctionTypes(
        /**
         * For example, from `() -> String` to `suspend () -> String`
         */
        val isFromSimpleToCustom: Boolean,
        /**
         * From `() -> Int` to `() -> Unit`
         */
        val isForUnitCoercion: Boolean,
        /**
         * Argument type itself or one of its supertypes which is a basic function type.
         * It's used to determine which `invoke` function needs to be called in the adapter.
         */
        val originalArgumentAsFunctionType: ConeClassLikeType,
    ) : FirFunctionConversionKind()
}