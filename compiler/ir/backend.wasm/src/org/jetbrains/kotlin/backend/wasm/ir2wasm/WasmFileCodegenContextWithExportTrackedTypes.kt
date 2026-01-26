/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.erasedUpperBound
import org.jetbrains.kotlin.ir.util.getAllSuperclasses

class ModuleReferencedTypes(private val idSignatureRetriever: IdSignatureRetriever) {
    val referencedGcTypes = mutableSetOf<IdSignature>()
    val referencedFunctionTypes = mutableSetOf<IdSignature>()

    fun addGcTypeToReferenced(irClass: IrClassSymbol) {
        val irClassOwner = irClass.owner
        val signature = idSignatureRetriever.declarationSignature(irClassOwner)!!
        if (!referencedGcTypes.add(signature)) return

        irClassOwner.declarations.forEach {
            when (it) {
                is IrFunction -> {
                    addFunctionTypeToReferenced(it.symbol)
                }
                is IrField -> {
                    addGcTypeToReferenced(it.type.erasedUpperBound.symbol)
                }
                is IrProperty -> {}
                is IrClass -> {}
                is IrTypeAlias -> {}
                else -> {
                    error("Unexpected symbol type ${it.symbol}")
                }
            }
        }

        irClassOwner.getAllSuperclasses().forEach { addGcTypeToReferenced(it.symbol) }
    }

    fun addFunctionTypeToReferenced(irClass: IrFunctionSymbol) {
        val irFunctionOwner = irClass.owner
        val signature = idSignatureRetriever.declarationSignature(irFunctionOwner)!!
        if (!referencedFunctionTypes.add(signature)) return

        irFunctionOwner.parameters.forEach { p ->
            p.type.erasedUpperBound.symbol.let(::addGcTypeToReferenced)
        }
        addGcTypeToReferenced(irFunctionOwner.returnType.erasedUpperBound.symbol)
    }
}

class WasmFileCodegenContextWithExportTrackedTypes(
    wasmFileFragment: WasmCompiledFileFragment,
    idSignatureRetriever: IdSignatureRetriever,
    moduleReferencedDeclarations: ModuleReferencedDeclarations,
    private val moduleReferencedTypes: ModuleReferencedTypes,
) : WasmFileCodegenContextWithExport(wasmFileFragment, idSignatureRetriever, moduleReferencedDeclarations) {

    override fun referenceFunction(irFunction: IrFunctionSymbol): FuncSymbol {
        referenceFunctionType(irFunction)
        return super.referenceFunction(irFunction)
    }

    override fun referenceGlobalVTable(irClass: IrClassSymbol): VTableGlobalSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass)
        return super.referenceGlobalVTable(irClass)
    }

    override fun referenceGlobalClassITable(irClass: IrClassSymbol): ClassITableGlobalSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass)
        return super.referenceGlobalClassITable(irClass)
    }

    override fun referenceRttiGlobal(irClass: IrClassSymbol): RttiGlobalSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass)
        return super.referenceRttiGlobal(irClass)
    }

    override fun referenceGcType(irClass: IrClassSymbol): GcTypeSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass)
        return super.referenceGcType(irClass)
    }

    override fun referenceHeapType(irClass: IrClassSymbol): GcHeapTypeSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass)
        return super.referenceHeapType(irClass)
    }

    override fun referenceVTableGcType(irClass: IrClassSymbol): VTableTypeSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass)
        return super.referenceVTableGcType(irClass)
    }

    override fun referenceVTableHeapType(irClass: IrClassSymbol): VTableHeapTypeSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass)
        return super.referenceVTableHeapType(irClass)
    }

    override fun referenceFunctionType(irClass: IrFunctionSymbol): FunctionTypeSymbol {
        moduleReferencedTypes.addFunctionTypeToReferenced(irClass)
        return super.referenceFunctionType(irClass)
    }

    override fun referenceFunctionHeapType(irClass: IrFunctionSymbol): FunctionHeapTypeSymbol {
        moduleReferencedTypes.addFunctionTypeToReferenced(irClass)
        return super.referenceFunctionHeapType(irClass)
    }
}
