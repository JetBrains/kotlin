/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType

class FirAnonymousInitializerSymbol : FirBasedSymbol<FirAnonymousInitializer>() {
    override fun toString(): String = "${this::class.simpleName} <init>"

    val dispatchReceiverType: ConeSimpleKotlinType?
        get() = fir.dispatchReceiverType
}
