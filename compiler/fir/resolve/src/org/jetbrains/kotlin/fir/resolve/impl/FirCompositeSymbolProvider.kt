/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class FirCompositeSymbolProvider(val providers: List<FirSymbolProvider>) : FirSymbolProvider {
    override fun getCallableSymbols(callableId: CallableId): List<ConeCallableSymbol> {
        for (provider in providers) {
            val symbols = provider.getCallableSymbols(callableId)
            if (symbols.isNotEmpty()) return symbols
        }
        return emptyList()
    }

    override fun getPackage(fqName: FqName): FqName? {
        return providers.firstNotNullResult { it.getPackage(fqName) }
    }

    override fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol? {
        return providers.firstNotNullResult { it.getClassLikeSymbolByFqName(classId) }
    }
}