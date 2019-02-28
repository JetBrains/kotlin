/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.name.ClassId


val FirSymbolProvider.Any: ConeClassLikeSymbol get() = this.getClassLikeSymbolByFqName(StandardClassIds.Any)!!
val FirSymbolProvider.Nothing: ConeClassLikeSymbol get() = this.getClassLikeSymbolByFqName(StandardClassIds.Nothing)!!


operator fun ClassId.invoke(symbolProvider: FirSymbolProvider): ConeClassLikeSymbol {
    return symbolProvider.getClassLikeSymbolByFqName(this)!!
}