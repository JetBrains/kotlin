/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

interface ConeClassifierLookupTag {
    val name: Name
}

interface ConeClassifierLookupTagWithFixedSymbol {
    val symbol: ConeClassifierSymbol
}

interface ConeTypeParameterLookupTag : ConeClassifierLookupTag {
    override val name: Name

}
interface ConeClassLikeLookupTag : ConeClassifierLookupTag {
    val classId: ClassId

    override val name: Name
        get() = classId.shortClassName
}

interface ConeTypeAliasLookupTag : ConeClassLikeLookupTag

interface ConeClassLookupTag : ConeClassLikeLookupTag

class ConeClassLikeLookupTagImpl(override val classId: ClassId) : ConeClassLikeLookupTag
