/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.expressions.FirAnnotation

abstract class FirBasedSymbol<E : FirDeclaration> {
    private var _fir: E? = null

    @SymbolInternals
    val fir: E
        get() = _fir
            ?: error("Fir is not initialized for $this")

    fun bind(e: E) {
        _fir = e
    }

    val origin: FirDeclarationOrigin
        get() = fir.origin

    val source: FirSourceElement?
        get() = fir.source

    val moduleData: FirModuleData
        get() = fir.moduleData

    val annotations: List<FirAnnotation>
        get() = (fir as? FirAnnotatedDeclaration)?.annotations ?: emptyList()
}

@RequiresOptIn
annotation class SymbolInternals
