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
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.getAllSuperclasses

class ModuleReferencedTypes(
    val gcTypes: MutableSet<IdSignature> = mutableSetOf(),
    val functionTypes: MutableSet<IdSignature> = mutableSetOf(),
)

fun ModuleReferencedTypes.addGcTypeToReferenced(
    irClass: IrClassSymbol,
    referencedModules: MutableSet<String>?,
    idSignatureRetriever: IdSignatureRetriever
) {
    val irClassOwner = irClass.owner
    val signature = idSignatureRetriever.declarationSignature(irClassOwner)!!
    if (!gcTypes.add(signature)) return

    if (referencedModules != null) {
        irClassOwner.fileOrNull?.module?.let { referencedModules.add(it.name.asString()) }
    }

    irClassOwner.declarations.forEach {
        when (it) {
            is IrFunction -> {
                addFunctionTypeToReferenced(it.symbol, referencedModules, idSignatureRetriever)
            }
            is IrField -> {
                addGcTypeToReferenced(it.type.erasedUpperBound.symbol, referencedModules, idSignatureRetriever)
            }
            is IrProperty -> {}
            is IrClass -> {}
            is IrTypeAlias -> {}
            else -> {
                error("Unexpected symbol type ${it.symbol}")
            }
        }
    }

    irClassOwner.getAllSuperclasses().forEach { addGcTypeToReferenced(it.symbol, referencedModules, idSignatureRetriever) }
}

fun ModuleReferencedTypes.addFunctionTypeToReferenced(
    irClass: IrFunctionSymbol,
    referencedModules: MutableSet<String>?,
    idSignatureRetriever: IdSignatureRetriever
) {
    val irFunctionOwner = irClass.owner
    val signature = idSignatureRetriever.declarationSignature(irFunctionOwner)!!
    if (!functionTypes.add(signature)) return

    if (referencedModules != null) {
        irFunctionOwner.fileOrNull?.module?.let { referencedModules.add(it.name.asString()) }
    }

    irFunctionOwner.parameters.forEach { p ->
        p.type.erasedUpperBound.symbol.let { addGcTypeToReferenced(it, referencedModules, idSignatureRetriever) }
    }
    addGcTypeToReferenced(irFunctionOwner.returnType.erasedUpperBound.symbol, referencedModules, idSignatureRetriever)
}

class WasmTrackedTypeCodegenContext(
    wasmFileFragment: WasmCompiledTypesFileFragment,
    private val moduleReferencedTypes: ModuleReferencedTypes,
    private val idSignatureRetriever: IdSignatureRetriever,
    private val referencedModules: MutableSet<String>?,
) : WasmTypeCodegenContext(wasmFileFragment, idSignatureRetriever) {

    override fun referenceGcType(irClass: IrClassSymbol): GcTypeSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass, referencedModules, idSignatureRetriever)
        return super.referenceGcType(irClass)
    }

    override fun referenceHeapType(irClass: IrClassSymbol): GcHeapTypeSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass, referencedModules, idSignatureRetriever)
        return super.referenceHeapType(irClass)
    }

    override fun referenceVTableGcType(irClass: IrClassSymbol): VTableTypeSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass, referencedModules, idSignatureRetriever)
        return super.referenceVTableGcType(irClass)
    }

    override fun referenceVTableHeapType(irClass: IrClassSymbol): VTableHeapTypeSymbol {
        moduleReferencedTypes.addGcTypeToReferenced(irClass, referencedModules, idSignatureRetriever)
        return super.referenceVTableHeapType(irClass)
    }

    override fun referenceFunctionType(irClass: IrFunctionSymbol): FunctionTypeSymbol {
        moduleReferencedTypes.addFunctionTypeToReferenced(irClass, referencedModules, idSignatureRetriever)
        return super.referenceFunctionType(irClass)
    }

    override fun referenceFunctionHeapType(irClass: IrFunctionSymbol): FunctionHeapTypeSymbol {
        moduleReferencedTypes.addFunctionTypeToReferenced(irClass, referencedModules, idSignatureRetriever)
        return super.referenceFunctionHeapType(irClass)
    }
}
