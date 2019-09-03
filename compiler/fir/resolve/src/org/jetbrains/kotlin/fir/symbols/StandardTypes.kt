/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId


val FirSymbolProvider.Any: FirClassLikeSymbol<*> get() = this.getClassLikeSymbolByFqName(StandardClassIds.Any)!!
val FirSymbolProvider.Nothing: FirClassLikeSymbol<*> get() = this.getClassLikeSymbolByFqName(StandardClassIds.Nothing)!!


operator fun ClassId.invoke(symbolProvider: FirSymbolProvider): FirClassLikeSymbol<*> {
    return symbolProvider.getClassLikeSymbolByFqName(this)!!
}