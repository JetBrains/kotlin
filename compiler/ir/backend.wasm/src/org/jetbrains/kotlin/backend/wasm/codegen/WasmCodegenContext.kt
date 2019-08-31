/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.parentAsClass


class WasmCodegenContext(
    val backendContext: WasmBackendContext,
    val wasm: WasmCompiledModuleFragment
) {
    var locals: Map<IrValueSymbol, Int> = emptyMap()
    var labels: Map<LoopLabel, String> = emptyMap()

    var currentClass: IrClass? = null

    private val classMetadataCache = mutableMapOf<IrClassSymbol, ClassMetadata>()
    fun getClassMetadata(irClass: IrClass): ClassMetadata =
        classMetadataCache.getOrPut(irClass.symbol) {
            val superClass = irClass.getSuperClass(backendContext.irBuiltIns)
            val superClassMetadata = superClass?.let { getClassMetadata(it) }
            ClassMetadata(irClass, superClassMetadata)
        }

    fun getClassId(declaration: IrClass): WasmSymbol<Int> =
        wasm.classIds.reference(declaration.symbol)

    fun getInterfaceId(declaration: IrClass): WasmSymbol<Int> =
        wasm.interfaceId.reference(declaration.symbol)

    fun getGlobalName(declaration: IrFunction): WasmFunctionSymbol =
        wasm.functions.reference(declaration.symbol)

    fun getGlobalName(declaration: IrField): WasmGlobalSymbol {
        assert(declaration.isStatic) {
            "accessedField should be static"
        }
        return wasm.globals.reference(declaration.symbol)
    }

    private val structFieldCache = mutableMapOf<IrFieldSymbol, Int>()
    fun getStructFieldRef(declaration: IrField): WasmStructFieldSymbol {
        val klass = declaration.parentAsClass
        val metadata = getClassMetadata(klass)
        val fieldId = metadata.fields.indexOf(declaration)
        return WasmStructFieldSymbol(fieldId)
    }

    fun getStructTypeName(klass: IrClass): WasmStructTypeSymbol {
        return wasm.structTypes.reference(klass.symbol)
    }

    fun getFunctionTypeName(function: IrFunction): WasmFunctionTypeSymbol =
        wasm.functionTypes.reference(function.symbol)

    fun getLocalName(declaration: IrValueDeclaration): Int =
        locals.getValue(declaration.symbol)

    fun getBreakLabelName(loop: IrLoop): String =
        labels[LoopLabel(loop, LoopLabelType.BREAK)]
            ?: error("Can't find break label name")

    fun getContinueLabelName(loop: IrLoop): String =
        labels[LoopLabel(loop, LoopLabelType.CONTINUE)]
            ?: error("Can't find continue label name")

    fun getLoopLabelName(loop: IrLoop): String =
        labels[LoopLabel(loop, LoopLabelType.LOOP)]
            ?: error("Can't find loop label name")

    fun virtualFunctionId(function: IrSimpleFunction): WasmSymbol<Int> =
        wasm.virtualFunctionId.reference(function.symbol)
}