/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.AccessorSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class FirFunctionSymbol<D : FirFunction>(
    override val callableId: CallableId
) : FirCallableSymbol<D>()

// ------------------------ named ------------------------

open class FirNamedFunctionSymbol(
    callableId: CallableId,
) : FirFunctionSymbol<FirSimpleFunction>(callableId)

interface FirIntersectionCallableSymbol {
    val intersections: Collection<FirCallableSymbol<*>>
}

class FirIntersectionOverrideFunctionSymbol(
    callableId: CallableId,
    override val intersections: Collection<FirCallableSymbol<*>>
) : FirNamedFunctionSymbol(callableId), FirIntersectionCallableSymbol

class FirConstructorSymbol(
    callableId: CallableId
) : FirFunctionSymbol<FirConstructor>(callableId)

open class FirAccessorSymbol(
    callableId: CallableId,
    override val accessorId: CallableId
) : FirPropertySymbol(callableId), AccessorSymbol

// ------------------------ unnamed ------------------------

sealed class FirFunctionWithoutNameSymbol<F : FirFunction>(
    stubName: Name
) : FirFunctionSymbol<F>(CallableId(FqName("special"), stubName))

class FirAnonymousFunctionSymbol : FirFunctionWithoutNameSymbol<FirAnonymousFunction>(Name.identifier("anonymous"))

class FirPropertyAccessorSymbol : FirFunctionWithoutNameSymbol<FirPropertyAccessor>(Name.identifier("accessor"))

class FirErrorFunctionSymbol : FirFunctionWithoutNameSymbol<FirErrorFunction>(Name.identifier("error"))
