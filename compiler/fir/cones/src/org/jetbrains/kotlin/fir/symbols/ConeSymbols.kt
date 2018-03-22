/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.name.ClassId

interface ConeSymbol

interface ConeTypeParameterSymbol : ConeSymbol

interface ConeClassLikeSymbol : ConeSymbol {
    val classId: ClassId
}

interface ConeTypeAliasSymbol : ConeClassLikeSymbol {
    val expansionType: ConeClassLikeType
}

interface ConeClassSymbol : ConeClassLikeSymbol {
    val superTypes: List<ConeClassLikeType>
}
