/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeAliasLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeAliasSymbol
import org.jetbrains.kotlin.name.ClassId

class FirTypeAliasSymbol(
    override val classId: ClassId
) : ConeTypeAliasSymbol, AbstractFirBasedSymbol<FirTypeAlias>() {
    override fun toLookupTag(): ConeClassLikeLookupTag = TypeAliasLookupTagImpl(classId)

    override fun equals(other: Any?): Boolean =
        other is FirTypeAliasSymbol && classId == other.classId && fir == other.fir

    override fun hashCode(): Int {
        var result = 31
        result = result * 19 + classId.hashCode()
        result = result * 19 + fir.hashCode()
        return result
    }
}

class TypeAliasLookupTagImpl(
    override val classId: ClassId
) : ConeTypeAliasLookupTag
