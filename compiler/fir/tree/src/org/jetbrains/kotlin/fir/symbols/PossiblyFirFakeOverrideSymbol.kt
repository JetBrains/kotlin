/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.FirDeclaration

interface PossiblyFirFakeOverrideSymbol<E, S : FirBasedSymbol<E>> : FirBasedSymbol<E> where E : FirSymbolOwner<E>, E : FirDeclaration {
    val overriddenSymbol: S?
}
