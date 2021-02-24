/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.wasm.ir.*

class WasmFunctionCodegenContextImpl(
    override val irFunction: IrFunction,
    private val wasmFunction: WasmFunction.Defined,
    override val backendContext: WasmBackendContext,
    private val referencing: WasmBaseCodegenContext
) : WasmBaseCodegenContext by referencing,
    WasmFunctionCodegenContext {
    override val bodyGen: WasmExpressionBuilder =
        WasmIrExpressionBuilder(wasmFunction.instructions)

    private val wasmLocals = LinkedHashMap<IrValueSymbol, WasmLocal>()
    private val loopLevels = LinkedHashMap<Pair<IrLoop, LoopLabelType>, Int>()

    override fun defineLocal(irValueDeclaration: IrValueSymbol) {
        assert(irValueDeclaration !in wasmLocals) { "Redefinition of local" }

        val owner = irValueDeclaration.owner
        val wasmLocal = WasmLocal(
            wasmFunction.locals.size,
            owner.name.asString(),
            if (owner is IrValueParameter) transformValueParameterType(owner) else transformType(owner.type),
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

    override fun defineLoopLevel(irLoop: IrLoop, labelType: LoopLabelType, level: Int) {
        val loopKey = Pair(irLoop, labelType)
        assert(loopKey !in loopLevels) { "Redefinition of loop" }
        loopLevels[loopKey] = level
    }

    override fun referenceLoopLevel(irLoop: IrLoop, labelType: LoopLabelType): Int {
        return loopLevels.getValue(Pair(irLoop, labelType))
    }
}