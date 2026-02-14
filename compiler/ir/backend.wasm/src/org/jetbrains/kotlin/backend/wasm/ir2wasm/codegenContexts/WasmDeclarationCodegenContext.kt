/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.utils.redefinitionError
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.wasm.ir.WasmFunction
import org.jetbrains.kotlin.wasm.ir.WasmGlobal

open class WasmDeclarationCodegenContext(
    private val wasmFileFragment: WasmCompiledDeclarationsFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
) : WasmCodegenContext(idSignatureRetriever) {

    open fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction) {
        if (wasmFileFragment.definedFunctions.put(irFunction.getReferenceKey(), wasmFunction) != null) {
            redefinitionError(irFunction.getReferenceKey(), "Functions")
        }
    }

    open fun defineGlobalField(irField: IrFieldSymbol, wasmGlobal: WasmGlobal) {
        if (wasmFileFragment.definedGlobalFields.put(irField.getReferenceKey(), wasmGlobal) != null) {
            redefinitionError(irField.getReferenceKey(), "GlobalFields")
        }
    }

    open fun defineGlobalVTable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        if (wasmFileFragment.definedGlobalVTables.put(irClass.getReferenceKey(), wasmGlobal) != null) {
            redefinitionError(irClass.getReferenceKey(), "GlobalVTables")
        }
    }

    open fun defineGlobalClassITable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        if (wasmFileFragment.definedGlobalClassITables.put(irClass.getReferenceKey(), wasmGlobal) != null) {
            redefinitionError(irClass.getReferenceKey(), "GlobalClassITables")
        }
    }

    open fun defineRttiGlobal(global: WasmGlobal, irClass: IrClassSymbol, irSuperClass: IrClassSymbol?) {
        val reference = irClass.getReferenceKey()
        if (wasmFileFragment.definedRttiGlobal.put(reference, global) != null) {
            redefinitionError(reference, "RttiGlobal")
        }
        if (wasmFileFragment.definedRttiSuperType.put(reference, irSuperClass?.getReferenceKey()) != null) {
            redefinitionError(reference, "RttiSuperType")
        }
    }

    open fun referenceFunction(irFunction: IrFunctionSymbol): FuncSymbol =
        FuncSymbol(irFunction.getReferenceKey())

    open fun referenceGlobalField(irField: IrFieldSymbol): FieldGlobalSymbol =
        FieldGlobalSymbol(irField.getReferenceKey())

    open fun referenceGlobalVTable(irClass: IrClassSymbol): VTableGlobalSymbol =
        VTableGlobalSymbol(irClass.getReferenceKey())

    open fun referenceGlobalClassITable(irClass: IrClassSymbol): ClassITableGlobalSymbol =
        ClassITableGlobalSymbol(irClass.getReferenceKey())

    open fun referenceRttiGlobal(irClass: IrClassSymbol): RttiGlobalSymbol =
        RttiGlobalSymbol(irClass.getReferenceKey())
}