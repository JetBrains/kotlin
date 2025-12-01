/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.wasm.ir.WasmExport
import org.jetbrains.kotlin.wasm.ir.WasmFunction
import org.jetbrains.kotlin.wasm.ir.WasmGlobal
import org.jetbrains.kotlin.wasm.ir.WasmHeapType
import org.jetbrains.kotlin.wasm.ir.WasmImmediate

class WasmFileCodegenContextWithExport(
    wasmFileFragment: WasmCompiledFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
    private val referencedDeclarationsCollector: MutableSet<IdSignature>,
) : WasmFileCodegenContext(wasmFileFragment, idSignatureRetriever) {
    override fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction) {
        super.defineFunction(irFunction, wasmFunction)
        val owner = irFunction.owner
        if (owner.isEffectivelyPrivate()) return
        val signature = idSignatureRetriever.declarationSignature(owner)
        addExport(
            WasmExport.Function(
                field = wasmFunction,
                name = "${WasmServiceImportExportKind.FUNC.prefix}$signature"
            )
        )
    }

    private fun addSignatureToCollector(declaration: IrDeclaration) {
        referencedDeclarationsCollector.add(
            idSignatureRetriever.declarationSignature(declaration)!!
        )
    }

    override fun referenceFunction(irFunction: IrFunctionSymbol): WasmImmediate.FuncIdx {
        addSignatureToCollector(irFunction.owner)
        return super.referenceFunction(irFunction)
    }

    override fun referenceGlobalField(irField: IrFieldSymbol): WasmImmediate.GlobalIdx.FieldIdx {
        addSignatureToCollector(irField.owner)
        return super.referenceGlobalField(irField)
    }

    override fun referenceGlobalVTable(irClass: IrClassSymbol): WasmImmediate.GlobalIdx.VTableIdx {
        addSignatureToCollector(irClass.owner)
        return super.referenceGlobalVTable(irClass)
    }

    override fun referenceGlobalClassITable(irClass: IrClassSymbol): WasmImmediate.GlobalIdx.ClassITableIdx {
        addSignatureToCollector(irClass.owner)
        return super.referenceGlobalClassITable(irClass)
    }

    override fun referenceRttiGlobal(irClass: IrClassSymbol): WasmImmediate.GlobalIdx.RttiIdx {
        addSignatureToCollector(irClass.owner)
        return super.referenceRttiGlobal(irClass)
    }

    override fun referenceGcType(irClass: IrClassSymbol): WasmImmediate.TypeIdx.GcTypeIdx {
        addSignatureToCollector(irClass.owner)
        return super.referenceGcType(irClass)
    }

    override fun referenceHeapType(irClass: IrClassSymbol): WasmHeapType.Type.GcType {
        addSignatureToCollector(irClass.owner)
        return super.referenceHeapType(irClass)
    }

    override fun referenceVTableGcType(irClass: IrClassSymbol): WasmImmediate.TypeIdx.VTableTypeIdx {
        addSignatureToCollector(irClass.owner)
        return super.referenceVTableGcType(irClass)
    }

    override fun referenceVTableHeapType(irClass: IrClassSymbol): WasmHeapType.Type.VTableType {
        addSignatureToCollector(irClass.owner)
        return super.referenceVTableHeapType(irClass)
    }

    override fun referenceFunctionType(irClass: IrFunctionSymbol): WasmImmediate.TypeIdx.FunctionTypeIdx {
        addSignatureToCollector(irClass.owner)
        return super.referenceFunctionType(irClass)
    }

    override fun referenceFunctionHeapType(irClass: IrFunctionSymbol): WasmHeapType.Type.FunctionType {
        addSignatureToCollector(irClass.owner)
        return super.referenceFunctionHeapType(irClass)
    }

    override fun defineGlobalVTable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        super.defineGlobalVTable(irClass, wasmGlobal)
        exportDeclarationGlobal(irClass.owner, WasmServiceImportExportKind.VTABLE, wasmGlobal)
    }

    override fun defineGlobalClassITable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        super.defineGlobalClassITable(irClass, wasmGlobal)
        exportDeclarationGlobal(irClass.owner, WasmServiceImportExportKind.ITABLE, wasmGlobal)
    }

    override fun defineRttiGlobal(global: WasmGlobal, irClass: IrClassSymbol, irSuperClass: IrClassSymbol?) {
        super.defineRttiGlobal(global, irClass, irSuperClass)
        exportDeclarationGlobal(irClass.owner, WasmServiceImportExportKind.RTTI, global)
    }

    private fun exportDeclarationGlobal(declaration: IrDeclarationWithVisibility, prefix: WasmServiceImportExportKind, global: WasmGlobal) {
        if (declaration.isEffectivelyPrivate()) return
        val signature = idSignatureRetriever.declarationSignature(declaration)
        addExport(
            WasmExport.Global(
                name = "${prefix.prefix}$signature",
                field = global
            )
        )
    }
}