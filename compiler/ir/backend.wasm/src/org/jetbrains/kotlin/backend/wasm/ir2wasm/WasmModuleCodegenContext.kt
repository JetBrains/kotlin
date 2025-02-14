/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.wasm.ir.*


enum class TypeGlobalImportPrefix(val prefix: String) {
    VTABLE("vtable_"),
    ITABLE("itable_"),
    RTTI("rtti_"),
}

const val FunctionImportPrefix = "func_"

open class WasmFileCodegenContext(
    private val wasmFileFragment: WasmCompiledFileFragment,
    protected val idSignatureRetriever: IdSignatureRetriever,
) {
    open fun handleFunctionWithImport(declaration: IrFunctionSymbol): Boolean = false
    open fun handleVTableWithImport(declaration: IrClassSymbol): Boolean = false
    open fun handleClassITableWithImport(declaration: IrClassSymbol): Boolean = false
    open fun handleRTTIWithImport(declaration: IrClassSymbol, superType: IrClassSymbol?): Boolean = false

    private fun IrSymbol.getReferenceKey(): IdSignature =
        idSignatureRetriever.declarationSignature(this.owner as IrDeclaration)!!

    fun referenceStringLiteralAddressAndId(string: String): Pair<WasmSymbol<Int>, WasmSymbol<Int>> {
        val address = wasmFileFragment.stringLiteralAddress.reference(string)
        val id = wasmFileFragment.stringLiteralPoolId.reference(string)
        return address to id
    }

    fun referenceConstantArray(resource: Pair<List<Long>, WasmType>): WasmSymbol<Int> =
        wasmFileFragment.constantArrayDataSegmentId.reference(resource)

    fun addExport(wasmExport: WasmExport<*>) {
        wasmFileFragment.exports += wasmExport
    }

    private fun IrClassSymbol.getSignature(): IdSignature =
        idSignatureRetriever.declarationSignature(this.owner)!!

    open fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction) {
        wasmFileFragment.functions.define(irFunction.getReferenceKey(), wasmFunction)
    }

    fun defineGlobalField(irField: IrFieldSymbol, wasmGlobal: WasmGlobal) {
        wasmFileFragment.globalFields.define(irField.getReferenceKey(), wasmGlobal)
    }

    open fun defineGlobalVTable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        wasmFileFragment.globalVTables.define(irClass.getReferenceKey(), wasmGlobal)
    }

    open fun defineGlobalClassITable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        wasmFileFragment.globalClassITables.define(irClass.getReferenceKey(), wasmGlobal)
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

    fun referenceFunctionType(irFunction: IrFunctionSymbol): WasmSymbol<WasmFunctionType> =
        wasmFileFragment.functionTypes.reference(irFunction.getReferenceKey())

    fun referenceTypeId(irClass: IrClassSymbol): Long =
        cityHash64(irClass.getSignature().toString().encodeToByteArray()).toLong()

    fun addJsFun(irFunction: IrFunctionSymbol, importName: WasmSymbol<String>, jsCode: String) {
        wasmFileFragment.jsFuns[irFunction.getReferenceKey()] =
            WasmCompiledModuleFragment.JsCodeSnippet(importName = importName, jsCode = jsCode)
    }

    fun addJsModuleImport(irFunction: IrFunctionSymbol, module: String) {
        wasmFileFragment.jsModuleImports[irFunction.getReferenceKey()] = module
    }

    val stringPoolSize: WasmSymbol<Int>
        get() = wasmFileFragment.stringPoolSize
            ?: WasmSymbol<Int>().also { wasmFileFragment.stringPoolSize = it }

    val throwableTagIndex: WasmSymbol<Int>
        get() = wasmFileFragment.throwableTagIndex
            ?: WasmSymbol<Int>().also { wasmFileFragment.throwableTagIndex = it }

    val jsExceptionTagIndex: WasmSymbol<Int>
        get() = wasmFileFragment.jsExceptionTagIndex
            ?: WasmSymbol<Int>().also { wasmFileFragment.jsExceptionTagIndex = it }

    fun addFieldInitializer(irField: IrFieldSymbol, instructions: List<WasmInstr>, isObjectInstanceField: Boolean) {
        wasmFileFragment.fieldInitializers.add(FieldInitializer(irField.getReferenceKey(), instructions, isObjectInstanceField))
    }

    fun addMainFunctionWrapper(mainFunctionWrapper: IrFunctionSymbol) {
        wasmFileFragment.mainFunctionWrappers.add(mainFunctionWrapper.getReferenceKey())
    }

    fun addTestFunDeclarator(testFunctionDeclarator: IrFunctionSymbol) {
        wasmFileFragment.testFunctionDeclarators.add(testFunctionDeclarator.getReferenceKey())
    }

    fun addEquivalentFunction(key: String, function: IrFunctionSymbol) {
        wasmFileFragment.equivalentFunctions.add(key to function.getReferenceKey())
    }

    fun addClassAssociatedObjects(klass: IrClassSymbol, associatedObjectsGetters: List<AssociatedObjectBySymbols>) {
        val classAssociatedObjects = ClassAssociatedObjects(
            referenceTypeId(klass),
            associatedObjectsGetters.map { (obj, getter, isExternal) ->
                AssociatedObject(referenceTypeId(obj), getter.getReferenceKey(), isExternal)
            }
        )
        wasmFileFragment.classAssociatedObjectsInstanceGetters.add(classAssociatedObjects)
    }

    fun addJsModuleAndQualifierReferences(reference: JsModuleAndQualifierReference) {
        wasmFileFragment.jsModuleAndQualifierReferences.add(reference)
    }

    fun defineBuiltinIdSignatures(
        throwable: IrClassSymbol?,
        tryGetAssociatedObject: IrFunctionSymbol?,
        jsToKotlinAnyAdapter: IrFunctionSymbol?,
        unitGetInstance: IrFunctionSymbol?,
        runRootSuites: IrFunctionSymbol?,
    ) {
        if (throwable != null || tryGetAssociatedObject != null || jsToKotlinAnyAdapter != null || unitGetInstance != null || runRootSuites != null) {
            val originalSignatures = wasmFileFragment.builtinIdSignatures
            wasmFileFragment.builtinIdSignatures = BuiltinIdSignatures(
                throwable = originalSignatures?.throwable
                    ?: throwable?.getReferenceKey(),
                tryGetAssociatedObject = originalSignatures?.tryGetAssociatedObject
                    ?: tryGetAssociatedObject?.getReferenceKey(),
                jsToKotlinAnyAdapter = originalSignatures?.jsToKotlinAnyAdapter
                    ?: jsToKotlinAnyAdapter?.getReferenceKey(),
                unitGetInstance = originalSignatures?.unitGetInstance
                    ?: unitGetInstance?.getReferenceKey(),
                runRootSuites = originalSignatures?.runRootSuites
                    ?: runRootSuites?.getReferenceKey(),
            )
        }
    }

    val interfaceTableTypes: SpecialITableTypes by lazy {
        SpecialITableTypes().also {
            wasmFileFragment.specialITableTypes = it
        }
    }

    private val rttiElements: RttiElements by lazy {
        RttiElements().also {
            wasmFileFragment.rttiElements = it
        }
    }

    val rttiType: WasmSymbol<WasmStructDeclaration> get() = rttiElements.rttiType

    open fun defineRttiGlobal(global: WasmGlobal, irClass: IrClassSymbol, irSuperClass: IrClassSymbol?) {
        rttiElements.globals.add(
            RttiGlobal(
                global = global,
                classSignature = irClass.getReferenceKey(),
                superClassSignature = irSuperClass?.getReferenceKey()
            )
        )
    }

    fun referenceRttiGlobal(irClass: IrClassSymbol): WasmSymbol<WasmGlobal> =
        rttiElements.globalReferences.reference(irClass.getReferenceKey())
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

data class AssociatedObjectBySymbols(val klass: IrClassSymbol, val getter: IrFunctionSymbol, val isExternal: Boolean)