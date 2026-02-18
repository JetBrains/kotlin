/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.utils.redefinitionError
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

enum class WasmServiceImportExportKind(val prefix: String) {
    VTABLE($$"__vt$"),
    ITABLE($$"__it$"),
    RTTI($$"$__rt$"),
    FUNC($$"__fn$")
}

open class WasmFileCodegenContext(
    private val wasmFileFragment: WasmCompiledFileFragment,
    protected val idSignatureRetriever: IdSignatureRetriever,
) {
    open fun handleFunctionWithImport(declaration: IrFunctionSymbol): Boolean = false
    open fun handleVTableWithImport(declaration: IrClassSymbol): Boolean = false
    open fun handleClassITableWithImport(declaration: IrClassSymbol): Boolean = false
    open fun handleRTTIWithImport(declaration: IrClassSymbol, superType: IrClassSymbol?): Boolean = false
    open fun handleGlobalField(declaration: IrFieldSymbol): Boolean = false

    open fun needToBeDefinedGcType(declaration: IrClassSymbol): Boolean = true
    open fun needToBeDefinedFunctionType(declaration: IrFunctionSymbol): Boolean = true

    protected fun IrSymbol.getReferenceKey(): IdSignature =
        idSignatureRetriever.declarationSignature(this.owner as IrDeclaration)!!

    fun referenceConstantArray(resource: Pair<List<Long>, WasmType>): WasmSymbol<Int> =
        wasmFileFragment.constantArrayDataSegmentId.getOrPut(resource) { WasmSymbol() }

    fun addExport(wasmExport: WasmExport<*>) {
        wasmFileFragment.exports += wasmExport
    }

    private fun IrClassSymbol.getSignature(): IdSignature =
        idSignatureRetriever.declarationSignature(this.owner)!!

    open fun defineFunction(irFunction: IrFunctionSymbol, wasmFunction: WasmFunction) {
        if (wasmFileFragment.definedFunctions.put(irFunction.getReferenceKey(), wasmFunction) != null) {
            redefinitionError(irFunction.getReferenceKey(), "Functions")
        }
    }

    open fun defineGlobalField(irField: IrFieldSymbol, wasmGlobal: WasmGlobal) {
        if (wasmFileFragment.definedGlobalFields.put(irField.getReferenceKey(), wasmGlobal) != null) {
            redefinitionError(irField.getReferenceKey(), "GlobalFields")
        }
    }

    open fun defineGlobalVTable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        if (wasmFileFragment.definedGlobalVTables.put(irClass.getReferenceKey(), wasmGlobal) != null) {
            redefinitionError(irClass.getReferenceKey(), "GlobalVTables")
        }
    }

    open fun defineGlobalClassITable(irClass: IrClassSymbol, wasmGlobal: WasmGlobal) {
        if (wasmFileFragment.definedGlobalClassITables.put(irClass.getReferenceKey(), wasmGlobal) != null) {
            redefinitionError(irClass.getReferenceKey(), "GlobalClassITables")
        }
    }

    open fun defineRttiGlobal(global: WasmGlobal, irClass: IrClassSymbol, irSuperClass: IrClassSymbol?) {
        val reference = irClass.getReferenceKey()
        if (wasmFileFragment.definedRttiGlobal.put(reference, global) != null) {
            redefinitionError(reference, "RttiGlobal")
        }
        if (wasmFileFragment.definedRttiSuperType.put(reference, irSuperClass?.getReferenceKey()) != null) {
            redefinitionError(reference, "RttiSuperType")
        }
    }

    fun defineGcType(irClass: IrClassSymbol, wasmType: WasmTypeDeclaration) {
        if (wasmFileFragment.definedGcTypes.put(irClass.getReferenceKey(), wasmType) != null) {
            redefinitionError(irClass.getReferenceKey(), "GcTypes")
        }
    }

    fun defineVTableGcType(irClass: IrClassSymbol, wasmType: WasmStructDeclaration) {
        if (wasmFileFragment.definedVTableGcTypes.put(irClass.getReferenceKey(), wasmType) != null) {
            redefinitionError(irClass.getReferenceKey(), "VTableGcTypes")
        }
    }

    fun defineFunctionType(irFunction: IrFunctionSymbol, wasmFunctionType: WasmFunctionType) {
        if (wasmFileFragment.definedFunctionTypes.put(irFunction.getReferenceKey(), wasmFunctionType) != null) {
            redefinitionError(irFunction.getReferenceKey(), "FunctionTypes")
        }
    }

    open fun referenceFunction(irFunction: IrFunctionSymbol): FuncSymbol =
        FuncSymbol(irFunction.getReferenceKey())

    fun referenceGlobalField(irField: IrFieldSymbol): FieldGlobalSymbol =
        FieldGlobalSymbol(irField.getReferenceKey())

    open fun referenceGlobalVTable(irClass: IrClassSymbol): VTableGlobalSymbol =
        VTableGlobalSymbol(irClass.getReferenceKey())

    open fun referenceGlobalClassITable(irClass: IrClassSymbol): ClassITableGlobalSymbol =
        ClassITableGlobalSymbol(irClass.getReferenceKey())

    open fun referenceRttiGlobal(irClass: IrClassSymbol): RttiGlobalSymbol =
        RttiGlobalSymbol(irClass.getReferenceKey())

    fun referenceGlobalStringGlobal(value: String): LiteralGlobalSymbol {
        return LiteralGlobalSymbol(value).also {
            wasmFileFragment.globalLiterals.add(it)
        }
    }

    fun referenceGlobalStringId(referenceValue: String): WasmSymbol<Int> =
        wasmFileFragment.globalLiteralsId.getOrPut(referenceValue) { WasmSymbol() }

    fun referenceStringLiteralId(string: String): WasmSymbol<Int> =
        wasmFileFragment.stringLiteralId.getOrPut(string) { WasmSymbol() }

    open fun referenceGcType(irClass: IrClassSymbol): GcTypeSymbol =
        GcTypeSymbol(irClass.getReferenceKey())

    open fun referenceHeapType(irClass: IrClassSymbol): GcHeapTypeSymbol =
        GcHeapTypeSymbol(irClass.getReferenceKey())

    open fun referenceVTableGcType(irClass: IrClassSymbol): VTableTypeSymbol =
        VTableTypeSymbol(irClass.getReferenceKey())

    open fun referenceVTableHeapType(irClass: IrClassSymbol): VTableHeapTypeSymbol =
        VTableHeapTypeSymbol(irClass.getReferenceKey())

    open fun referenceFunctionType(irClass: IrFunctionSymbol): FunctionTypeSymbol =
        FunctionTypeSymbol(irClass.getReferenceKey())

    open fun referenceFunctionHeapType(irClass: IrFunctionSymbol): FunctionHeapTypeSymbol =
        FunctionHeapTypeSymbol(irClass.getReferenceKey())

    fun referenceTypeId(irClass: IrClassSymbol): Long =
        cityHash64(irClass.getSignature().toString().encodeToByteArray()).toLong()

    fun addJsFun(irFunction: IrFunctionSymbol, importName: WasmSymbol<String>, jsCode: String) {
        wasmFileFragment.jsFuns[irFunction.getReferenceKey()] =
            WasmCompiledModuleFragment.JsCodeSnippet(importName = importName, jsCode = jsCode)
    }

    fun addJsModuleImport(irFunction: IrFunctionSymbol, module: String) {
        wasmFileFragment.jsModuleImports[irFunction.getReferenceKey()] = module
    }

    fun addJsBuiltin(declarationName: String, polyfillImpl: String) {
        wasmFileFragment.jsBuiltinsPolyfills[declarationName] = polyfillImpl
    }

    open fun addObjectInstanceFieldInitializer(initializer: IrFunctionSymbol) {
        wasmFileFragment.objectInstanceFieldInitializers.add(initializer.getReferenceKey())
    }

    open fun addNonConstantFieldInitializers(initializer: IrFunctionSymbol) {
        wasmFileFragment.nonConstantFieldInitializers.add(initializer.getReferenceKey())
    }

    open fun addMainFunctionWrapper(mainFunctionWrapper: IrFunctionSymbol) {
        wasmFileFragment.mainFunctionWrappers.add(mainFunctionWrapper.getReferenceKey())
    }

    open fun addTestFunDeclarator(testFunctionDeclarator: IrFunctionSymbol) {
        wasmFileFragment.testFunctionDeclarators.add(testFunctionDeclarator.getReferenceKey())
    }

    open fun addEquivalentFunction(key: String, function: IrFunctionSymbol) {
        wasmFileFragment.equivalentFunctions.add(key to function.getReferenceKey())
    }

    open fun addClassAssociatedObjects(klass: IrClassSymbol, associatedObjectsGetters: List<AssociatedObjectBySymbols>) {
        val classAssociatedObjects = ClassAssociatedObjects(
            referenceTypeId(klass),
            associatedObjectsGetters.map { (obj, getter, isExternal) ->
                AssociatedObject(referenceTypeId(obj), getter.getReferenceKey(), isExternal)
            }
        )
        wasmFileFragment.classAssociatedObjectsInstanceGetters.add(classAssociatedObjects)
    }

    open fun addJsModuleAndQualifierReferences(reference: JsModuleAndQualifierReference) {
        wasmFileFragment.jsModuleAndQualifierReferences.add(reference)
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

data class AssociatedObjectBySymbols(val klass: IrClassSymbol, val getter: IrFunctionSymbol, val isExternal: Boolean)