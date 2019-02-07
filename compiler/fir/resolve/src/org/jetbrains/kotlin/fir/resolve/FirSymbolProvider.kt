/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

interface FirSymbolProvider {

    fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol?

    fun getCallableSymbols(callableId: CallableId): List<ConeCallableSymbol>

    fun getPackage(fqName: FqName): FqName? // TODO: Replace to symbol sometime

    companion object {
        fun getInstance(session: FirSession) = session.service<FirSymbolProvider>()
    }
}