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
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class IrExpectActualMap() {
    val expectToActual: Map<IrSymbol, IrSymbol> get() = _expectToActual
    private val _expectToActual: MutableMap<IrSymbol, IrSymbol> = mutableMapOf()

    /**
     * Direct means "not through typealias".
     * ClassId of expect and actual symbols are the same.
     * For every actual, it's possible to have multiple expects (because of `actual typealias`).
     * But only a single "direct" expect is possible.
     */
    val actualToDirectExpect: Map<IrSymbol, IrSymbol> get() = _actualToDirectExpect
    private val _actualToDirectExpect: MutableMap<IrSymbol, IrSymbol> = mutableMapOf()

    val propertyAccessorsActualizedByFields: MutableMap<IrSimpleFunctionSymbol, IrPropertySymbol> = mutableMapOf()

    fun putRegular(expectSymbol: IrSymbol, actualSymbol: IrSymbol): IrSymbol? {
        val registeredActual = _expectToActual.put(expectSymbol, actualSymbol)
        val expect = expectSymbol.owner
        val actual = actualSymbol.owner
        if (expect is IrDeclaration && actual is IrDeclaration &&
            expect.parentsWithSelf.firstIsInstanceOrNull<IrClass>()?.classId ==
            actual.parentsWithSelf.firstIsInstanceOrNull<IrClass>()?.classId
        ) _actualToDirectExpect.put(actualSymbol, expectSymbol)
        return registeredActual
    }
}
