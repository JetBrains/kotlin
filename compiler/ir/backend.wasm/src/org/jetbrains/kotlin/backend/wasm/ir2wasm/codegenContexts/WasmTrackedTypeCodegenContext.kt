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

class ModuleReferencedTypes(
    val gcTypes: MutableSet<IdSignature> = mutableSetOf(),
    val functionTypes: MutableSet<IdSignature> = mutableSetOf(),
)

fun ModuleReferencedTypes.addGcTypeToReferenced(irClass: IrClassSymbol, idSignatureRetriever: IdSignatureRetriever) {
    val irClassOwner = irClass.owner
    val signature = idSignatureRetriever.declarationSignature(irClassOwner)!!
    if (!gcTypes.add(signature)) return

    irClassOwner.declarations.forEach {
        when (it) {
            is IrFunction -> {
                addFunctionTypeToReferenced(it.symbol, idSignatureRetriever)
            }
            is IrField -> {
                addGcTypeToReferenced(it.type.erasedUpperBound.symbol, idSignatureRetriever)
            }
            is IrProperty -> {}
            is IrClass -> {}
            is IrTypeAlias -> {}
            else -> {
                error("Unexpected symbol type ${it.symbol}")
            }
        }
    }

    irClassOwner.getAllSuperclasses().forEach { addGcTypeToReferenced(it.symbol, idSignatureRetriever) }
}

fun ModuleReferencedTypes.addFunctionTypeToReferenced(irClass: IrFunctionSymbol, idSignatureRetriever: IdSignatureRetriever) {
    val irFunctionOwner = irClass.owner
    val signature = idSignatureRetriever.declarationSignature(irFunctionOwner)!!
    if (!functionTypes.add(signature)) return

    irFunctionOwner.parameters.forEach { p ->
        p.type.erasedUpperBound.symbol.let { addGcTypeToReferenced(it, idSignatureRetriever) }
    }
    addGcTypeToReferenced(irFunctionOwner.returnType.erasedUpperBound.symbol, idSignatureRetriever)
}

class WasmTrackedTypeCodegenContext(
    wasmFileFragment: WasmCompiledTypesFileFragment,
    private val moduleReferencedTypes: ModuleReferencedTypes,
    private val idSignatureRetriever: IdSignatureRetriever,
) : WasmTypeCodegenContext(wasmFileFragment, idSignatureRetriever) {

    override fun referenceGcType(irClass: IrClassSymbol): GcTypeSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass, idSignatureRetriever)
        return super.referenceGcType(irClass)
    }

    override fun referenceHeapType(irClass: IrClassSymbol): GcHeapTypeSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass, idSignatureRetriever)
        return super.referenceHeapType(irClass)
    }

    override fun referenceVTableGcType(irClass: IrClassSymbol): VTableTypeSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass, idSignatureRetriever)
        return super.referenceVTableGcType(irClass)
    }

    override fun referenceVTableHeapType(irClass: IrClassSymbol): VTableHeapTypeSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass, idSignatureRetriever)
        return super.referenceVTableHeapType(irClass)
    }

    override fun referenceFunctionType(irClass: IrFunctionSymbol): FunctionTypeSymbol {
        moduleReferencedTypes.addFunctionTypeToReferenced(irClass, idSignatureRetriever)
        return super.referenceFunctionType(irClass)
    }

    override fun referenceFunctionHeapType(irClass: IrFunctionSymbol): FunctionHeapTypeSymbol {
        moduleReferencedTypes.addFunctionTypeToReferenced(irClass, idSignatureRetriever)
        return super.referenceFunctionHeapType(irClass)
    }
}
