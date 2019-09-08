/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirErrorFunction
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.ConePropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class FirFunctionSymbol<D : FirFunction<D>>(
    override val callableId: CallableId
) : ConeFunctionSymbol, FirCallableSymbol<D>() {
    override val parameters: List<ConeKotlinType>
        get() = emptyList()
}

// ------------------------ named ------------------------

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

// ------------------------ unnamed ------------------------

sealed class FirFunctionWithoutNameSymbol<F : FirFunction<F>>(
    stubName: Name
) : FirFunctionSymbol<F>(CallableId(FqName("special"), stubName)) {
    override val parameters: List<ConeKotlinType>
        get() = emptyList()
}

class FirAnonymousFunctionSymbol : FirFunctionWithoutNameSymbol<FirAnonymousFunction>(Name.identifier("anonymous"))

class FirPropertyAccessorSymbol : FirFunctionWithoutNameSymbol<FirPropertyAccessor>(Name.identifier("accessor"))

class FirErrorFunctionSymbol : FirFunctionWithoutNameSymbol<FirErrorFunction>(Name.identifier("error"))