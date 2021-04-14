/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.name.Name

class FirTypeParameterSymbol : FirClassifierSymbol<FirTypeParameter>() {

    val name: Name
        get() = fir.name

    private val lookupTag = ConeTypeParameterLookupTag(this)

    override fun toLookupTag(): ConeTypeParameterLookupTag = lookupTag

    override fun toString(): String = "${this::class.simpleName} ${name.asString()}"
}

