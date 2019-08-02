/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConePropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

sealed class FirFunctionSymbol<D : FirMemberFunction<D>>(
    override val callableId: CallableId
) : ConeFunctionSymbol, FirCallableSymbol<D>() {
    override val parameters: List<ConeKotlinType>
        get() = emptyList()
}

open class FirNamedFunctionSymbol(
    callableId: CallableId,
    val isFakeOverride: Boolean = false,
    // Actual for fake override only
    val overriddenSymbol: FirNamedFunctionSymbol? = null
) : FirFunctionSymbol<FirNamedFunction>(callableId)

class FirConstructorSymbol(
    callableId: CallableId
) : FirFunctionSymbol<FirConstructor>(callableId)

class FirAccessorSymbol(
    callableId: CallableId,
    val accessorId: CallableId
) : ConePropertySymbol, FirFunctionSymbol<FirNamedFunction>(callableId)