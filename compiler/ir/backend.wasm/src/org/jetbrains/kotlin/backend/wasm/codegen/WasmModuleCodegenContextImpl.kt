/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.parentAsClass


class WasmModuleCodegenContextImpl(
    override val backendContext: WasmBackendContext,
    private val wasmFragment: WasmCompiledModuleFragment
): WasmModuleCodegenContext {
    private val typeTransformer =
        WasmTypeTransformer(this, backendContext.irBuiltIns)

    override fun transformType(irType: IrType): WasmValueType {
        return with(typeTransformer) { irType.toWasmValueType() }
    }

    override fun transformResultType(irType: IrType): WasmValueType? {
        return with(typeTransformer) { irType.toWasmResultType() }
    }

    override fun referenceStringLiteral(string: String): WasmSymbol<Int> {
        wasmFragment.stringLiterals.add(string)
        return wasmFragment.stringLiteralId.reference(string)
    }

    override fun generateTypeInfo(irClass: IrClassSymbol, typeInfo: ConstantDataElement) {
        wasmFragment.typeInfo.define(irClass, typeInfo)
    }

    override fun setStartFunction(wasmFunction: WasmFunction) {
        wasmFragment.startFunction = wasmFunction
    }

    override fun addExport(wasmExport: WasmExport) {
        wasmFragment.exports += wasmExport
    }

    override fun registerVirtualFunction(irFunction: IrSimpleFunctionSymbol) {
        wasmFragment.virtualFunctions += irFunction
    }

    override fun registerInterface(irInterface: IrClassSymbol) {
        wasmFragment.interfaces += irInterface
    }

    override fun registerClass(irClass: IrClassSymbol) {
        wasmFragment.classes += irClass
    }

    override fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction) {
        wasmFragment.functions.define(irFunction, wasmFunction)
    }

    override fun defineGlobal(irField: IrFieldSymbol, wasmGlobal: WasmGlobal) {
        wasmFragment.globals.define(irField, wasmGlobal)
    }

    override fun defineStructType(irClass: IrClassSymbol, wasmStructType: WasmStructType) {
        wasmFragment.structTypes.define(irClass, wasmStructType)
    }

    override fun defineFunctionType(irFunction: IrFunctionSymbol, wasmFunctionType: WasmFunctionType) {
        wasmFragment.functionTypes.define(irFunction, wasmFunctionType)
    }

    private val classMetadataCache = mutableMapOf<IrClassSymbol, ClassMetadata>()
    override fun getClassMetadata(irClass: IrClassSymbol): ClassMetadata =
        classMetadataCache.getOrPut(irClass) {
            val superClass = irClass.owner.getSuperClass(backendContext.irBuiltIns)
            val superClassMetadata = superClass?.let { getClassMetadata(it.symbol) }
            ClassMetadata(irClass.owner, superClassMetadata)
        }

    override fun referenceFunction(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunction> =
        wasmFragment.functions.reference(irFunction)

    override fun referenceGlobal(irField: IrFieldSymbol): WasmSymbol<WasmGlobal> =
        wasmFragment.globals.reference(irField)

    override fun referenceStructType(irClass: IrClassSymbol): WasmSymbol<WasmStructType> =
        wasmFragment.structTypes.reference(irClass)

    override fun referenceFunctionType(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunctionType> =
        wasmFragment.functionTypes.reference(irFunction)

    override fun referenceClassId(irClass: IrClassSymbol): WasmSymbol<Int> =
        wasmFragment.classIds.reference(irClass)

    override fun referenceInterfaceId(irInterface: IrClassSymbol): WasmSymbol<Int> =
        wasmFragment.interfaceId.reference(irInterface)

    override fun referenceVirtualFunctionId(irFunction: IrSimpleFunctionSymbol): WasmSymbol<Int> {
        if (irFunction.owner.modality == Modality.ABSTRACT)
            error("Abstract functions are not stored in table")
        return wasmFragment.virtualFunctionId.reference(irFunction)
    }

    override fun getStructFieldRef(field: IrField): WasmSymbol<Int> {
        val klass = field.parentAsClass
        val metadata = getClassMetadata(klass.symbol)
        val fieldId = metadata.fields.indexOf(field)
        return WasmSymbol(fieldId)
    }
}