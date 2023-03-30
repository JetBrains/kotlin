/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.wasm.ir.*

class WasmModuleCodegenContext(
    val backendContext: WasmBackendContext,
    private val wasmFragment: WasmCompiledModuleFragment
) {
    private val typeTransformer =
        WasmTypeTransformer(this, backendContext.irBuiltIns)

    val scratchMemAddr: WasmSymbol<Int>
        get() = wasmFragment.scratchMemAddr

    val stringPoolSize: WasmSymbol<Int>
        get() = wasmFragment.stringPoolSize

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
            if (context.backendContext.inlineClassesUtils.shouldValueParameterBeBoxed(irValueParameter)) {
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

    fun referenceStringLiteralAddressAndId(string: String): Pair<WasmSymbol<Int>, WasmSymbol<Int>> {
        val address = wasmFragment.stringLiteralAddress.reference(string)
        val id = wasmFragment.stringLiteralPoolId.reference(string)
        return address to id
    }

    fun referenceConstantArray(resource: Pair<List<Long>, WasmType>): WasmSymbol<Int> =
        wasmFragment.constantArrayDataSegmentId.reference(resource)

    fun generateTypeInfo(irClass: IrClassSymbol, typeInfo: ConstantDataElement) {
        wasmFragment.typeInfo.define(irClass, typeInfo)
    }

    fun registerInitFunction(wasmFunction: WasmFunction, priority: String) {
        wasmFragment.initFunctions += WasmCompiledModuleFragment.FunWithPriority(wasmFunction, priority)
    }

    fun addExport(wasmExport: WasmExport<*>) {
        wasmFragment.exports += wasmExport
    }

    fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction) {
        wasmFragment.functions.define(irFunction, wasmFunction)
    }

    fun defineGlobalField(irField: IrFieldSymbol, wasmGlobal: WasmGlobal) {
        wasmFragment.globalFields.define(irField, wasmGlobal)
    }

    fun defineGlobalVTable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        wasmFragment.globalVTables.define(irClass, wasmGlobal)
    }

    fun defineGlobalClassITable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        wasmFragment.globalClassITables.define(irClass, wasmGlobal)
    }

    fun defineGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        wasmFragment.gcTypes.define(irClass, wasmType)
    }

    fun defineVTableGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        wasmFragment.vTableGcTypes.define(irClass, wasmType)
    }

    fun defineFunctionType(irFunction: IrFunctionSymbol, wasmFunctionType: WasmFunctionType) {
        wasmFragment.functionTypes.define(irFunction, wasmFunctionType)
    }

    private val classMetadataCache = mutableMapOf<IrClassSymbol, ClassMetadata>()
    fun getClassMetadata(irClass: IrClassSymbol): ClassMetadata =
        classMetadataCache.getOrPut(irClass) {
            val superClass = irClass.owner.getSuperClass(backendContext.irBuiltIns)
            val superClassMetadata = superClass?.let { getClassMetadata(it.symbol) }
            ClassMetadata(
                irClass.owner,
                superClassMetadata,
                backendContext.irBuiltIns
            )
        }

    private val interfaceMetadataCache = mutableMapOf<IrClassSymbol, InterfaceMetadata>()
    fun getInterfaceMetadata(irClass: IrClassSymbol): InterfaceMetadata =
        interfaceMetadataCache.getOrPut(irClass) { InterfaceMetadata(irClass.owner, backendContext.irBuiltIns) }

    fun referenceFunction(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunction> =
        wasmFragment.functions.reference(irFunction)

    fun referenceGlobalField(irField: IrFieldSymbol): WasmSymbol<WasmGlobal> =
        wasmFragment.globalFields.reference(irField)

    fun referenceGlobalVTable(irClass: IrClassSymbol): WasmSymbol<WasmGlobal> =
        wasmFragment.globalVTables.reference(irClass)

    fun referenceGlobalClassITable(irClass: IrClassSymbol): WasmSymbol<WasmGlobal> =
        wasmFragment.globalClassITables.reference(irClass)

    private fun referenceNonNothingType(
        irClass: IrClassSymbol,
        from: WasmCompiledModuleFragment.ReferencableAndDefinable<IrClassSymbol, WasmTypeDeclaration>
    ): WasmSymbol<WasmTypeDeclaration> {
        val type = irClass.defaultType
        require(!type.isNothing()) {
            "Can't reference Nothing type"
        }
        return from.reference(irClass)
    }

    fun referenceGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> =
        referenceNonNothingType(irClass, wasmFragment.gcTypes)

    fun referenceVTableGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> =
        referenceNonNothingType(irClass, wasmFragment.vTableGcTypes)

    fun referenceClassITableGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> =
        referenceNonNothingType(irClass, wasmFragment.classITableGcType)

    fun defineClassITableGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        wasmFragment.classITableGcType.define(irClass, wasmType)
    }

    fun isAlreadyDefinedClassITableGcType(irClass: IrClassSymbol): Boolean =
        wasmFragment.classITableGcType.defined.keys.contains(irClass)

    fun referenceClassITableInterfaceSlot(irClass: IrClassSymbol): WasmSymbol<Int> {
        val type = irClass.defaultType
        require(!type.isNothing()) {
            "Can't reference Nothing type"
        }
        return wasmFragment.classITableInterfaceSlot.reference(irClass)
    }

    fun defineClassITableInterfaceSlot(irClass: IrClassSymbol, slot: Int) {
        wasmFragment.classITableInterfaceSlot.define(irClass, slot)
    }

    fun referenceFunctionType(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunctionType> =
        wasmFragment.functionTypes.reference(irFunction)

    fun referenceTypeId(irClass: IrClassSymbol): WasmSymbol<Int> =
        wasmFragment.typeIds.reference(irClass)

    fun getStructFieldRef(field: IrField): WasmSymbol<Int> {
        val klass = field.parentAsClass
        val metadata = getClassMetadata(klass.symbol)
        val fieldId = metadata.fields.indexOf(field) + 2 //Implicit vtable and vtable field
        return WasmSymbol(fieldId)
    }

    fun addJsFun(importName: String, jsCode: String) {
        wasmFragment.jsFuns +=
            WasmCompiledModuleFragment.JsCodeSnippet(importName = importName, jsCode = jsCode)
    }

    fun addJsModuleImport(module: String) {
        wasmFragment.jsModuleImports += module
    }
}

