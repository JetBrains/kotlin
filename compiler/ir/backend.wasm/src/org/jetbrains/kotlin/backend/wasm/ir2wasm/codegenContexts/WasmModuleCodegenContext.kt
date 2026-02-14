/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.wasm.ir.WasmType

class WasmModuleMetadataCache(private val backendContext: WasmBackendContext) {
    private val interfaceMetadataCache = mutableMapOf<IrClassSymbol, InterfaceMetadata>()
    fun getInterfaceMetadata(irClass: IrClassSymbol): InterfaceMetadata =
        interfaceMetadataCache.getOrPut(irClass) { InterfaceMetadata(irClass.owner, backendContext.irBuiltIns) }

    private val classMetadataCache = mutableMapOf<IrClassSymbol, ClassMetadata>()
    fun getClassMetadata(irClass: IrClassSymbol): ClassMetadata =
        classMetadataCache.getOrPut(irClass) {
            val superClass = irClass.owner.getSuperClass(backendContext.irBuiltIns)
            val superClassMetadata = superClass?.let { getClassMetadata(it.symbol) }
            ClassMetadata(
                klass = irClass.owner,
                superClass = superClassMetadata,
                irBuiltIns = backendContext.irBuiltIns,
                allowAccidentalOverride = backendContext.partialLinkageSupport.isEnabled
            )
        }
}

class WasmModuleTypeTransformer(
    backendContext: WasmBackendContext,
    wasmTypeContext: WasmTypeCodegenContext,
) {
    private val typeTransformer =
        WasmTypeTransformer(backendContext, wasmTypeContext)

    fun transformType(irType: IrType): WasmType {
        return with(typeTransformer) { irType.toWasmValueType() }
    }

    fun transformFieldType(irType: IrType): WasmType {
        return with(typeTransformer) { irType.toWasmFieldType() }
    }

    fun transformBoxedType(irType: IrType): WasmType {
        return with(typeTransformer) { irType.toBoxedInlineClassType() }
    }

    fun transformValueParameterType(irValueParameter: IrValueParameter): WasmType {
        return with(typeTransformer) {
            if (backendContext.inlineClassesUtils.shouldValueParameterBeBoxed(irValueParameter)) {
                irValueParameter.type.toBoxedInlineClassType()
            } else {
                irValueParameter.type.toWasmValueType()
            }
        }
    }

    fun transformResultType(irType: IrType): WasmType? {
        return with(typeTransformer) { irType.toWasmResultType() }
    }

    fun transformBlockResultType(irType: IrType): WasmType? {
        return with(typeTransformer) { irType.toWasmBlockResultType() }
    }

    fun hasBlockResultType(irType: IrType): Boolean {
        return with(typeTransformer) { irType.hasWasmResultType() }
    }
}

data class AssociatedObjectBySymbols(val klass: IrClassSymbol, val getter: IrFunctionSymbol, val isExternal: Boolean)