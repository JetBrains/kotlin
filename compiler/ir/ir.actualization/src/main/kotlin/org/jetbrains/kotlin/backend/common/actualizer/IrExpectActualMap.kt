/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol

class IrExpectActualMap() {
    private val _expectToActual: MutableMap<IrSymbol, IrSymbol> = mutableMapOf()
    val expectToActual: Map<IrSymbol, IrSymbol> get() = _expectToActual

    private val _actualToDirectExpect: MutableMap<IrSymbol, IrSymbol> = mutableMapOf()
    val actualToDirectExpect: Map<IrSymbol, IrSymbol> get() = _actualToDirectExpect

    val propertyAccessorsActualizedByFields: MutableMap<IrSimpleFunctionSymbol, IrPropertySymbol> = mutableMapOf()

    fun putRegular(expect: IrSymbol, actual: IrSymbol, direct: Boolean): IrSymbol? {
        val registeredActual = _expectToActual.put(expect, actual)
        if (direct) _actualToDirectExpect.put(actual, expect)
        return registeredActual
    }
}
