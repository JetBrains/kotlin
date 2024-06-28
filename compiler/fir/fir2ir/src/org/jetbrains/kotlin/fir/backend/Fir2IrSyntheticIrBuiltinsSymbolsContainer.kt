/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.PrimitiveType.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl

class Fir2IrSyntheticIrBuiltinsSymbolsContainer {
    private val primitiveIntegralTypes: List<PrimitiveType> = listOf(BYTE, SHORT, INT, LONG)
    val primitiveFloatingPointTypes: List<PrimitiveType> = listOf(FLOAT, DOUBLE)
    private val primitiveNumericIrTypes: List<PrimitiveType> = primitiveIntegralTypes + primitiveFloatingPointTypes
    val primitiveIrTypesWithComparisons: List<PrimitiveType> = primitiveNumericIrTypes + CHAR

    val eqeqeqSymbol: IrSimpleFunctionSymbol = symbol()
    val eqeqSymbol: IrSimpleFunctionSymbol = symbol()
    val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol = symbol()
    val checkNotNullSymbol: IrSimpleFunctionSymbol = symbol()

    // ------------------------- primitive types symbol maps -------------------------

    val lessFunByOperandType: Map<PrimitiveType, IrSimpleFunctionSymbol> = primitiveIrTypesWithComparisons.symbols()
    val lessOrEqualFunByOperandType: Map<PrimitiveType, IrSimpleFunctionSymbol> = primitiveIrTypesWithComparisons.symbols()
    val greaterOrEqualFunByOperandType: Map<PrimitiveType, IrSimpleFunctionSymbol> = primitiveIrTypesWithComparisons.symbols()
    val greaterFunByOperandType: Map<PrimitiveType, IrSimpleFunctionSymbol> = primitiveIrTypesWithComparisons.symbols()

    val ieee754equalsFunByOperandType: Map<PrimitiveType, IrSimpleFunctionSymbol> = primitiveFloatingPointTypes.symbols()

    // ------------------------- utilities -------------------------

    private fun List<PrimitiveType>.symbols(): Map<PrimitiveType, IrSimpleFunctionSymbol> = associateWith { symbol() }
    private fun symbol(): IrSimpleFunctionSymbol = IrSimpleFunctionSymbolImpl()
}
