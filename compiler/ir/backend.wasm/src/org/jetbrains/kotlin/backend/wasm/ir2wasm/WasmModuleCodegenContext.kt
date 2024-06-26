/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.wasm.ir.*

class WasmFileCodegenContext(
    private val wasmFileFragment: WasmCompiledFileFragment,
    private val idSignatureRetriever: IdSignatureRetriever,
) {
    private fun IrSymbol.getReferenceKey(): IdSignature =
        idSignatureRetriever.declarationSignature(this.owner as IrDeclaration)!!

    fun referenceStringLiteralAddressAndId(string: String): Pair<WasmSymbol<Int>, WasmSymbol<Int>> {
        val address = wasmFileFragment.stringLiteralAddress.reference(string)
        val id = wasmFileFragment.stringLiteralPoolId.reference(string)
        return address to id
    }

    fun referenceConstantArray(resource: Pair<List<Long>, WasmType>): WasmSymbol<Int> =
        wasmFileFragment.constantArrayDataSegmentId.reference(resource)

    fun generateTypeInfo(irClass: IrClassSymbol, typeInfo: ConstantDataElement) {
        wasmFileFragment.typeInfo[irClass.getReferenceKey()] = typeInfo
    }

    fun addExport(wasmExport: WasmExport<*>) {
        wasmFileFragment.exports += wasmExport
    }

    private fun IrClassSymbol.getSignature(): IdSignature =
        idSignatureRetriever.declarationSignature(this.owner)!!

    fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction) {
        wasmFileFragment.functions.define(irFunction.getReferenceKey(), wasmFunction)
    }

    fun defineGlobalField(irField: IrFieldSymbol, wasmGlobal: WasmGlobal) {
        wasmFileFragment.globalFields.define(irField.getReferenceKey(), wasmGlobal)
    }

    fun defineGlobalVTable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        wasmFileFragment.globalVTables.define(irClass.getReferenceKey(), wasmGlobal)
    }

    fun defineGlobalClassITable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        wasmFileFragment.globalClassITables.define(irClass.getReferenceKey(), wasmGlobal)
    }

    fun addInterfaceUnion(interfaces: List<IrClassSymbol>) {
        wasmFileFragment.interfaceUnions.add(interfaces.map { idSignatureRetriever.declarationSignature(it.owner)!! })
    }

    fun defineGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        wasmFileFragment.gcTypes.define(irClass.getReferenceKey(), wasmType)
    }

    fun defineVTableGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        wasmFileFragment.vTableGcTypes.define(irClass.getReferenceKey(), wasmType)
    }

    fun defineFunctionType(irFunction: IrFunctionSymbol, wasmFunctionType: WasmFunctionType) {
        wasmFileFragment.functionTypes.define(irFunction.getReferenceKey(), wasmFunctionType)
    }

    fun referenceFunction(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunction> =
        wasmFileFragment.functions.reference(irFunction.getReferenceKey())

    fun referenceGlobalField(irField: IrFieldSymbol): WasmSymbol<WasmGlobal> =
        wasmFileFragment.globalFields.reference(irField.getReferenceKey())

    fun referenceGlobalVTable(irClass: IrClassSymbol): WasmSymbol<WasmGlobal> =
        wasmFileFragment.globalVTables.reference(irClass.getReferenceKey())

    fun referenceGlobalClassITable(irClass: IrClassSymbol): WasmSymbol<WasmGlobal> =
        wasmFileFragment.globalClassITables.reference(irClass.getReferenceKey())

    fun referenceGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> =
        wasmFileFragment.gcTypes.reference(irClass.getReferenceKey())

    fun referenceVTableGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> =
        wasmFileFragment.vTableGcTypes.reference(irClass.getReferenceKey())

    fun referenceClassITableGcType(irClass: IrClassSymbol): WasmSymbol<WasmTypeDeclaration> =
        wasmFileFragment.classITableGcType.reference(irClass.getSignature())

    fun referenceClassITableInterfaceTableSize(irInterface: IrClassSymbol): WasmSymbol<Int> =
        wasmFileFragment.classITableInterfaceTableSize.reference(irInterface.getSignature())

    fun referenceClassITableInterfaceHasImplementors(irInterface: IrClassSymbol): WasmSymbol<Int> =
        wasmFileFragment.classITableInterfaceHasImplementors.reference(irInterface.getSignature())

    fun referenceClassITableInterfaceSlot(irClass: IrClassSymbol): WasmSymbol<Int> {
        val type = irClass.defaultType
        require(!type.isNothing()) {
            "Can't reference Nothing type"
        }
        return wasmFileFragment.classITableInterfaceSlot.reference(irClass.getSignature())
    }

    fun referenceFunctionType(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunctionType> =
        wasmFileFragment.functionTypes.reference(irFunction.getReferenceKey())

    fun referenceTypeId(irClass: IrClassSymbol): WasmSymbol<Int> =
        if (irClass.owner.isInterface) {
            wasmFileFragment.interfaceIds.reference(irClass.getSignature())
        } else {
            wasmFileFragment.classIds.reference(irClass.getReferenceKey())
        }

    fun addJsFun(importName: WasmSymbol<String>, jsCode: String) {
        wasmFileFragment.jsFuns +=
            WasmCompiledModuleFragment.JsCodeSnippet(importName = importName, jsCode = jsCode)
    }

    fun addJsModuleImport(module: String) {
        wasmFileFragment.jsModuleImports += module
    }

    val scratchMemAddr: WasmSymbol<Int>
        get() = wasmFileFragment.scratchMemAddr

    val stringPoolSize: WasmSymbol<Int>
        get() = wasmFileFragment.stringPoolSize

    fun addFieldInitializer(irField: IrFieldSymbol, instructions: List<WasmInstr>, isObjectInstanceField: Boolean) {
        wasmFileFragment.fieldInitializers.add(FieldInitializer(irField.getReferenceKey(), instructions, isObjectInstanceField))
    }

    fun addMainFunctionWrapper(mainFunctionWrapper: IrFunctionSymbol) {
        wasmFileFragment.mainFunctionWrappers.add(mainFunctionWrapper.getReferenceKey())
    }

    fun defineTestFun(testFun: IrFunctionSymbol) {
        wasmFileFragment.testFun = testFun.getReferenceKey()
    }

    fun addClosureCallExport(exportSignature: String, exportFunction: IrFunctionSymbol) {
        wasmFileFragment.closureCallExports.add(exportSignature to exportFunction.getReferenceKey())
    }

    fun addClassAssociatedObjects(klass: IrClassSymbol, associatedObjectsGetters: List<Pair<IrClassSymbol, IrFunctionSymbol>>) {
        val classAssociatedObjects = ClassAssociatedObjects(
            klass.getReferenceKey(),
            associatedObjectsGetters.map { (obj, getter) ->
                AssociatedObject(obj.getReferenceKey(), getter.getReferenceKey())
            }
        )
        wasmFileFragment.classAssociatedObjectsInstanceGetters.add(classAssociatedObjects)
    }

    fun defineTryGetAssociatedObjectFun(func: IrFunctionSymbol) {
        wasmFileFragment.tryGetAssociatedObjectFun = func.getReferenceKey()
    }
}

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
    wasmFileCodegenContext: WasmFileCodegenContext,
) {
    private val typeTransformer =
        WasmTypeTransformer(backendContext, wasmFileCodegenContext)

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
}