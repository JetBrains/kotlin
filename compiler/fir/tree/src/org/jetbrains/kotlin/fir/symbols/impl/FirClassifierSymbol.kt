/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.fir.types.ConeClassifierLookupTag
import org.jetbrains.kotlin.mpp.ClassifierSymbolMarker

sealed class FirClassifierSymbol<out E : FirDeclaration>(
    symbolId: FirSymbolId<FirClassifierSymbol<E>>,
) : FirThisOwnerSymbol<E>(symbolId), ClassifierSymbolMarker {
    abstract override val symbolId: FirSymbolId<FirClassifierSymbol<E>>

    abstract fun toLookupTag(): ConeClassifierLookupTag
}
