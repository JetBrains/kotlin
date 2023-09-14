/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef

abstract class ReturnTypeCalculator {
    abstract val callableCopyTypeCalculator: CallableCopyTypeCalculator

    abstract fun tryCalculateReturnTypeOrNull(declaration: FirCallableDeclaration): FirResolvedTypeRef?

    fun tryCalculateReturnType(declaration: FirCallableDeclaration): FirResolvedTypeRef {
        return tryCalculateReturnTypeOrNull(declaration)!!
    }

    fun tryCalculateReturnType(symbol: FirCallableSymbol<*>): FirResolvedTypeRef {
        return tryCalculateReturnType(symbol.fir)
    }
}
