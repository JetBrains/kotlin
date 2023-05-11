/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.mpp.ClassifierSymbolMarker

sealed class FirClassifierSymbol<E : FirDeclaration> : FirBasedSymbol<E>(), ClassifierSymbolMarker {
    abstract fun toLookupTag(): ConeClassifierLookupTag
}
