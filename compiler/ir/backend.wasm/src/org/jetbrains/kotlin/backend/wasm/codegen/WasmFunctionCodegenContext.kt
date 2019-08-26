/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.*

interface WasmFunctionCodegenContext : WasmBaseCodegenContext {
    val currentIrFunction: IrFunction

    fun defineLocal(irValueDeclaration: IrValueSymbol)
    fun referenceLocal(irValueDeclaration: IrValueSymbol): WasmLocal
    fun referenceLocal(index: Int): WasmLocal

    fun getNextLabelId(): Int

    fun defineLoopLabel(irLoop: IrLoop, labelType: LoopLabelType, target: WasmBranchTarget)
    fun referenceLoopLabel(irLoop: IrLoop, labelType: LoopLabelType): WasmBranchTarget

    fun addInstruction(wasmInstruction: WasmInstruction)
}