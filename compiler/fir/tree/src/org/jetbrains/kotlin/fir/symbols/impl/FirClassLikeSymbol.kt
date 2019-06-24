/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.name.ClassId

sealed class FirClassLikeSymbol<D : FirClassLikeDeclaration<D>>(
    override val classId: ClassId
) : ConeClassLikeSymbol, AbstractFirBasedSymbol<D>() {
    override fun equals(other: Any?): Boolean =
        other is FirClassLikeSymbol<*> && fir == other.fir

    override fun hashCode(): Int = fir.hashCode()
}

class FirClassSymbol(classId: ClassId) : FirClassLikeSymbol<FirRegularClass>(classId), ConeClassSymbol {
    override fun toLookupTag(): ConeClassLikeLookupTag = ConeClassLikeLookupTagImpl(classId)
}

class FirTypeAliasSymbol(override val classId: ClassId) : FirClassLikeSymbol<FirTypeAlias>(classId), ConeTypeAliasSymbol {
    override fun toLookupTag(): TypeAliasLookupTagImpl = TypeAliasLookupTagImpl(classId)
}

class TypeAliasLookupTagImpl(
    override val classId: ClassId
) : ConeTypeAliasLookupTag()
