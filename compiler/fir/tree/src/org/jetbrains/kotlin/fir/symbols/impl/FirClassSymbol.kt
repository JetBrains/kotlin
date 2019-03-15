/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.ConeClassSymbol
import org.jetbrains.kotlin.name.ClassId

class FirClassSymbol(override val classId: ClassId) : ConeClassSymbol, AbstractFirBasedSymbol<FirRegularClass>() {
    override fun toLookupTag(): ConeClassLikeLookupTag = ConeClassLikeLookupTagImpl(classId)

    override fun equals(other: Any?): Boolean =
        other is FirClassSymbol && classId == other.classId && fir == other.fir

    override fun hashCode(): Int {
        var result = 31
        result = result * 19 + classId.hashCode()
        result = result * 19 + fir.hashCode()
        return result
    }
}
