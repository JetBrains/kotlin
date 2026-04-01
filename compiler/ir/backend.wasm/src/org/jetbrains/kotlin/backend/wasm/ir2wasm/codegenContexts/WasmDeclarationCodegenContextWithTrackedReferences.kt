/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fileOrNull

class WasmDeclarationCodegenContextWithTrackedReferences(
    private val moduleReferencedDeclarations: ModuleReferencedDeclarations,
    private val moduleReferencedTypes: ModuleReferencedTypes?,
    private val referencedModules: MutableSet<String>?,
    wasmFileFragment: WasmCompiledDeclarationsFileFragment,
    private val idSignatureRetriever: IdSignatureRetriever,
) : WasmDeclarationCodegenContext(
    wasmFileFragment = wasmFileFragment,
    idSignatureRetriever = idSignatureRetriever,
) {
    override fun referenceFunction(irFunction: IrFunctionSymbol): FuncSymbol {
        val functionOwner = irFunction.owner
        if (functionOwner.isEffectivelyPrivate()) return super.referenceFunction(irFunction)

        val signature = idSignatureRetriever.declarationSignature(functionOwner)!!
        if (!moduleReferencedDeclarations.functions.add(signature)) return super.referenceFunction(irFunction)

        if (referencedModules != null) {
            functionOwner.fileOrNull?.module?.let { referencedModules.add(it.name.asString()) }
        }

        moduleReferencedTypes?.addFunctionTypeToReferenced(
            irClass = irFunction,
            referencedModules = referencedModules,
            idSignatureRetriever = idSignatureRetriever
        )
        return super.referenceFunction(irFunction)
    }

    private fun addClassSymbol(irClass: IrClassSymbol, to: MutableSet<IdSignature>) {
        val classOwner = irClass.owner
        if (classOwner.isEffectivelyPrivate()) return

        val signature = idSignatureRetriever.declarationSignature(classOwner)!!
        if (!to.add(signature)) return

        if (referencedModules != null) {
            classOwner.fileOrNull?.module?.let { referencedModules.add(it.name.asString()) }
        }

        moduleReferencedTypes?.addGcTypeToReferenced(
            irClass = irClass,
            referencedModules = referencedModules,
            idSignatureRetriever = idSignatureRetriever
        )
    }

    override fun referenceGlobalVTable(irClass: IrClassSymbol): VTableGlobalSymbol {
        addClassSymbol(irClass, moduleReferencedDeclarations.globalVTable)
        return super.referenceGlobalVTable(irClass)
    }

    override fun referenceGlobalClassITable(irClass: IrClassSymbol): ClassITableGlobalSymbol {
        addClassSymbol(irClass, moduleReferencedDeclarations.globalClassITable)
        return super.referenceGlobalClassITable(irClass)
    }

    override fun referenceRttiGlobal(irClass: IrClassSymbol): RttiGlobalSymbol {
        addClassSymbol(irClass, moduleReferencedDeclarations.rttiGlobal)
        return super.referenceRttiGlobal(irClass)
    }
}