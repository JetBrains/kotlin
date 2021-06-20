/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class FirClassLikeSymbol<D : FirClassLikeDeclaration<D>>(
    val classId: ClassId
) : FirClassifierSymbol<D>() {
    abstract override fun toLookupTag(): ConeClassLikeLookupTag

    override fun toString(): String = "${this::class.simpleName} ${classId.asString()}"
}

sealed class FirClassSymbol<C : FirClass<C>>(classId: ClassId) : FirClassLikeSymbol<C>(classId) {
    private val lookupTag =
        if (classId.isLocal) ConeClassLookupTagWithFixedSymbol(classId, this)
        else ConeClassLikeLookupTagImpl(classId)

    override fun toLookupTag(): ConeClassLikeLookupTag = lookupTag
}

class FirRegularClassSymbol(classId: ClassId) : FirClassSymbol<FirRegularClass>(classId)

val ANONYMOUS_CLASS_ID = ClassId(FqName.ROOT, FqName.topLevel(Name.special("<anonymous>")), true)

class FirAnonymousObjectSymbol : FirClassSymbol<FirAnonymousObject>(ANONYMOUS_CLASS_ID)

class FirTypeAliasSymbol(classId: ClassId) : FirClassLikeSymbol<FirTypeAlias>(classId) {
    override fun toLookupTag() = ConeClassLikeLookupTagImpl(classId)
}
