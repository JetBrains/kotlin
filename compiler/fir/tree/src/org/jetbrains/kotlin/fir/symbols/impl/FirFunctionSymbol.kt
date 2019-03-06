/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

class FirFunctionSymbol(
    override val callableId: CallableId,
    val isFakeOverride: Boolean = false
) : ConeFunctionSymbol, AbstractFirBasedSymbol<FirCallableDeclaration>() {
    override val parameters: List<ConeKotlinType>
        get() = emptyList()
}