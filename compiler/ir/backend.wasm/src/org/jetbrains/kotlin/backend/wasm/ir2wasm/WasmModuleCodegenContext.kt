/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.wasm.ir.*

class WasmModuleCodegenContext(
    val backendContext: WasmBackendContext,
    val idSignatureRetriever: IdSignatureRetriever,
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
        wasmFragment.typeInfo.define(irClass.getReferenceKey(), typeInfo)
    }

    fun registerInitFunction(wasmFunction: WasmFunction, priority: String) {
        wasmFragment.initFunctions += WasmCompiledModuleFragment.FunWithPriority(wasmFunction, priority)
    }

    fun addExport(wasmExport: WasmExport<*>) {
        wasmFragment.exports += wasmExport
    }

    private fun IrSymbol.getReferenceKey(): ReferenceKey =
        idSignatureRetriever.declarationSignature(this.owner as IrDeclaration)
            ?.let { SignatureKey(it) }
            ?: SymbolKey(this)

    private fun IrClassSymbol.getSignature(): IdSignature =
        idSignatureRetriever.declarationSignature(this.owner)!!

    fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction) {
        wasmFragment.functions.define(irFunction.getReferenceKey(), wasmFunction)
    }

    fun defineGlobalField(irField: IrFieldSymbol, wasmGlobal: WasmGlobal) {
        wasmFragment.globalFields.define(irField.getReferenceKey(), wasmGlobal)
    }

    fun defineGlobalVTable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        wasmFragment.globalVTables.define(irClass.getReferenceKey(), wasmGlobal)
    }

    fun defineGlobalClassITable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        wasmFragment.globalClassITables.define(irClass.getReferenceKey(), wasmGlobal)
    }

    fun addInterfaceUnion(interfaces: List<IrClassSymbol>) {
        wasmFragment.interfaceUnions.add(interfaces.map { idSignatureRetriever.declarationSignature(it.owner)!! })
    }

    fun defineGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        wasmFragment.gcTypes.define(irClass.getReferenceKey(), wasmType)
    }

    fun defineVTableGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        wasmFragment.vTableGcTypes.define(irClass.getReferenceKey(), wasmType)
    }

    fun defineFunctionType(irFunction: IrFunctionSymbol, wasmFunctionType: WasmFunctionType) {
        wasmFragment.functionTypes.define(irFunction.getReferenceKey(), wasmFunctionType)
    }

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

    private val interfaceMetadataCache = mutableMapOf<IrClassSymbol, InterfaceMetadata>()
    fun getInterfaceMetadata(irClass: IrClassSymbol): InterfaceMetadata =
        interfaceMetadataCache.getOrPut(irClass) { InterfaceMetadata(irClass.owner, backendContext.irBuiltIns) }

    fun referenceFunction(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunction> =
        wasmFragment.functions.reference(irFunction.getReferenceKey())

    fun referenceGlobalField(irField: IrFieldSymbol): WasmSymbol<WasmGlobal> =
        wasmFragment.globalFields.reference(irField.getReferenceKey())

    fun referenceGlobalVTable(irClass: IrClassSymbol): WasmSymbol<WasmGlobal> =
        wasmFragment.globalVTables.reference(irClass.getReferenceKey())

    fun referenceGlobalClassITable(irClass: IrClassSymbol): WasmSymbol<WasmGlobal> =
        wasmFragment.globalClassITables.reference(irClass.getReferenceKey())

    fun referenceGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> =
        wasmFragment.gcTypes.reference(irClass.getReferenceKey())

    fun referenceVTableGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> =
        wasmFragment.vTableGcTypes.reference(irClass.getReferenceKey())

    fun referenceVTableGcType(iface: IdSignature): WasmSymbol<WasmTypeDeclaration> =
        wasmFragment.vTableGcTypes.reference(SignatureKey(iface))

    fun referenceClassITableGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> =
        wasmFragment.classITableGcType.reference(irClass.getSignature())

    fun referenceClassITableInterfaceTableSize(irInterface: IrClassSymbol): WasmSymbol<Int> =
        wasmFragment.classITableInterfaceTableSize.reference(irInterface.getSignature())

    fun referenceClassITableInterfaceHasImplementors(irInterface: IrClassSymbol): WasmSymbol<Int> =
        wasmFragment.classITableInterfaceHasImplementors.reference(irInterface.getSignature())

    fun defineClassITableGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        wasmFragment.classITableGcType.define(irClass.getSignature(), wasmType)
    }

    fun defineClassITableGcType(iface: IdSignature, wasmType: WasmTypeDeclaration) {
        wasmFragment.classITableGcType.define(iface, wasmType)
    }

    fun referenceClassITableInterfaceSlot(irClass: IrClassSymbol): WasmSymbol<Int> {
        val type = irClass.defaultType
        require(!type.isNothing()) {
            "Can't reference Nothing type"
        }
        return wasmFragment.classITableInterfaceSlot.reference(irClass.getSignature())
    }

    fun defineClassITableInterfaceSlot(iface: IdSignature, slot: Int) {
        wasmFragment.classITableInterfaceSlot.define(iface, slot)
    }

    fun defineClassITableInterfaceTableSize(iface: IdSignature, size: Int) {
        wasmFragment.classITableInterfaceTableSize.define(iface, size)
    }

    fun defineClassITableInterfaceHasImplementors(iface: IdSignature, hasImplementors: Int) {
        wasmFragment.classITableInterfaceHasImplementors.define(iface, hasImplementors)
    }

    fun defineDeclaredInterface(iface: IrClassSymbol) {
        wasmFragment.declaredInterfaces.add((iface.getReferenceKey() as SignatureKey).signature)
    }

    fun referenceFunctionType(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunctionType> =
        wasmFragment.functionTypes.reference(irFunction.getReferenceKey())

    fun referenceTypeId(irClass: IrClassSymbol): WasmSymbol<Int> =
        if (irClass.owner.isInterface) {
            wasmFragment.interfaceIds.reference(irClass.getSignature())
        } else {
            wasmFragment.classIds.reference(irClass.getReferenceKey())
        }

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


