/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

interface ConeSymbol

interface ConeTypeParameterSymbol : ConeSymbol {
    val name: Name
}

interface ConeClassLikeSymbol : ConeSymbol {
    val classId: ClassId
}

interface ConeTypeAliasSymbol : ConeClassLikeSymbol

interface ConeClassSymbol : ConeClassLikeSymbol
