/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.wasm.ir.WasmExpressionBuilder
import org.jetbrains.kotlin.wasm.ir.WasmLocal

enum class LoopLabelType { BREAK, CONTINUE }
enum class SyntheticLocalType { IS_INTERFACE_PARAMETER, TABLE_SWITCH_SELECTOR }

interface WasmFunctionCodegenContext : WasmBaseCodegenContext {
    val irFunction: IrFunction

    fun defineLocal(irValueDeclaration: IrValueSymbol)
    fun referenceLocal(irValueDeclaration: IrValueSymbol): WasmLocal
    fun referenceLocal(index: Int): WasmLocal
    fun referenceLocal(type: SyntheticLocalType): WasmLocal

    fun defineNonLocalReturnLevel(block: IrReturnableBlockSymbol, level: Int)
    fun referenceNonLocalReturnLevel(block: IrReturnableBlockSymbol): Int

    fun defineLoopLevel(irLoop: IrLoop, labelType: LoopLabelType, level: Int)
    fun referenceLoopLevel(irLoop: IrLoop, labelType: LoopLabelType): Int

    // So far always a single tag
    val tagIdx: Int

    val bodyGen: WasmExpressionBuilder
}