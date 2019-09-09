/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.*

class WasmFunctionCodegenContextImpl(
    override val currentIrFunction: IrFunction,
    override val backendContext: WasmBackendContext,
    private val referencing: WasmBaseCodegenContext,
    private val wasmFunction: WasmDefinedFunction
): WasmBaseCodegenContext by referencing, WasmFunctionCodegenContext {
    private var labelIds = 0
    override fun getNextLabelId(): Int = labelIds++

    override fun addInstruction(wasmInstruction: WasmInstruction) {
        currentIrFunction.body
    }

    private val wasmLocals = LinkedHashMap<IrValueSymbol, WasmLocal>()
    private val loopLabels = LinkedHashMap<Pair<IrLoop, LoopLabelType>, WasmBranchTarget>()

    private var localIds: Int = 0
    override fun defineLocal(irValueDeclaration: IrValueSymbol) {
        assert(irValueDeclaration !in wasmLocals) { "Redefinition of local" }

        val owner = irValueDeclaration.owner
        val wasmLocal = WasmLocal(
            localIds++,
            owner.name.asString(),
            transformType(owner.type),
            isParameter = irValueDeclaration is IrValueParameterSymbol
        )

        wasmLocals[irValueDeclaration] = wasmLocal
        wasmFunction.locals += wasmLocal
    }

    override fun referenceLocal(irValueDeclaration: IrValueSymbol): WasmLocal {
        return wasmLocals.getValue(irValueDeclaration)
    }

    override fun referenceLocal(index: Int): WasmLocal {
        return wasmFunction.locals[index]
    }

    override fun defineLoopLabel(irLoop: IrLoop, labelType: LoopLabelType, target: WasmBranchTarget) {
        val loopKey = Pair(irLoop, labelType)
        assert(loopKey !in loopLabels) { "Redefinition of loop" }
        loopLabels[loopKey] = target
    }

    override fun referenceLoopLabel(irLoop: IrLoop, labelType: LoopLabelType): WasmBranchTarget {
        return loopLabels.getValue(Pair(irLoop, labelType))
    }
}