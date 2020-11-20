/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.isInner
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType

class FirOuterClassManager(
    private val session: FirSession,
    private val outerLocalClassForNested: Map<FirClassLikeSymbol<*>, FirClassLikeSymbol<*>>,
) {
    private val symbolProvider = session.firSymbolProvider

    fun outerClass(classSymbol: FirClassLikeSymbol<*>): FirClassLikeSymbol<*>? {
        if (classSymbol !is FirClassSymbol<*>) return null
        val classId = classSymbol.classId
        if (classId.isLocal) return outerLocalClassForNested[classSymbol]
        val outerClassId = classId.outerClassId ?: return null
        return symbolProvider.getClassLikeSymbolByFqName(outerClassId)
    }

    fun outerType(classLikeType: ConeClassLikeType): ConeClassLikeType? {
        val fullyExpandedType = classLikeType.fullyExpandedType(session)

        val symbol = fullyExpandedType.lookupTag.toSymbol(session) ?: return null

        if (symbol is FirRegularClassSymbol && !symbol.fir.isInner) return null

        val containingSymbol = outerClass(symbol) ?: return null
        val currentTypeArgumentsNumber = (symbol as? FirRegularClassSymbol)?.fir?.typeParameters?.count { it is FirTypeParameter } ?: 0

        return containingSymbol.constructType(
            fullyExpandedType.typeArguments.drop(currentTypeArgumentsNumber).toTypedArray(),
            isNullable = false
        ) as ConeClassLikeType
    }
}
