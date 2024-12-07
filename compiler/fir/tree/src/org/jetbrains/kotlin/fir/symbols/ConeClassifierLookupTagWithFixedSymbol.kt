/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.types.ConeClassifierLookupTag

/**
 * @see org.jetbrains.kotlin.fir.types.ConeClassifierLookupTag
 */
abstract class ConeClassifierLookupTagWithFixedSymbol : ConeClassifierLookupTag() {
    abstract val symbol: FirClassifierSymbol<*>
}