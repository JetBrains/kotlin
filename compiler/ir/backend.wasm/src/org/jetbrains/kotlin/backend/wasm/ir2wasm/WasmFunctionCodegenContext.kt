/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.wasm.ir.*
import java.util.LinkedList

enum class LoopLabelType { BREAK, CONTINUE }
enum class SyntheticLocalType { IS_INTERFACE_PARAMETER, TABLE_SWITCH_SELECTOR }

class WasmFunctionCodegenContext(
    val irFunction: IrFunction?,
    private val wasmFunction: WasmFunction.Defined,
    private val backendContext: WasmBackendContext,
    private val wasmFileCodegenContext: WasmFileCodegenContext,
    private val wasmModuleTypeTransformer: WasmModuleTypeTransformer,
) {
    val bodyGen: WasmExpressionBuilder =
        WasmIrExpressionBuilder(wasmFunction.instructions)

    private val wasmLocals = LinkedHashMap<IrValueSymbol, WasmLocal>()
    private val wasmSyntheticLocals = LinkedHashMap<SyntheticLocalType, WasmLocal>()
    private val loopLevels = LinkedHashMap<Pair<IrLoop, LoopLabelType>, Int>()
    private val nonLocalReturnLevels = LinkedHashMap<IrReturnableBlockSymbol, Int>()
    private val inlinedFunctionStack = LinkedList<IrFunction>()

    fun defineLocal(irValueDeclaration: IrValueSymbol) {
        assert(irValueDeclaration !in wasmLocals) { "Redefinition of local" }

        val owner = irValueDeclaration.owner
        val wasmLocal = WasmLocal(
            wasmFunction.locals.size,
            owner.name.asString(),
            if (owner is IrValueParameter) wasmModuleTypeTransformer.transformValueParameterType(owner) else wasmModuleTypeTransformer.transformType(owner.type),
            isParameter = irValueDeclaration is IrValueParameterSymbol
        )

        wasmLocals[irValueDeclaration] = wasmLocal
        wasmFunction.locals += wasmLocal
    }

    fun defineTmpVariable(type: WasmType): Int {
        val wasmLocal = WasmLocal(wasmFunction.locals.size, "tmp", type, false)
        wasmFunction.locals += wasmLocal
        return wasmLocal.id
    }

    fun referenceLocal(irValueDeclaration: IrValueSymbol): WasmLocal {
        return wasmLocals.getValue(irValueDeclaration)
    }

    fun referenceLocal(index: Int): WasmLocal {
        return wasmFunction.locals[index]
    }

    private val SyntheticLocalType.wasmType
        get() = when (this) {
            SyntheticLocalType.IS_INTERFACE_PARAMETER ->
                WasmRefNullType(WasmHeapType.Type(wasmFileCodegenContext.referenceGcType(backendContext.irBuiltIns.anyClass)))
            SyntheticLocalType.TABLE_SWITCH_SELECTOR -> WasmI32
        }

    fun referenceLocal(type: SyntheticLocalType): WasmLocal = wasmSyntheticLocals.getOrPut(type) {
        WasmLocal(
            wasmFunction.locals.size,
            type.name,
            type.wasmType,
            isParameter = false
        ).also {
            wasmFunction.locals += it
        }
    }

    fun defineNonLocalReturnLevel(block: IrReturnableBlockSymbol, level: Int) {
        nonLocalReturnLevels[block] = level
    }

    fun referenceNonLocalReturnLevel(block: IrReturnableBlockSymbol): Int {
        return nonLocalReturnLevels.getValue(block)
    }

    fun defineLoopLevel(irLoop: IrLoop, labelType: LoopLabelType, level: Int) {
        val loopKey = Pair(irLoop, labelType)
        assert(loopKey !in loopLevels) { "Redefinition of loop" }
        loopLevels[loopKey] = level
    }

    fun referenceLoopLevel(irLoop: IrLoop, labelType: LoopLabelType): Int {
        return loopLevels.getValue(Pair(irLoop, labelType))
    }

    val currentFunction: IrFunction?
        get() = inlinedFunctionStack.firstOrNull() ?: irFunction

    fun stepIntoInlinedFunction(inlineFunction: IrFunction) {
        inlinedFunctionStack.push(inlineFunction)
    }

    fun stepOutLastInlinedFunction() {
        inlinedFunctionStack.pop()
    }
}