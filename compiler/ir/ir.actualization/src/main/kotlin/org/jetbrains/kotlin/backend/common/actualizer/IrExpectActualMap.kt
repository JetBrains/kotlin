/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.parentsWithSelf

class IrExpectActualMap() {
    private val _expectToActual: MutableMap<IrSymbol, IrSymbol> = mutableMapOf()
    val expectToActual: Map<IrSymbol, IrSymbol> get() = _expectToActual

    private val _actualToDirectExpect: MutableMap<IrSymbol, IrSymbol> = mutableMapOf()
    val actualToDirectExpect: Map<IrSymbol, IrSymbol> get() = _actualToDirectExpect

    val propertyAccessorsActualizedByFields: MutableMap<IrSimpleFunctionSymbol, IrPropertySymbol> = mutableMapOf()

    fun putRegular(expectSymbol: IrSymbol, actualSymbol: IrSymbol): IrSymbol? {
        val registeredActual = _expectToActual.put(expectSymbol, actualSymbol)
        val expect = expectSymbol.owner
        val actual = actualSymbol.owner
        if (expect is IrDeclaration && actual is IrDeclaration &&
            expect.parentsWithSelf.filterIsInstance<IrClass>().firstOrNull()?.classId ==
            actual.parentsWithSelf.filterIsInstance<IrClass>().firstOrNull()?.classId
        ) _actualToDirectExpect.put(actualSymbol, expectSymbol)
        return registeredActual
    }
}
