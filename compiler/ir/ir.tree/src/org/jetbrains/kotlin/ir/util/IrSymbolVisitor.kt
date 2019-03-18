/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.symbols.*

interface IrSymbolVisitor<out R, in D> {
    fun visitSymbol(symbol: IrSymbol, data: D): R

    fun visitPackageFragmentSymbol(symbol: IrPackageFragmentSymbol, data: D) =
        visitSymbol(symbol, data)

    fun visitFileSymbol(symbol: IrFileSymbol, data: D) =
        visitPackageFragmentSymbol(symbol, data)

    fun visitExternalPackageFragmentSymbol(symbol: IrExternalPackageFragmentSymbol, data: D) =
        visitPackageFragmentSymbol(symbol, data)

    fun visitClassifierSymbol(symbol: IrClassifierSymbol, data: D) =
        visitSymbol(symbol, data)

    fun visitClassSymbol(symbol: IrClassSymbol, data: D) =
        visitClassifierSymbol(symbol, data)

    fun visitTypeParameterSymbol(symbol: IrTypeParameterSymbol, data: D) =
        visitClassifierSymbol(symbol, data)

    fun visitValueSymbol(symbol: IrValueSymbol, data: D) =
        visitSymbol(symbol, data)

    fun visitVariableSymbol(symbol: IrVariableSymbol, data: D) =
        visitValueSymbol(symbol, data)

    fun visitValueParameterSymbol(symbol: IrValueParameterSymbol, data: D) =
        visitValueSymbol(symbol, data)

    fun visitReturnTargetSymbol(symbol: IrReturnTargetSymbol, data: D) =
        visitSymbol(symbol, data)

    fun visitFunctionSymbol(symbol: IrFunctionSymbol, data: D) =
        visitReturnTargetSymbol(symbol, data)

    fun visitSimpleFunctionSymbol(symbol: IrSimpleFunctionSymbol, data: D) =
        visitFunctionSymbol(symbol, data)

    fun visitConstructorSymbol(symbol: IrConstructorSymbol, data: D) =
        visitFunctionSymbol(symbol, data)

    fun visitReturnableBlockSymbol(symbol: IrReturnableBlockSymbol, data: D) =
        visitReturnTargetSymbol(symbol, data)

    fun visitAnonymousInitializerSymbol(symbol: IrAnonymousInitializerSymbol, data: D) =
        visitSymbol(symbol, data)

    fun visitEnumEntrySymbol(symbol: IrEnumEntrySymbol, data: D) =
        visitSymbol(symbol, data)

    fun visitFieldSymbol(symbol: IrFieldSymbol, data: D) =
        visitSymbol(symbol, data)

    fun visitPropertySymbol(symbol: IrPropertySymbol, data: D) =
        visitSymbol(symbol, data)
}