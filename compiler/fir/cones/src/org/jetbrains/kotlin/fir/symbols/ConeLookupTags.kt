/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

interface ConeClassifierLookupTag

interface ConeClassifierLookupTagWithFixedSymbol {
    val symbol: ConeClassifierSymbol
}

interface ConeTypeParameterLookupTag : ConeClassifierLookupTag {
    val name: Name

}
interface ConeClassLikeLookupTag : ConeClassifierLookupTag {
    val classId: ClassId
}

interface ConeTypeAliasLookupTag : ConeClassLikeLookupTag

interface ConeClassLookupTag : ConeClassLikeLookupTag

class ConeClassLikeLookupTagImpl(override val classId: ClassId) : ConeClassLikeLookupTag
