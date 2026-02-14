/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol

class WasmDeclarationCodegenContextWithTrackedReferences(
    private val moduleReferencedDeclarations: ModuleReferencedDeclarations,
    private val moduleReferencedTypes: ModuleReferencedTypes?,
    wasmFileFragment: WasmCompiledDeclarationsFileFragment,
    private val idSignatureRetriever: IdSignatureRetriever,
) : WasmDeclarationCodegenContext(
    wasmFileFragment = wasmFileFragment,
    idSignatureRetriever = idSignatureRetriever,
) {
    override fun referenceFunction(irFunction: IrFunctionSymbol): FuncSymbol {
        moduleReferencedDeclarations.functions.add(
            idSignatureRetriever.declarationSignature(irFunction.owner)!!
        )
        moduleReferencedTypes?.addFunctionTypeToReferenced(irFunction, idSignatureRetriever)
        return super.referenceFunction(irFunction)
    }

    override fun referenceGlobalVTable(irClass: IrClassSymbol): VTableGlobalSymbol {
        moduleReferencedDeclarations.globalVTable.add(
            idSignatureRetriever.declarationSignature(irClass.owner)!!
        )
        moduleReferencedTypes?.addGcTypeToReferenced(irClass, idSignatureRetriever)
        return super.referenceGlobalVTable(irClass)
    }

    override fun referenceGlobalClassITable(irClass: IrClassSymbol): ClassITableGlobalSymbol {
        moduleReferencedDeclarations.globalClassITable.add(
            idSignatureRetriever.declarationSignature(irClass.owner)!!
        )
        moduleReferencedTypes?.addGcTypeToReferenced(irClass, idSignatureRetriever)
        return super.referenceGlobalClassITable(irClass)
    }

    override fun referenceRttiGlobal(irClass: IrClassSymbol): RttiGlobalSymbol {
        moduleReferencedDeclarations.rttiGlobal.add(
            idSignatureRetriever.declarationSignature(irClass.owner)!!
        )
        moduleReferencedTypes?.addGcTypeToReferenced(irClass, idSignatureRetriever)
        return super.referenceRttiGlobal(irClass)
    }
}