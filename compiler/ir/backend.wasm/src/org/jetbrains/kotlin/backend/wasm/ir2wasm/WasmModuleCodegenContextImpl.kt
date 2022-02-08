/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.lower.WasmSignature
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.wasm.ir.*

class WasmModuleCodegenContextImpl(
    override val backendContext: WasmBackendContext,
    private val wasmFragment: WasmCompiledModuleFragment
) : WasmModuleCodegenContext {
    private val typeTransformer =
        WasmTypeTransformer(this, backendContext.irBuiltIns)

    override val scratchMemAddr: WasmSymbol<Int>
        get() = wasmFragment.scratchMemAddr

    override fun transformType(irType: IrType): WasmType {
        return with(typeTransformer) { irType.toWasmValueType() }
    }

    override fun transformFieldType(irType: IrType): WasmType {
        return with(typeTransformer) { irType.toWasmFieldType() }
    }

    override fun transformBoxedType(irType: IrType): WasmType {
        return with(typeTransformer) { irType.toBoxedInlineClassType() }
    }

    override fun transformValueParameterType(irValueParameter: IrValueParameter): WasmType {
        return with(typeTransformer) {
            if (context.backendContext.inlineClassesUtils.shouldValueParameterBeBoxed(irValueParameter)) {
                irValueParameter.type.toBoxedInlineClassType()
            } else {
                irValueParameter.type.toWasmValueType()
            }
        }
    }

    override fun transformResultType(irType: IrType): WasmType? {
        return with(typeTransformer) { irType.toWasmResultType() }
    }

    override fun transformBlockResultType(irType: IrType): WasmType? {
        return with(typeTransformer) { irType.toWasmBlockResultType() }
    }

    override fun referenceStringLiteral(string: String): WasmSymbol<Int> {
        return wasmFragment.stringLiteralId.reference(string)
    }

    override fun generateTypeInfo(irClass: IrClassSymbol, typeInfo: ConstantDataElement) {
        wasmFragment.typeInfo.define(irClass, typeInfo)
    }

    override fun generateInterfaceTable(irClass: IrClassSymbol, table: ConstantDataElement) {
        wasmFragment.definedClassITableData.define(irClass, table)
    }

    override fun registerInitFunction(wasmFunction: WasmFunction, priority: String) {
        wasmFragment.initFunctions += WasmCompiledModuleFragment.FunWithPriority(wasmFunction, priority)
    }

    override fun addExport(wasmExport: WasmExport<*>) {
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

    override fun defineGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        wasmFragment.gcTypes.define(irClass, wasmType)
    }

    override fun defineRTT(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        wasmFragment.runtimeTypes.define(irClass, wasmGlobal)
    }

    override fun defineFunctionType(irFunction: IrFunctionSymbol, wasmFunctionType: WasmFunctionType) {
        wasmFragment.functionTypes.define(irFunction, wasmFunctionType)
    }

    override fun defineInterfaceMethodTable(irFunction: IrFunctionSymbol, wasmTable: WasmTable) {
        wasmFragment.interfaceMethodTables.define(irFunction, wasmTable)
    }

    override fun referenceInterfaceImplementationId(
        interfaceImplementation: InterfaceImplementation
    ): WasmSymbol<Int> =
        wasmFragment.referencedInterfaceImplementationId.reference(interfaceImplementation)


    override fun registerInterfaceImplementationMethod(
        interfaceImplementation: InterfaceImplementation,
        table: Map<IrFunctionSymbol, WasmSymbol<WasmFunction>?>
    ) {
        wasmFragment.interfaceImplementationsMethods[interfaceImplementation] = table
    }

    private val classMetadataCache = mutableMapOf<IrClassSymbol, ClassMetadata>()
    override fun getClassMetadata(irClass: IrClassSymbol): ClassMetadata =
        classMetadataCache.getOrPut(irClass) {
            val superClass = irClass.owner.getSuperClass(backendContext.irBuiltIns)
            val superClassMetadata = superClass?.let { getClassMetadata(it.symbol) }
            ClassMetadata(
                irClass.owner,
                superClassMetadata,
                backendContext.irBuiltIns
            )
        }

    override fun referenceFunction(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunction> =
        wasmFragment.functions.reference(irFunction)

    override fun referenceGlobal(irField: IrFieldSymbol): WasmSymbol<WasmGlobal> =
        wasmFragment.globals.reference(irField)

    override fun referenceGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> {
        val type = irClass.defaultType
        require(!type.isNothing()) {
            "Can't reference Nothing type"
        }
        return wasmFragment.gcTypes.reference(irClass)
    }

    override fun referenceClassRTT(irClass: IrClassSymbol): WasmSymbol<WasmGlobal> =
        wasmFragment.runtimeTypes.reference(irClass)

    override fun referenceFunctionType(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunctionType> =
        wasmFragment.functionTypes.reference(irFunction)

    override fun referenceClassId(irClass: IrClassSymbol): WasmSymbol<Int> =
        wasmFragment.classIds.reference(irClass)

    override fun referenceInterfaceTableAddress(irClass: IrClassSymbol): WasmSymbol<Int> {
        if (irClass.owner.modality == Modality.ABSTRACT) return WasmSymbol(-1)
        return wasmFragment.referencedClassITableAddresses.reference(irClass)
    }


    override fun referenceInterfaceId(irInterface: IrClassSymbol): WasmSymbol<Int> {
        return wasmFragment.interfaceId.reference(irInterface)
    }

    override fun referenceVirtualFunctionId(irFunction: IrSimpleFunctionSymbol): WasmSymbol<Int> {
        if (irFunction.owner.modality == Modality.ABSTRACT)
            error("Abstract functions are not stored in table")
        return wasmFragment.virtualFunctionId.reference(irFunction)
    }

    override fun referenceSignatureId(signature: WasmSignature): WasmSymbol<Int> {
        wasmFragment.signatures.add(signature)
        return wasmFragment.signatureId.reference(signature)
    }

    override fun referenceInterfaceTable(irFunction: IrFunctionSymbol): WasmSymbol<WasmTable> {
        return wasmFragment.interfaceMethodTables.reference(irFunction)
    }

    override fun getStructFieldRef(field: IrField): WasmSymbol<Int> {
        val klass = field.parentAsClass
        val metadata = getClassMetadata(klass.symbol)
        val fieldId = metadata.fields.indexOf(field)
        return WasmSymbol(fieldId)
    }

    override fun addJsFun(importName: String, jsCode: String) {
        wasmFragment.jsFuns +=
            WasmCompiledModuleFragment.JsCodeSnippet(importName = importName, jsCode = jsCode)
    }
}