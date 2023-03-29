/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId

fun FirProvider.getContainingFile(symbol: FirBasedSymbol<*>): FirFile? = when (symbol) {
    is FirSyntheticFunctionSymbol -> {
        // SAM case
        val classId = ClassId(symbol.callableId.packageName, symbol.callableId.callableName)
        getFirClassifierContainerFile(classId)
    }
    is FirClassLikeSymbol<*> -> getFirClassifierContainerFileIfAny(symbol)
    is FirCallableSymbol<*> -> getFirCallableContainerFile(symbol)
    else -> null
}
