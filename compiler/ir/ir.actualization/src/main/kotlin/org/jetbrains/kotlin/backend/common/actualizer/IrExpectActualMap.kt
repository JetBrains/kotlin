/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol

class IrExpectActualMap {
    val regularSymbols: MutableMap<IrSymbol, IrSymbol> = mutableMapOf()
    val propertyAccessorsActualizedByFields: MutableMap<IrSimpleFunctionSymbol, IrPropertySymbol> = mutableMapOf()
}
