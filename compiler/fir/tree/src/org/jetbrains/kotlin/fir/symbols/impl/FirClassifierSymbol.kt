/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirNamedDeclaration
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.FirSymbolOwner
import org.jetbrains.kotlin.types.model.TypeParameterMarker

abstract class FirClassifierSymbol<E> :
    AbstractFirBasedSymbol<E>(), TypeParameterMarker where E : FirNamedDeclaration, E : FirSymbolOwner<E> {
    abstract fun toLookupTag(): ConeClassifierLookupTag
}