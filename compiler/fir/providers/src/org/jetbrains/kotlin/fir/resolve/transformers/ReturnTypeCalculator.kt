/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirTypedDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

abstract class ReturnTypeCalculator {
    abstract fun tryCalculateReturnType(declaration: FirTypedDeclaration): FirResolvedTypeRef

    fun tryCalculateReturnType(symbol: FirCallableSymbol<*>): FirResolvedTypeRef {
        return tryCalculateReturnType(symbol.fir)
    }
}
