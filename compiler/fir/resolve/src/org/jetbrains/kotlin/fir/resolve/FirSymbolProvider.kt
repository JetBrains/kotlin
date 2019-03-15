/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.toFirClassLike
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

interface FirSymbolProvider {

    fun getClassLikeSymbolByFqName(classId: ClassId): ConeClassLikeSymbol?

    fun getSymbolByLookupTag(lookupTag: ConeClassifierLookupTag): ConeClassifierSymbol? {
        return when (lookupTag) {
            is ConeClassLikeLookupTag -> getClassLikeSymbolByFqName(lookupTag.classId)
            is ConeClassifierLookupTagWithFixedSymbol -> lookupTag.symbol
            else -> error("Unknown lookupTag type: ${lookupTag::class}")
        }
    }

    fun getCallableSymbols(callableId: CallableId): List<ConeCallableSymbol>

    fun getPackage(fqName: FqName): FqName? // TODO: Replace to symbol sometime

    // TODO: should not retrieve session through the FirElement::session
    fun getSessionForClass(classId: ClassId): FirSession? = getClassLikeSymbolByFqName(classId)?.toFirClassLike()?.session

    companion object {
        fun getInstance(session: FirSession) = session.service<FirSymbolProvider>()
    }
}
