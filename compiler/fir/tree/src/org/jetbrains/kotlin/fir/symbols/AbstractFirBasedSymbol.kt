/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.FirElement

abstract class AbstractFirBasedSymbol<E> : FirBasedSymbol<E> where E : FirElement, E : FirSymbolOwner<E> {

    override lateinit var fir: E

    override fun bind(e: E) {
        fir = e
    }
}