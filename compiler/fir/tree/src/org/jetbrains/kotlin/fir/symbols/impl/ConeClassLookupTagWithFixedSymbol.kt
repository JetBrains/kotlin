/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.name.ClassId

data class ConeClassLookupTagWithFixedSymbol(
    override val classId: ClassId,
    val symbol: FirClassSymbol<*>
) : ConeClassLikeLookupTag() {
}