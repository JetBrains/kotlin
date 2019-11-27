/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

sealed class FirClassLikeSymbol<D>(
    val classId: ClassId
) : FirClassifierSymbol<D>() where D : FirClassLikeDeclaration<D>, D : FirSymbolOwner<D> {
    abstract override fun toLookupTag(): ConeClassLikeLookupTag

    override fun equals(other: Any?): Boolean =
        other is FirClassLikeSymbol<*> && fir == other.fir

    override fun hashCode(): Int = fir.hashCode()
}

sealed class FirClassSymbol<C : FirClass<C>>(classId: ClassId) : FirClassLikeSymbol<C>(classId) {
    private val lookupTag =
        if (classId.isLocal) ConeClassLookupTagWithFixedSymbol(classId, this)
        else ConeClassLikeLookupTagImpl(classId)

    override fun toLookupTag(): ConeClassLikeLookupTag = lookupTag
}

class FirRegularClassSymbol(classId: ClassId) : FirClassSymbol<FirRegularClass>(classId)

class FirAnonymousObjectSymbol : FirClassSymbol<FirAnonymousObject>(ClassId(FqName.ROOT, FqName("anonymous"), true))

class FirTypeAliasSymbol(classId: ClassId) : FirClassLikeSymbol<FirTypeAlias>(classId) {
    override fun toLookupTag() = ConeClassLikeLookupTagImpl(classId)
}
