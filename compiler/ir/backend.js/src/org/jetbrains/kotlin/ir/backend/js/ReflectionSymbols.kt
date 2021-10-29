/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType

interface ReflectionSymbols {
    val getKClassFromExpression: IrSimpleFunctionSymbol
    val getKClass: IrSimpleFunctionSymbol
    val getClassData: IrSimpleFunctionSymbol
    val createKType: IrSimpleFunctionSymbol?
    val createDynamicKType: IrSimpleFunctionSymbol?
    val createKTypeParameter: IrSimpleFunctionSymbol?
    val getStarKTypeProjection: IrSimpleFunctionSymbol?
    val createCovariantKTypeProjection: IrSimpleFunctionSymbol?
    val createInvariantKTypeProjection: IrSimpleFunctionSymbol?
    val createContravariantKTypeProjection: IrSimpleFunctionSymbol?
    val primitiveClassesObject: IrClassSymbol
    val kTypeClass: IrClassSymbol
}