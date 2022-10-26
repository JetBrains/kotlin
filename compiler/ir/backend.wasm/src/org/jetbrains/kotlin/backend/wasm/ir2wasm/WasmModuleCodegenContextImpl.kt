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

class WasmModuleCodegenContextImpl(
    override val backendContext: WasmBackendContext,
    private val wasmFragment: WasmCompiledModuleFragment
) : WasmModuleCodegenContext {
    private val typeTransformer =
        WasmTypeTransformer(this, backendContext.irBuiltIns)

    override val scratchMemAddr: WasmSymbol<Int>
        get() = wasmFragment.scratchMemAddr

    override val stringPoolSize: WasmSymbol<Int>
        get() = wasmFragment.stringPoolSize

    override val scratchMemSizeInBytes: Int
        get() = wasmFragment.scratchMemSizeInBytes

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

    override fun referenceStringLiteralAddressAndId(string: String): Pair<WasmSymbol<Int>, WasmSymbol<Int>> {
        val address = wasmFragment.stringLiteralAddress.reference(string)
        val id = wasmFragment.stringLiteralPoolId.reference(string)
        return address to id
    }

    override fun referenceConstantArray(resource: Pair<List<Long>, WasmType>): WasmSymbol<Int> =
        wasmFragment.constantArrayDataSegmentId.reference(resource)

    override fun generateTypeInfo(irClass: IrClassSymbol, typeInfo: ConstantDataElement) {
        wasmFragment.typeInfo.define(irClass, typeInfo)
    }

    override fun registerInitFunction(wasmFunction: WasmFunction, priority: String) {
        wasmFragment.initFunctions += WasmCompiledModuleFragment.FunWithPriority(wasmFunction, priority)
    }

    override fun addExport(wasmExport: WasmExport<*>) {
        wasmFragment.exports += wasmExport
    }

    override fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction) {
        wasmFragment.functions.define(irFunction, wasmFunction)
    }

    override fun defineGlobalField(irField: IrFieldSymbol, wasmGlobal: WasmGlobal) {
        wasmFragment.globalFields.define(irField, wasmGlobal)
    }

    override fun defineGlobalVTable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        wasmFragment.globalVTables.define(irClass, wasmGlobal)
    }

    override fun defineGlobalClassITable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        wasmFragment.globalClassITables.define(irClass, wasmGlobal)
    }

    override fun defineGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        wasmFragment.gcTypes.define(irClass, wasmType)
    }

    override fun defineVTableGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        wasmFragment.vTableGcTypes.define(irClass, wasmType)
    }

    override fun defineFunctionType(irFunction: IrFunctionSymbol, wasmFunctionType: WasmFunctionType) {
        wasmFragment.functionTypes.define(irFunction, wasmFunctionType)
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

    private val interfaceMetadataCache = mutableMapOf<IrClassSymbol, InterfaceMetadata>()
    override fun getInterfaceMetadata(irClass: IrClassSymbol): InterfaceMetadata =
        interfaceMetadataCache.getOrPut(irClass) { InterfaceMetadata(irClass.owner, backendContext.irBuiltIns) }

    override fun referenceFunction(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunction> =
        wasmFragment.functions.reference(irFunction)

    override fun referenceGlobalField(irField: IrFieldSymbol): WasmSymbol<WasmGlobal> =
        wasmFragment.globalFields.reference(irField)

    override fun referenceGlobalVTable(irClass: IrClassSymbol): WasmSymbol<WasmGlobal> =
        wasmFragment.globalVTables.reference(irClass)

    override fun referenceGlobalClassITable(irClass: IrClassSymbol): WasmSymbol<WasmGlobal> =
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

    override fun referenceGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> =
        referenceNonNothingType(irClass, wasmFragment.gcTypes)

    override fun referenceVTableGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> =
        referenceNonNothingType(irClass, wasmFragment.vTableGcTypes)

    override fun referenceClassITableGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> =
        referenceNonNothingType(irClass, wasmFragment.classITableGcType)

    override fun defineClassITableGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        wasmFragment.classITableGcType.define(irClass, wasmType)
    }

    override fun isAlreadyDefinedClassITableGcType(irClass: IrClassSymbol): Boolean =
        wasmFragment.classITableGcType.defined.keys.contains(irClass)

    override fun referenceClassITableInterfaceSlot(irClass: IrClassSymbol): WasmSymbol<Int> {
        val type = irClass.defaultType
        require(!type.isNothing()) {
            "Can't reference Nothing type"
        }
        return wasmFragment.classITableInterfaceSlot.reference(irClass)
    }

    override fun defineClassITableInterfaceSlot(irClass: IrClassSymbol, slot: Int) {
        wasmFragment.classITableInterfaceSlot.define(irClass, slot)
    }

    override fun referenceFunctionType(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunctionType> =
        wasmFragment.functionTypes.reference(irFunction)

    override fun referenceClassId(irClass: IrClassSymbol): WasmSymbol<Int> =
        wasmFragment.classIds.reference(irClass)

    override fun referenceInterfaceId(irInterface: IrClassSymbol): WasmSymbol<Int> {
        return wasmFragment.interfaceId.reference(irInterface)
    }

    override fun getStructFieldRef(field: IrField): WasmSymbol<Int> {
        val klass = field.parentAsClass
        val metadata = getClassMetadata(klass.symbol)
        val fieldId = metadata.fields.indexOf(field) + 2 //Implicit vtable and vtable field
        return WasmSymbol(fieldId)
    }

    override fun addJsFun(importName: String, jsCode: String) {
        wasmFragment.jsFuns +=
            WasmCompiledModuleFragment.JsCodeSnippet(importName = importName, jsCode = jsCode)
    }
}

