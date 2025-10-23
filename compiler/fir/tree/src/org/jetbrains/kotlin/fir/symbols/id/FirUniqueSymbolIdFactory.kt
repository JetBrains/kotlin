/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.id

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * TODO (marco): Document.
 */
object FirUniqueSymbolIdFactory : FirSymbolIdFactory() {
    override fun <E : FirDeclaration, S : FirBasedSymbol<E>> unique(): FirSymbolId<S> = FirUniqueSymbolId()

    override fun <E : FirDeclaration, S : FirBasedSymbol<E>> sourceBased(sourceElement: KtSourceElement): FirSymbolId<S> =
        FirUniqueSymbolId()
}
