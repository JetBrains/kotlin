/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import com.intellij.util.containers.reverse
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.backend.common.serialization.cityHash128
import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.backend.wasm.*
import org.jetbrains.kotlin.backend.wasm.utils.fitsLatin1
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.wasm.WasmSpecialExportNames
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import java.util.*

enum class ExceptionTagType { WASM_TAG, JS_TAG, TRAP, WASM_SHARED_ANYREF }

class WasmCompiledModuleFragment(
    private val wasmCompiledFileFragments: List<WasmCompiledFileFragment>,
    private val isWasmJsTarget: Boolean,
) {
    // Used during linking
    private val serviceCodeLocation = SourceLocation.NoLocation("Generated service code")

    private val stringDataSectionIndex = WasmImmediate.DataIdx(0)
    private val stringAddressesAndLengthsIndex = WasmImmediate.DataIdx(1)

    private val wasmCompiledCodeFileFragments: List<WasmCompiledCodeFileFragment> =
        wasmCompiledFileFragments.filterIsInstance<WasmCompiledCodeFileFragment>()

    private inline fun forEachLinkerData(body: (WasmCompiledLinkerDataFileFragment) -> Unit) {
        wasmCompiledCodeFileFragments.forEach { fragment ->
            body(fragment.linkerData)
        }
    }

    class JsCodeSnippet(val importName: WasmSymbol<String>, val jsCode: String)

    private fun partitionDefinedAndImportedFunctions(definedDeclarations: DefinedDeclarationsResolver): Pair<MutableList<WasmFunction.Defined>, MutableList<WasmFunction.Imported>> {
        val definedFunctions = mutableListOf<WasmFunction.Defined>()
        val importedFunctions = mutableListOf<WasmFunction.Imported>()
        definedDeclarations.functions.values.distinct().forEach { function ->
            when (function) {
                is WasmFunction.Defined -> definedFunctions.add(function)
                is WasmFunction.Imported -> importedFunctions.add(function)
            }
        }
        return definedFunctions to importedFunctions
    }

    private fun createAndExportServiceFunctions(
        definedDeclarations: DefinedDeclarationsResolver,
        stringEntities: StringLiteralWasmEntities,
        stringPoolSize: Int,
        stringPoolSizeWithGlobals: Int,
        wasmElements: MutableList<WasmElement>,
        exports: MutableList<WasmExport<*>>,
        wasmCommandModuleInitialization: Boolean,
        emitHotRuntime: Boolean,
    ) {
        stringAddressesAndLengthsField(
            definedDeclarations = definedDeclarations,
        )

        createFieldInitializerFunction(definedDeclarations = definedDeclarations, stringPoolSize = stringPoolSize)

        createAssociatedObjectGetter(definedDeclarations = definedDeclarations, wasmElements = wasmElements)

        val mainFunctionsExportName = if (wasmCommandModuleInitialization)
            WasmSpecialExportNames.INITIALIZE
        else
            WasmSpecialExportNames.START

        val mainFunctions = selectMainFunctions()
        // Hot mode is REACTOR-STYLE: `_initialize` is the field-init export,
        // and exporting `_start` alongside it violates the convention (a
        // command has _start; a reactor has _initialize) — e.g. Node's WASI
        // refuses either API when both exist. Main is still exported, under
        // the neutral `main`, for hosts that want to run it explicitly.
        val mainExportName = when {
            emitHotRuntime -> "main"
            else -> mainFunctionsExportName
        }
        mainFunctions.forEach { mainFunction ->
            val mainFunction = definedDeclarations.resolve(FuncSymbol(mainFunction))
            exports.add(WasmExport.Function(mainExportName, mainFunction))
        }

        createStringPoolField(definedDeclarations, stringPoolSizeWithGlobals)

        if (isWasmJsTarget) {
            createStringLiteralFunction(
                definedDeclarations = definedDeclarations,
                stringEntities = stringEntities,
                wasmElements = wasmElements,
                stringLiteralType = StringLiteralType.JsString,
            )
        }

        createStringLiteralFunction(
            definedDeclarations = definedDeclarations,
            stringEntities = stringEntities,
            wasmElements = wasmElements,
            stringLiteralType = StringLiteralType.Latin1,
        )

        createStringLiteralFunction(
            definedDeclarations = definedDeclarations,
            stringEntities = stringEntities,
            wasmElements = wasmElements,
            stringLiteralType = StringLiteralType.Utf16,
        )

        createStartUnitTestsFunction(definedDeclarations, exports)
    }

    private class StringLiteralWasmEntities(
        val kotlinStringType: WasmType,
        val wasmCharArrayType: WasmType,
        val wasmCharArrayDeclaration: IdSignature,
    )

    private fun getStringLiteralWasmEntities(
        definedDeclarations: DefinedDeclarationsResolver,
    ): StringLiteralWasmEntities {
        val createStringFunction = definedDeclarations.functions[Synthetics.Functions.createStringBuiltIn.value]
            ?: compilationException("kotlin.createString is not found in fragments", null)

        val createStringFunctionTypeSignature = (createStringFunction.type as FunctionHeapTypeSymbol).type
        val createStringFunctionType = definedDeclarations.functionTypes.getValue(createStringFunctionTypeSignature)
        val kotlinStringType = createStringFunctionType.resultTypes[0]
        val wasmCharArrayType = createStringFunctionType.parameterTypes[0]
        val wasmCharArrayDeclaration = (wasmCharArrayType.getHeapType() as GcHeapTypeSymbol).type
        val wasmStringArrayDeclaration =
            WasmArrayDeclaration("string_array", WasmStructFieldDeclaration("string", kotlinStringType, true))
        definedDeclarations.gcTypes[Synthetics.GcTypes.wasmStringArrayType.value] = wasmStringArrayDeclaration

        val newStringLiteralFunctionType = WasmFunctionType(listOf(WasmI32), listOf(kotlinStringType))
        definedDeclarations.functionTypes[Synthetics.FunctionHeapTypes.stringLiteralFunctionType.type] = newStringLiteralFunctionType

        val newStringLiteralJsFunctionType = WasmFunctionType(listOf(WasmI32, WasmRefType(WasmHeapType.Simple.Extern)), listOf(kotlinStringType))
        definedDeclarations.functionTypes[Synthetics.FunctionHeapTypes.jsStringLiteralFunctionType.type] = newStringLiteralJsFunctionType

        return StringLiteralWasmEntities(
            kotlinStringType = kotlinStringType,
            wasmCharArrayType = wasmCharArrayType,
            wasmCharArrayDeclaration = wasmCharArrayDeclaration,
        )
    }

    fun linkWasmCompiledFragments(
        multimoduleOptions: MultimoduleCompileOptions?,
        exceptionTagType: ExceptionTagType,
        wasmCommandModuleInitialization: Boolean,
        useStackSwitching: Boolean,
        importWasmMemoryInsteadOfExport: Boolean,
        emitHotRuntime: Boolean = false,
    ): WasmModule {
        val definedDeclarations = getDefinedDeclarationsFromFragments()

        val isStdlibOrMonolith = multimoduleOptions?.stdlibModuleNameForImport == null

        val data = mutableListOf<WasmData>()
        val stringPoolSize = bindStringPoolSymbolsAndGetSize(data)
        val stringPoolSizeWithGlobals = bindGlobalLiterals(definedDeclarations, stringPoolSize)

        bindConstantArrayDataSegmentIds(data)

        val exports = mutableListOf<WasmExport<*>>()
        forEachLinkerData {
            exports.addAll(it.exports)
        }

        val memories = createAndExportMemory(
            importWasmMemoryInsteadOfExport = importWasmMemoryInsteadOfExport,
            isStdlibOrMonolith = isStdlibOrMonolith,
            definedDeclarations = definedDeclarations,
            exports = exports,
        )
        val [importedMemories, definedMemories] = memories.partition { it.importPair != null }

        val parameterlessNoReturnFunctionType = WasmFunctionType(emptyList(), emptyList())
        definedDeclarations.functionTypes[Synthetics.FunctionHeapTypes.parameterlessNoReturnFunctionType.type] = parameterlessNoReturnFunctionType

        val stringEntities = getStringLiteralWasmEntities(definedDeclarations)

        createAndBindSpecialITableTypes(definedDeclarations)
        createAndBindRttiTypeDeclaration(definedDeclarations)

        val elements = getUsedAsWasmRawFunctionReference(definedDeclarations)

        // The fdefn cells (DESIGN §16): collect this module's reloadable
        // bindings and class LAYOUTS from the klib fragments' linker data,
        // then register the machinery the master init needs — the
        // canonical cell struct type, the resolve helper, the exported
        // cell accessors, and the per-class layout registrations (the
        // generic migration/slot-value machinery lives in the stdlib).
        // Nothing here is coordinated with previous generations: the
        // names and layouts are this module's own description, resolved
        // against the image at load.
        val hotReloadCellBindings = mutableListOf<Triple<String, IdSignature, IdSignature>>()
        // every literal the hot-reload init materializes: binding names
        // (codegen registered one channel half per target — the imported
        // JS-string constant on wasm-js, the string pool on wasm-wasi)
        val hotReloadLiteralIds = mutableMapOf<String, WasmSymbol<Int>>()
        val hotLayoutBindings = mutableListOf<HotLayoutBinding>()
        if (emitHotRuntime) {
            forEachLinkerData { linkerData ->
                hotReloadCellBindings.addAll(linkerData.hotReloadCellBindings)
                if (!isStdlibOrMonolith) {
                    // the STDLIB BASE registers none: its classes' shapes
                    // are recomputable from current IR and never migrate,
                    // and every delta re-registers its own (identical
                    // content — registration is idempotent)
                    hotLayoutBindings.addAll(linkerData.hotLayoutBindings)
                }
                for ([name, id] in linkerData.globalLiteralsId) {
                    hotReloadLiteralIds.putIfAbsent(name, id)
                }
                for ([name, id] in linkerData.stringLiteralId) {
                    hotReloadLiteralIds.putIfAbsent(name, id)
                }
            }
            if (hotReloadCellBindings.isNotEmpty()) {
                createFdefnMachinery(definedDeclarations)
            }

        }

        createAndExportServiceFunctions(
            definedDeclarations = definedDeclarations,
            stringEntities = stringEntities,
            stringPoolSize = stringPoolSize,
            stringPoolSizeWithGlobals = stringPoolSizeWithGlobals,
            wasmElements = elements,
            exports = exports,
            wasmCommandModuleInitialization = wasmCommandModuleInitialization,
            emitHotRuntime = emitHotRuntime,
        )

        val masterInitFunction = createMasterInitFunction(
            definedDeclarations = definedDeclarations,
            hotReloadCellBindings = hotReloadCellBindings,
            hotReloadLiteralIds = hotReloadLiteralIds,
            hotLayoutBindings = hotLayoutBindings,
        )

        val globals = getGlobals(definedDeclarations)

        if (useStackSwitching) {
            createContTypes(definedDeclarations, isStdlibOrMonolith)
        }

        val tags = getTags(
            definedDeclarations = definedDeclarations,
            exceptionTagType = exceptionTagType,
            generateStackSwitchingTag =
                useStackSwitching && isStdlibOrMonolith,
        )

        val [importedTags, definedTags] = tags.partition { it.importPair != null }

        val [importedGlobals, definedGlobals] = globals.partition { it.importPair != null }

        val [definedFunctions, importedFunctions] = partitionDefinedAndImportedFunctions(definedDeclarations)

        val importsInOrder = mutableListOf<WasmNamedModuleField>()
        importsInOrder.addAll(importedFunctions)
        importsInOrder.addAll(importedTags)
        importsInOrder.addAll(importedGlobals)
        importsInOrder.addAll(importedMemories)

        val recursiveTypeGroups = getTypes(definedDeclarations)

        // Hot reload: the registry section is the image's COMPLETE
        // Hot reload: reactor-style output — no start section; init is
        // exported as _initialize so the host controls instantiate ->
        // init -> sweep (and a rejected patch never runs its init).
        if (emitHotRuntime) {
            exports.add(
                WasmExport.Function(
                    field = masterInitFunction,
                    name = WasmSpecialExportNames.INITIALIZE,
                )
            )
        }

        return WasmModule(
            resolver = definedDeclarations,
            recGroups = recursiveTypeGroups,
            importsInOrder = importsInOrder,
            importedFunctions = importedFunctions,
            importedMemories = importedMemories,
            definedFunctions = definedFunctions,
            importedTags = importedTags,
            tables = emptyList(),
            memories = definedMemories,
            globals = definedGlobals,
            importedGlobals = importedGlobals,
            exports = exports,
            startFunction = if (emitHotRuntime) null else masterInitFunction,
            elements = elements,
            data = data,
            dataCount = true,
            tags = definedTags,
            customSections = emptyList(),
        ).apply { calculateIds() }
    }

    private fun createAndBindSpecialITableTypes(
        definedDeclarations: DefinedDeclarationsResolver,
    ) {
        val wasmAnyArrayType = WasmArrayDeclaration(
            name = "AnyArray",
            field = WasmStructFieldDeclaration("", WasmRefNullType(WasmHeapType.Simple.Any), false)
        )
        definedDeclarations.gcTypes[Synthetics.GcTypes.wasmAnyArrayType.value] = wasmAnyArrayType

        val specialSlotITableTypeSlots = mutableListOf<WasmStructFieldDeclaration>()
        val wasmAnyRefStructField = WasmStructFieldDeclaration("", WasmAnyRef, false)
        repeat(WasmBackendContext.SPECIAL_INTERFACE_TABLE_SIZE) {
            specialSlotITableTypeSlots.add(wasmAnyRefStructField)
        }
        specialSlotITableTypeSlots.add(
            WasmStructFieldDeclaration(
                name = "",
                type = WasmRefNullType(Synthetics.HeapTypes.wasmAnyArrayType),
                isMutable = false
            )
        )
        val specialSlotITableType = WasmStructDeclaration(
            name = "SpecialITable",
            fields = specialSlotITableTypeSlots,
            superType = null,
            isFinal = true
        )
        definedDeclarations.gcTypes[Synthetics.GcTypes.specialSlotITableType.value] = specialSlotITableType
    }

    private fun getTags(
        definedDeclarations: DefinedDeclarationsResolver,
        exceptionTagType: ExceptionTagType,
        generateStackSwitchingTag: Boolean,
    ): List<WasmTag> {
        val exceptionTag = when (exceptionTagType) {
            ExceptionTagType.TRAP -> null
            ExceptionTagType.JS_TAG -> {
                val jsExceptionTagFuncType = WasmFunctionType(
                    parameterTypes = listOf(WasmExternRef),
                    resultTypes = emptyList()
                )
                definedDeclarations.functionTypes[Synthetics.FunctionHeapTypes.jsExceptionTagFuncType.type] = jsExceptionTagFuncType
                WasmTag(Synthetics.FunctionHeapTypes.jsExceptionTagFuncType, WasmImportDescriptor("intrinsics", WasmSymbol("tag")))
            }
            ExceptionTagType.WASM_TAG -> {
                val tagFuncType = WasmRefNullType(Synthetics.HeapTypes.throwableBuiltInType)

                val throwableTagFuncType = WasmFunctionType(
                    parameterTypes = listOf(tagFuncType),
                    resultTypes = emptyList()
                )
                definedDeclarations.functionTypes[Synthetics.FunctionHeapTypes.jsExceptionTagFuncType.type] = throwableTagFuncType
                WasmTag(Synthetics.FunctionHeapTypes.jsExceptionTagFuncType)
            }
            // Hot reload (wasi): one host-created anyref-typed tag shared by
            // every generation, so cross-generation catch works. Kotlin
            // Throwables flow through untyped; catch sites cast back.
            ExceptionTagType.WASM_SHARED_ANYREF -> {
                val tagFuncType = WasmFunctionType(
                    parameterTypes = listOf(WasmAnyRef),
                    resultTypes = emptyList()
                )
                definedDeclarations.functionTypes[Synthetics.FunctionHeapTypes.jsExceptionTagFuncType.type] = tagFuncType
                WasmTag(Synthetics.FunctionHeapTypes.jsExceptionTagFuncType, WasmImportDescriptor("intrinsics", WasmSymbol("tag")))
            }
        }

        val contTagType = if (generateStackSwitchingTag) {
            val kotlinAnyRefType = WasmRefNullType(Synthetics.HeapTypes.anyBuiltInType)
            val contTagFuncType = WasmFunctionType(listOf(kotlinAnyRefType), listOf())
            definedDeclarations.functionTypes[Synthetics.FunctionHeapTypes.wasmContTagFuncType.type] = contTagFuncType
            WasmTag(Synthetics.FunctionHeapTypes.wasmContTagFuncType)
        } else null

        return listOfNotNull(exceptionTag, contTagType)
    }

    private fun createContTypes(definedDeclarations: DefinedDeclarationsResolver, isStdlibOrMonolith: Boolean) {
        val boundContFuncType = WasmFunctionType(emptyList(), listOf(WasmRefNullType(Synthetics.HeapTypes.anyBuiltInType)))
        definedDeclarations.functionTypes[Synthetics.FunctionHeapTypes.boundContFuncType.type] = boundContFuncType
        definedDeclarations.gcTypes[Synthetics.HeapTypes.boundContType.type] =
            WasmContType("cont_0", Synthetics.FunctionHeapTypes.boundContFuncType)

        if (isStdlibOrMonolith) {
            // Continuation types of `SuspendFunction{0,1,2}.invoke`
            for (arity in 0..2) {
                val funType = suspendFunctionInvokeWasmType(arity)
                val funTypeSignature = getFunctionTypeSignature(funType)
                definedDeclarations.functionTypes.putIfAbsent(funTypeSignature, funType)
                definedDeclarations.gcTypes[getContTypeSignature(funType)] =
                    WasmContType("cont_${arity + 2}", FunctionHeapTypeSymbol(funTypeSignature))
            }
        }
    }

    private fun getTypes(definedDeclarations: DefinedDeclarationsResolver): List<RecursiveTypeGroup> {

        val allFunctionTypes = definedDeclarations.functionTypes
        val reversedFunctionTypeMap = allFunctionTypes.reverse()
        //Rebind all function types to canonical
        for (functionType in allFunctionTypes) {
            val canonicalSignature = reversedFunctionTypeMap.getValue(functionType.value)
            if (functionType.key != canonicalSignature) {
                val canonicalType = allFunctionTypes.getValue(canonicalSignature)
                allFunctionTypes[functionType.key] = canonicalType
            }
        }

        val heapTypeResolver: (WasmHeapType.Type) -> WasmTypeDeclaration = definedDeclarations::resolve

        val recursiveGroups = with(RecursiveGroupBuilder(heapTypeResolver)) {
            addTypes(definedDeclarations.gcTypes.values.toSet())
            addTypes(definedDeclarations.vTableGcTypes.values.toSet())
            addTypes(allFunctionTypes.values.toSet())
            build()
        }

        val gcTypesReversed = IdentityHashMap<WasmTypeDeclaration, IdSignature>()
        for (gcType in definedDeclarations.gcTypes) {
            gcTypesReversed[gcType.value] = gcType.key
        }

        val vtTypeReversed = IdentityHashMap<WasmTypeDeclaration, IdSignature>()
        for (vtableType in definedDeclarations.vTableGcTypes) {
            vtTypeReversed[vtableType.value] = vtableType.key
        }

        val vtableSeed = Hash128Bits(0xc3a5c85c97cb3127U, 0xb492b66fbe98f273U)
        val getStableId: (WasmStructDeclaration) -> Hash128Bits = fun(declaration: WasmStructDeclaration): Hash128Bits {
            val gcType = gcTypesReversed[declaration]
            val signatureString = (gcType ?: vtTypeReversed.getValue(declaration)).toString()
            val signatureHash = cityHash128(signatureString.toByteArray())
            return if (gcType != null) signatureHash else vtableSeed.combineWith(signatureHash)
        }

        recursiveGroups.forEach { group ->
            val single = group.singleOrNull()
            if (single != null && single !is WasmStructDeclaration) {
                return@forEach
            }

            canonicalStableSort(group, heapTypeResolver, getStableId)

            val firstGroupGcTypeSignature = group.firstNotNullOfOrNull {
                gcTypesReversed[it]
            }

            if (firstGroupGcTypeSignature != null) {
                val mixin64BitIndex = firstGroupGcTypeSignature.toString().cityHash64().toULong()

                val mixIn = WasmStructDeclaration(
                    name = "mixin_type",
                    fields = encodeIndex(mixin64BitIndex),
                    superType = null,
                    isFinal = true
                )
                group.add(mixIn)
            }
        }

        return recursiveGroups
    }

    private fun createAndBindRttiTypeDeclaration(
        definedDeclarations: DefinedDeclarationsResolver,
    ) {
        val wasmLongArray = WasmArrayDeclaration("LongArray", WasmStructFieldDeclaration("Long", WasmI64, false))
        definedDeclarations.gcTypes[Synthetics.GcTypes.wasmLongArray.value] = wasmLongArray

        val stringLiteralFunctionRef = WasmRefNullType(
            if (isWasmJsTarget)
                Synthetics.FunctionHeapTypes.jsStringLiteralFunctionType
            else
                Synthetics.FunctionHeapTypes.stringLiteralFunctionType
        )

        val fieldsList = mutableListOf(
            WasmStructFieldDeclaration("implementedIFaceIds", WasmRefNullType(Synthetics.HeapTypes.wasmLongArray), false),
            WasmStructFieldDeclaration("superClassRtti", WasmRefNullType(Synthetics.HeapTypes.rttiType), false),            WasmStructFieldDeclaration("packageNamePoolId", WasmI32, false),
            WasmStructFieldDeclaration("simpleNamePoolId", WasmI32, false),
            WasmStructFieldDeclaration("klassId", WasmI64, false),
            WasmStructFieldDeclaration("typeInfoFlag", WasmI32, false),
            WasmStructFieldDeclaration("qualifierStringLoader", stringLiteralFunctionRef, false),
            WasmStructFieldDeclaration("simpleNameStringLoader", stringLiteralFunctionRef, false),
        )
        if (isWasmJsTarget) {
            fieldsList.add(WasmStructFieldDeclaration("packageNameGlobal", WasmRefType(WasmHeapType.Simple.Extern), false))
            fieldsList.add(WasmStructFieldDeclaration("simpleNameGlobal", WasmRefType(WasmHeapType.Simple.Extern), false))
        }
        val rttiTypeDeclaration = WasmStructDeclaration(
            name = "RTTI",
            fields = fieldsList,
            superType = null,
            isFinal = true
        )

        definedDeclarations.gcTypes[Synthetics.GcTypes.rttiType.value] = rttiTypeDeclaration
    }

    private fun getUsedAsWasmRawFunctionReference(definedDeclarations: DefinedDeclarationsResolver) = mutableListOf<WasmElement>().apply {
        // WebAssembly requires functions used for value via ref.func to be forward declared.
        forEachLinkerData { linkerData ->
            addAll(linkerData.wasmReferencedFunctions.map { key ->
                WasmElement(
                    type = WasmFuncRef,
                    values = listOf(WasmTable.Value.Function(definedDeclarations.resolve(FuncSymbol(key)))),
                    mode = WasmElement.Mode.Declarative)
            })
        }
    }

    private fun getGlobals(definedDeclarations: DefinedDeclarationsResolver) = mutableSetOf<WasmGlobal>().apply {
        addAll(definedDeclarations.globalFields.values)
        addAll(definedDeclarations.globalVTables.values)
        addAll(definedDeclarations.globalClassITables.values)

        val rttiGlobals = definedDeclarations.globalRTTI
        val rttiSuperTypes = mutableMapOf<IdSignature, IdSignature?>()

        wasmCompiledFileFragments.forEach { fragment ->
            rttiGlobals.putAll(fragment.definedDeclarations.definedRttiGlobal)
            rttiSuperTypes.putAll(fragment.definedDeclarations.definedRttiSuperType)
        }

        fun wasmRttiGlobalOrderKey(superType: IdSignature?): Int =
            superType?.let { wasmRttiGlobalOrderKey(rttiSuperTypes[it]) + 1 } ?: 0

        rttiGlobals.keys.sortedBy(::wasmRttiGlobalOrderKey).forEach { add(rttiGlobals[it]!!) }

        addAll(definedDeclarations.globalLiteralGlobals.values)
    }

    private fun createAndExportMemory(
        importWasmMemoryInsteadOfExport: Boolean,
        isStdlibOrMonolith: Boolean,
        definedDeclarations: DefinedDeclarationsResolver,
        exports: MutableList<WasmExport<*>>,
    ): List<WasmMemory> {
        if (!isStdlibOrMonolith) return emptyList()

        val memoryImport = importWasmMemoryInsteadOfExport.ifTrue {
            WasmImportDescriptor("intrinsics", WasmSymbol("memory"))
        }

        val memorySizeInPages = 0
        val memory = WasmMemory(WasmLimits(memorySizeInPages.toUInt(), null/* "unlimited" */), memoryImport)

        if (importWasmMemoryInsteadOfExport) {
            val memoryGlobal = WasmGlobal("memory", WasmExternRef, false, emptyList(), memoryImport)
            definedDeclarations.globalFields[Synthetics.Globals.wasmMemoryGlobal.value] = memoryGlobal
        } else {
            // Need to export the memory in order to pass complex objects to the host language.
            // Export name "memory" is a WASI ABI convention.
            val exportMemory = WasmExport.Memory(WasmSpecialExportNames.MEMORY, memory)
            exports.add(exportMemory)
        }
        return listOf(memory)
    }

    /** The fdefn era (DESIGN §16): the canonical cell struct type every
     *  hot module carries. The struct is STANDALONE in the type
     *  section: a rec group is the canonicalization unit and this type
     *  references nothing local, so standalone declarations unify
     *  across modules. */
    private fun createFdefnMachinery(
        definedDeclarations: DefinedDeclarationsResolver,
    ) {
        definedDeclarations.gcTypes[HotReloadPayloads.FDEFN_TYPE] = WasmStructDeclaration(
            name = "fdefn",
            fields = listOf(WasmStructFieldDeclaration("fn", WasmFuncRef, isMutable = true)),
            superType = null,
            isFinal = false,
        )
    }

    private fun createMasterInitFunction(
        definedDeclarations: DefinedDeclarationsResolver,
        hotReloadCellBindings: List<Triple<String, IdSignature, IdSignature>> = emptyList(),
        hotReloadLiteralIds: Map<String, WasmSymbol<Int>> = emptyMap(),
        hotLayoutBindings: List<HotLayoutBinding> = emptyList(),
    ): WasmFunction.Defined {
        val masterInitFunction = WasmFunction.Defined("_initializeModule", Synthetics.FunctionHeapTypes.parameterlessNoReturnFunctionType)
        with(WasmExpressionBuilder(masterInitFunction.instructions)) {
            /** text -> kotlin.String, exactly the way every string literal
             *  lowers per target: wasm-js passes (id, imported JS-string
             *  constant) to the js-string loader; wasm-wasi passes just the
             *  string-pool id (the loader reads its own address+length). */
            fun buildString(text: String, literalIds: Map<String, WasmSymbol<Int>>) {
                val poolId = literalIds[text]
                    ?: error("hot reload: no string-literal id for \"${text.take(40)}\"")
                buildConstI32Symbol(poolId, serviceCodeLocation)
                if (isWasmJsTarget) {
                    buildInstr(
                        WasmOp.GLOBAL_GET, serviceCodeLocation,
                        LiteralGlobalSymbol(text),
                    )
                    buildCall(FuncSymbol(Synthetics.Functions.createStringLiteralJsString.value), serviceCodeLocation)
                } else {
                    val loader =
                        if (text.fitsLatin1) Synthetics.Functions.createStringLiteralLatin1
                        else Synthetics.Functions.createStringLiteralUtf16
                    buildCall(FuncSymbol(loader.value), serviceCodeLocation)
                }
            }

            // LAYOUTS FIRST: every allocation of a hot class reads the
            // module's layout global (the fresh payload's `$layout`),
            // and the val/object
            // initializers below allocate — so the layout objects must
            // exist before they run. Safe before the string pool and the
            // cells: the registration calls carry no string literals
            // (class-id constants + ref.funcs), and the cell globals hold
            // their own impls from instantiation (GENESIS).
            for (binding in hotLayoutBindings) {
                // register the class's CURRENT definition (the dispatch halves —
                // the class's CURRENT vtable and itable, read at
                // instantiation-initialized globals — then the allocate
                // closure; the slots one by one after; the register IS
                // the set-current: load is apply, the newest definition
                // in the image is live) and store the returned LAYOUT
                // OBJECT into the module's per-class layout global:
                // every allocation of the class reads it, stamping it
                // into the fresh payload's `$layout`
                buildConstI64(binding.classId, serviceCodeLocation)
                buildGetGlobal(VTableGlobalSymbol(binding.classKey!!), serviceCodeLocation)
                if (binding.hasItable) {
                    buildGetGlobal(ClassITableGlobalSymbol(binding.classKey!!), serviceCodeLocation)
                } else {
                    buildRefNull(WasmHeapType.Simple.None, serviceCodeLocation)
                }
                buildInstr(WasmOp.REF_FUNC, serviceCodeLocation, FuncSymbol(binding.allocateKey))
                buildCall(FuncSymbol(Synthetics.Functions.hotLayoutsRegisterBuiltIn.value), serviceCodeLocation)
                buildInstr(
                    WasmOp.GLOBAL_SET, serviceCodeLocation,
                    FieldGlobalSymbol(HotReloadPayloads.layoutGlobalKey(binding.classId)),
                )
                for (slot in binding.readerKeys.indices) {
                    buildConstI64(binding.classId, serviceCodeLocation)
                    buildConstI64(binding.tags[slot * 2], serviceCodeLocation)
                    buildConstI64(binding.tags[slot * 2 + 1], serviceCodeLocation)
                    buildInstr(WasmOp.REF_FUNC, serviceCodeLocation, FuncSymbol(binding.readerKeys[slot]))
                    buildInstr(WasmOp.REF_FUNC, serviceCodeLocation, FuncSymbol(binding.writerKeys[slot]))
                    buildCall(FuncSymbol(Synthetics.Functions.hotLayoutsAddSlotBuiltIn.value), serviceCodeLocation)
                }
            }

            // _fieldInitialize next: it populates the string POOL
            // ($_addressesAndLengths starts null — wasm-wasi's binding-name
            // literals read it), and runs the val/object initializers. Those
            // initializers may already call this module's bindings — GENESIS
            // covers that window: every cell global holds its own impl from
            // instantiation, so pre-resolution calls run the same bodies the
            // resolution is about to install.
            buildCall(Synthetics.Functions.fieldInitializerFunction, serviceCodeLocation)

            // LOAD IS APPLY: resolve every binding's cell by name and
            // install THIS module's impl — first definition and
            // redefinition are the same instruction sequence, and the
            // newest definition in the image is live.
            for (binding in hotReloadCellBindings) {
                buildString(binding.first, hotReloadLiteralIds)
                buildInstr(WasmOp.REF_FUNC, serviceCodeLocation, FuncSymbol(binding.second))
                buildCall(FuncSymbol(Synthetics.Functions.hotFdefnsResolveBuiltIn.value), serviceCodeLocation)
                // the stdlib resolve returns anyref; the cell global is
                // the raw fdefn struct — the cast is the honest check
                buildRefCastNullStatic(GcHeapTypeSymbol(HotReloadPayloads.FDEFN_TYPE), serviceCodeLocation)
                buildInstr(
                    WasmOp.GLOBAL_SET, serviceCodeLocation,
                    FieldGlobalSymbol(binding.third),
                )
            }

            // The image's CLASS LAYOUTS (the layout era): build this
            // module's layout structs from the linker data — the slot
            // descriptor arrays (interleaved name/type tags) plus the
            // reader/writer/allocate funcrefs the codegen emitted — and
            // register each into the image's table, then declare the
            // class's CURRENT layout (load is apply). The STDLIB BASE
            // registers none: its classes' shapes are recomputable from
            // current IR and never migrate, and every delta re-registers
            // its own (a re-registration of a known shape is identical).

            if (definedDeclarations.functions.containsKey(Synthetics.Functions.associatedObjectGetter.value)) {
                buildInstr(WasmOp.REF_FUNC, serviceCodeLocation, Synthetics.Functions.associatedObjectGetter)
                buildInstr(WasmOp.STRUCT_NEW, serviceCodeLocation, Synthetics.GcTypes.associatedObjectGetterWrapper)
                buildCall(Synthetics.Functions.registerModuleDescriptorBuiltIn, serviceCodeLocation)
            }

            buildInstr(WasmOp.RETURN, serviceCodeLocation)
        }
        definedDeclarations.functions[Synthetics.Functions.masterInitFunction.value] = masterInitFunction
        return masterInitFunction
    }

    // TODO: Gets main functions which should be run on start. Basically it should single, but we have no way to decide.
    private fun selectMainFunctions(): List<IdSignature> {
        var mainFunctionId: String? = null
        val mainFunctionSignatures = mutableListOf<IdSignature>()
        forEachLinkerData { linkerData ->
            linkerData.mainFunctionWrappers.forEach { (val fqName, val signature = function) ->
                val currentId = mainFunctionId ?: fqName
                if (mainFunctionId == null || fqName < currentId) {
                    mainFunctionId = fqName
                    mainFunctionSignatures.clear()
                    mainFunctionSignatures.add(signature)
                } else if (fqName == currentId) {
                    mainFunctionSignatures.add(signature)
                }
            }
        }
        return mainFunctionSignatures
    }

    private fun createAssociatedObjectGetter(
        definedDeclarations: DefinedDeclarationsResolver,
        wasmElements: MutableList<WasmElement>,
    ) {
        // If AO accessor removed by DCE - we do not need it then
        if (!definedDeclarations.functions.containsKey(Synthetics.Functions.tryGetAssociatedObjectBuiltIn.value)) return
        val nullableAnyWasmType = WasmRefNullType(Synthetics.HeapTypes.anyBuiltInType)
        val associatedObjectGetterType = WasmFunctionType(listOf(WasmI64, WasmI64), listOf(nullableAnyWasmType))
        definedDeclarations.functionTypes[Synthetics.FunctionHeapTypes.associatedObjectGetterType.type] = associatedObjectGetterType

        val classIdLocal = WasmLocal(0, "classId", WasmI64, true)
        val keyIdLocal = WasmLocal(1, "keyId", WasmI64, true)
        val associatedObjectGetter = WasmFunction.Defined(
            name = "_associatedObjectGetter",
            type = Synthetics.FunctionHeapTypes.associatedObjectGetterType,
            locals = mutableListOf(classIdLocal, keyIdLocal)
        )
        definedDeclarations.functions[Synthetics.Functions.associatedObjectGetter.value] = associatedObjectGetter

        // Make this function possible to func.ref
        wasmElements.add(
            WasmElement(
                type = WasmFuncRef,
                values = listOf(WasmTable.Value.Function(WasmSymbol(associatedObjectGetter))),
                mode = WasmElement.Mode.Declarative
            )
        )

        associatedObjectGetter.instructions.clear()
        with(WasmExpressionBuilder(associatedObjectGetter.instructions)) {
            forEachLinkerData { linkerData ->
                for ((val klassId = klass, val associatedObjectsInstanceGetters = objects) in linkerData.classAssociatedObjectsInstanceGetters) {
                    buildGetLocal(classIdLocal, serviceCodeLocation)
                    buildConstI64(klassId, serviceCodeLocation)
                    buildInstr(WasmOp.I64_EQ, serviceCodeLocation)
                    buildIf("Class matches")
                    associatedObjectsInstanceGetters.forEach { (val keyId = obj, val getter = getterFunc, val isExternal) ->
                        if (definedDeclarations.functions.containsKey(getter)) { //Could be deleted with DCE
                            buildGetLocal(keyIdLocal, serviceCodeLocation)
                            buildConstI64(keyId, serviceCodeLocation)
                            buildInstr(WasmOp.I64_EQ, serviceCodeLocation)
                            buildIf("Object matches")
                            buildCall(getter, serviceCodeLocation)
                            if (isExternal) {
                                buildCall(Synthetics.Functions.jsToKotlinAnyAdapterBuiltIn, serviceCodeLocation)
                            }
                            buildInstr(WasmOp.RETURN, serviceCodeLocation)
                            buildEnd()
                        }
                    }
                    buildEnd()
                }
            }
            buildRefNull(WasmHeapType.Simple.None, serviceCodeLocation)
            buildInstr(WasmOp.RETURN, serviceCodeLocation)
        }

        val associatedObjectGetterTypeRef =
            WasmRefType(Synthetics.HeapTypes.associatedObjectGetterType)

        val associatedObjectGetterWrapper = WasmStructDeclaration(
            name = "AssociatedObjectGetterWrapper",
            fields = listOf(WasmStructFieldDeclaration("getter", associatedObjectGetterTypeRef, false)),
            superType = null,
            isFinal = true
        )

        definedDeclarations.gcTypes[Synthetics.GcTypes.associatedObjectGetterWrapper.value] = associatedObjectGetterWrapper
    }

    private fun createStartUnitTestsFunction(definedDeclarations: DefinedDeclarationsResolver, exports: MutableList<WasmExport<*>>) {
        if (!definedDeclarations.functions.containsKey(Synthetics.Functions.runRootSuitesBuiltIn.value)) return

        val startUnitTestsFunction =
            WasmFunction.Defined(WasmSpecialExportNames.START_UNIT_TESTS, Synthetics.FunctionHeapTypes.parameterlessNoReturnFunctionType)
        with(WasmExpressionBuilder(startUnitTestsFunction.instructions)) {
            forEachLinkerData { linkerData ->
                linkerData.testFunctionDeclarators.forEach { declarator ->
                    buildCall(declarator, serviceCodeLocation)
                }
            }
            buildCall(Synthetics.Functions.runRootSuitesBuiltIn, serviceCodeLocation)
        }
        exports.add(WasmExport.Function(WasmSpecialExportNames.START_UNIT_TESTS, startUnitTestsFunction))
        definedDeclarations.functions[Synthetics.Functions.startUnitTestsFunction.value] = startUnitTestsFunction
    }

    private fun createFieldInitializerFunction(definedDeclarations: DefinedDeclarationsResolver, stringPoolSize: Int) {
        val fieldInitializerFunction = WasmFunction.Defined("_fieldInitialize", Synthetics.FunctionHeapTypes.parameterlessNoReturnFunctionType)
        with(WasmExpressionBuilder(fieldInitializerFunction.instructions)) {
            buildConstI32(0, serviceCodeLocation)
            buildConstI32(stringPoolSize, serviceCodeLocation)
            buildInstr(
                WasmOp.ARRAY_NEW_DATA,
                serviceCodeLocation,
                Synthetics.GcTypes.wasmLongArrayDeclaration,
                stringAddressesAndLengthsIndex,
            )
            buildSetGlobal(Synthetics.Globals.addressesAndLengthsGlobal, serviceCodeLocation)

            forEachLinkerData { linkerData ->
                linkerData.objectInstanceFieldInitializers.forEach { objectInitializer ->
                    buildCall(objectInitializer, serviceCodeLocation)
                }
            }

            forEachLinkerData { linkerData ->
                linkerData.nonConstantFieldInitializers.forEach { nonConstantInitializer ->
                    buildCall(nonConstantInitializer, serviceCodeLocation)
                }
            }
        }
        definedDeclarations.functions[Synthetics.Functions.fieldInitializerFunction.value] = fieldInitializerFunction
    }

    private fun stringAddressesAndLengthsField(
        definedDeclarations: DefinedDeclarationsResolver,
    ) {
        val wasmLongArrayDeclaration =
            WasmArrayDeclaration("long_array", WasmStructFieldDeclaration("long", WasmI64, false))
        definedDeclarations.gcTypes[Synthetics.GcTypes.wasmLongArrayDeclaration.value] = wasmLongArrayDeclaration

        val stringAddressesAndLengthsInitializer = listOf(
            wasmInstrWithoutLocation(
                operator = WasmOp.REF_NULL,
                immediate1 = WasmImmediate.HeapType(WasmRefNullrefType)
            ),
        )

        val refAddressesAndLengthsType =
            WasmRefNullType(Synthetics.HeapTypes.wasmLongArrayDeclaration)

        val global = WasmGlobal("_addressesAndLengths", refAddressesAndLengthsType, true, stringAddressesAndLengthsInitializer)
        definedDeclarations.globalFields[Synthetics.Globals.addressesAndLengthsGlobal.value] = global
    }

    private fun createStringPoolField(
        definedDeclarations: DefinedDeclarationsResolver,
        stringPoolSize: Int
    ) {
        val stringCacheFieldInitializer = listOf(
            wasmInstrWithoutLocation(
                operator = WasmOp.I32_CONST,
                immediate1 = WasmImmediate.ConstI32(stringPoolSize),
            ),
            wasmInstrWithoutLocation(
                operator = WasmOp.ARRAY_NEW_DEFAULT,
                immediate1 = Synthetics.GcTypes.wasmStringArrayType
            ),
        )

        val refToArrayOfNullableStringsType =
            WasmRefType(Synthetics.HeapTypes.wasmStringArrayType)

        val global = WasmGlobal("_stringPool", refToArrayOfNullableStringsType, false, stringCacheFieldInitializer)
        definedDeclarations.globalFields[Synthetics.Globals.stringPoolGlobal.value] = global
    }

    private enum class StringLiteralType {
        JsString,
        Latin1,
        Utf16
    }

    private fun createStringLiteralFunction(
        definedDeclarations: DefinedDeclarationsResolver,
        stringEntities: StringLiteralWasmEntities,
        wasmElements: MutableList<WasmElement>,
        stringLiteralType: StringLiteralType,
    ) {
        val isJsString = stringLiteralType == StringLiteralType.JsString
        val isLatin1 = stringLiteralType == StringLiteralType.Latin1

        val byteArray = WasmArrayDeclaration("byte_array", WasmStructFieldDeclaration("byte", WasmI8, false))
        definedDeclarations.gcTypes[Synthetics.GcTypes.byteArray.value] = byteArray

        var localIter = 0
        val poolIdLocal = WasmLocal(localIter++, "poolId", WasmI32, true)

        val jsString: WasmLocal?
        val startAddress: WasmLocal?
        val length: WasmLocal?
        val addressAndLength: WasmLocal?

        if (isJsString) {
            jsString = WasmLocal(localIter++, "jsString", WasmRefType(WasmHeapType.Simple.Extern), true)
            startAddress = null
            length = null
            addressAndLength = null
        } else {
            jsString = null
            startAddress = WasmLocal(localIter++, "startAddress", WasmI32, false)
            length = WasmLocal(localIter++, "length", WasmI32, false)
            addressAndLength = WasmLocal(localIter++, "addressAndLength", WasmI64, false)
        }

        val temporary = WasmLocal(localIter++, "temporary", stringEntities.kotlinStringType, false)

        val stringLiteralFunctionType =
            if (isJsString)
                Synthetics.FunctionHeapTypes.jsStringLiteralFunctionType
            else
                Synthetics.FunctionHeapTypes.stringLiteralFunctionType

        val stringLiteralFunction = WasmFunction.Defined(
            name = "_stringLiteral${stringLiteralType.name}",
            type = stringLiteralFunctionType,
            locals = listOfNotNull(poolIdLocal, jsString, startAddress, length, addressAndLength, temporary).toMutableList()
        )
        with(WasmExpressionBuilder(stringLiteralFunction.instructions)) {
            buildBlock("cache_check", stringEntities.kotlinStringType) { blockResult ->
                buildGetGlobal(Synthetics.Globals.stringPoolGlobal, serviceCodeLocation)
                buildGetLocal(poolIdLocal, serviceCodeLocation)
                buildInstr(
                    WasmOp.ARRAY_GET,
                    serviceCodeLocation,
                    Synthetics.GcTypes.wasmStringArrayType
                )
                buildBrInstr(WasmOp.BR_ON_NON_NULL, blockResult, serviceCodeLocation)

                // cache miss
                if (isJsString) {
                    buildGetLocal(jsString ?: error("jsString is not set"), serviceCodeLocation)
                    buildCall(Synthetics.Functions.jsToKotlinStringAdapterBuiltIn, serviceCodeLocation)
                } else {
                    buildGetGlobal(Synthetics.Globals.addressesAndLengthsGlobal, serviceCodeLocation)
                    buildGetLocal(poolIdLocal, serviceCodeLocation)
                    buildInstr(
                        op = WasmOp.ARRAY_GET,
                        location = serviceCodeLocation,
                        Synthetics.GcTypes.wasmLongArrayDeclaration,
                    )
                    buildSetLocal(addressAndLength ?: error("addressAndLength is not set"), serviceCodeLocation)

                    //Get length
                    buildGetLocal(addressAndLength, serviceCodeLocation)
                    buildConstI64(32L, serviceCodeLocation)
                    buildInstr(
                        op = WasmOp.I64_SHR_S,
                        location = serviceCodeLocation,
                    )
                    buildInstr(
                        op = WasmOp.I32_WRAP_I64,
                        location = serviceCodeLocation,
                    )
                    buildSetLocal(length ?: error("length is not set"), serviceCodeLocation)

                    //Get startAddress
                    buildGetLocal(addressAndLength, serviceCodeLocation)
                    buildInstr(
                        op = WasmOp.I32_WRAP_I64,
                        location = serviceCodeLocation,
                    )
                    buildSetLocal(startAddress ?: error("startAddress is not set"), serviceCodeLocation)

                    // create new string
                    buildGetLocal(startAddress, serviceCodeLocation)
                    buildGetLocal(length, serviceCodeLocation)

                    if (!isLatin1) {
                        buildInstr(
                            op = WasmOp.ARRAY_NEW_DATA,
                            location = serviceCodeLocation,
                            GcTypeSymbol(stringEntities.wasmCharArrayDeclaration), stringDataSectionIndex
                        )
                    } else {
                        val iterator = WasmLocal(localIter++, "intIterator", WasmI32, false)
                        val wasmByteArray = WasmLocal(localIter++, "byteArray", WasmRefType(Synthetics.HeapTypes.byteArray), false)
                        val wasmCharArray = WasmLocal(localIter++, "charArray", stringEntities.wasmCharArrayType, false)
                        stringLiteralFunction.locals.addAll(listOf(iterator, wasmByteArray, wasmCharArray))

                        buildInstr(
                            op = WasmOp.ARRAY_NEW_DATA,
                            location = serviceCodeLocation,
                            Synthetics.GcTypes.byteArray, stringDataSectionIndex
                        )
                        buildSetLocal(wasmByteArray, serviceCodeLocation)

                        buildGetLocal(length, serviceCodeLocation)
                        buildInstr(
                            op = WasmOp.ARRAY_NEW_DEFAULT,
                            location = serviceCodeLocation,
                            GcTypeSymbol(stringEntities.wasmCharArrayDeclaration)
                        )
                        buildSetLocal(wasmCharArray, serviceCodeLocation)

                        buildBlock("loop_body") { loopExit ->
                            buildLoop("copy_loop") { loop ->
                                buildGetLocal(iterator, serviceCodeLocation)
                                buildGetLocal(length, serviceCodeLocation)
                                buildInstr(WasmOp.I32_EQ, serviceCodeLocation)
                                buildBrIf(loopExit, serviceCodeLocation)

                                // char array set
                                buildGetLocal(wasmCharArray, serviceCodeLocation)
                                buildGetLocal(iterator, serviceCodeLocation)

                                // byte array get
                                buildGetLocal(wasmByteArray, serviceCodeLocation)
                                buildGetLocal(iterator, serviceCodeLocation)
                                buildInstr(WasmOp.ARRAY_GET_U, serviceCodeLocation, Synthetics.GcTypes.byteArray)

                                buildInstr(WasmOp.ARRAY_SET, serviceCodeLocation, GcTypeSymbol(stringEntities.wasmCharArrayDeclaration))

                                buildGetLocal(iterator, serviceCodeLocation)
                                buildConstI32(1, serviceCodeLocation)
                                buildInstr(WasmOp.I32_ADD, serviceCodeLocation)
                                buildSetLocal(iterator, serviceCodeLocation)
                                buildBr(loop, serviceCodeLocation)
                            }
                        }
                        buildGetLocal(wasmCharArray, serviceCodeLocation)
                    }

                    buildCall(Synthetics.Functions.createStringBuiltIn, serviceCodeLocation)
                }
                buildSetLocal(temporary, serviceCodeLocation)

                //remember and return string
                buildGetGlobal(Synthetics.Globals.stringPoolGlobal, serviceCodeLocation)
                buildGetLocal(poolIdLocal, serviceCodeLocation)
                buildGetLocal(temporary, serviceCodeLocation)
                buildInstr(
                    WasmOp.ARRAY_SET,
                    serviceCodeLocation,
                    Synthetics.GcTypes.wasmStringArrayType
                )
                buildGetLocal(temporary, serviceCodeLocation)
            }
            buildInstr(WasmOp.RETURN, serviceCodeLocation)
        }

        // Make this function possible to func.ref
        wasmElements.add(
            WasmElement(
                type = WasmFuncRef,
                values = listOf(WasmTable.Value.Function(WasmSymbol(stringLiteralFunction))),
                mode = WasmElement.Mode.Declarative
            )
        )

        val functionSignature = when (stringLiteralType) {
            StringLiteralType.JsString -> Synthetics.Functions.createStringLiteralJsString.value
            StringLiteralType.Latin1 -> Synthetics.Functions.createStringLiteralLatin1.value
            StringLiteralType.Utf16 -> Synthetics.Functions.createStringLiteralUtf16.value
        }
        definedDeclarations.functions[functionSignature] = stringLiteralFunction
    }

    private fun <T> putAllChecked(from: Map<IdSignature, T>, to: MutableMap<IdSignature, T>, info: String) {
        val oldFromSize = to.size
        to.putAll(from)
        if (oldFromSize + from.size != to.size) {
            compilationException("Declaration redefinition happened on $info.", null)
        }
    }

    private fun getDefinedDeclarationsFromFragments(): DefinedDeclarationsResolver {
        val resolver = DefinedDeclarationsResolver()
        wasmCompiledFileFragments.forEach { fragment ->
            val fragmentTypes = fragment.definedTypes
            val fragmentDeclarations = fragment.definedDeclarations
            putAllChecked(fragmentDeclarations.definedFunctions, resolver.functions, "functions")
            putAllChecked(fragmentDeclarations.definedGlobalFields, resolver.globalFields, "globalFields")
            putAllChecked(fragmentDeclarations.definedGlobalVTables, resolver.globalVTables, "globalVTables")
            putAllChecked(fragmentDeclarations.definedGlobalClassITables, resolver.globalClassITables, "globalClassITables")
            putAllChecked(fragmentDeclarations.definedRttiGlobal, resolver.globalRTTI, "globalRTTI")
            putAllChecked(fragmentTypes.definedGcTypes, resolver.gcTypes, "gcTypes")
            putAllChecked(fragmentTypes.definedVTableGcTypes, resolver.vTableGcTypes, "vTableGcTypes")
            // functionTypes are deduplicated by Wasm signature structure, duplicates are expected and equivalent
            resolver.functionTypes.putAll(fragmentTypes.definedFunctionTypes)
        }

        rebindEquivalentFunctions(resolver.functions)
        rebindEquivalentDeclarations(resolver.gcTypes) { it.equivalentTypes }
        rebindEquivalentDeclarations(resolver.vTableGcTypes) { it.equivalentTypes }
        rebindEquivalentDeclarations(resolver.globalRTTI) { it.equivalentTypes }
        rebindEquivalentDeclarations(resolver.globalVTables) { it.equivalentTypes }
        rebindEquivalentDeclarations(resolver.globalClassITables) { it.equivalentTypes }
        bindUniqueJsFunNames()
        return resolver
    }

    private fun bindGlobalLiterals(
        definedDeclarations: DefinedDeclarationsResolver,
        stringPoolSize: Int,
        extraLiterals: Map<String, WasmSymbol<Int>> = emptyMap(),
    ): Int {
        var literalCounter = stringPoolSize
        val literalGlobalIdMap = mutableMapOf<String, Int>()

        // one imported JS-string constant global per distinct string (the
        // import's NAME is the string value; the global's own name is
        // debug-only), one id per distinct string, both channels shared
        fun globalFor(string: String): WasmGlobal =
            definedDeclarations.globalLiteralGlobals.computeIfAbsent(string) {
                WasmGlobal(
                    name = "string_${definedDeclarations.globalLiteralGlobals.size}",
                    type = WasmRefType(WasmHeapType.Simple.Extern),
                    isMutable = false,
                    init = emptyList(),
                    importPair = WasmImportDescriptor(importedStringConstants, WasmSymbol(string))
                )
            }

        fun idOf(stringValue: String): Int {
            var stringId = literalGlobalIdMap[stringValue]
            if (stringId == null) {
                stringId = literalCounter
                literalGlobalIdMap[stringValue] = stringId
                literalCounter++
            }
            return stringId
        }

        // codegen-registered literals first, then the link-time ones (the
        // registry entry keys/chunks)
        forEachLinkerData { linkerData ->
            for (symbol in linkerData.globalLiterals) globalFor(symbol.value)
            for ([stringValue, literalIdSymbol] in linkerData.globalLiteralsId) {
                literalIdSymbol.bind(idOf(stringValue))
            }
        }
        for ([stringValue, literalIdSymbol] in extraLiterals) {
            globalFor(stringValue)
            literalIdSymbol.bind(idOf(stringValue))
        }
        return literalCounter
    }

    private fun bindStringPoolSymbolsAndGetSize(
        data: MutableList<WasmData>,
        extraLiterals: Map<String, WasmSymbol<Int>> = emptyMap(),
    ): Int {
        val stringDataSectionBytes = mutableListOf<Byte>()
        var stringDataSectionStart = 0
        val visitedStrings = mutableMapOf<String, Int>()
        val addressesAndLengths = mutableListOf<Long>()

        fun addString(string: String, symbol: WasmSymbol<Int>) {
            var stringId = visitedStrings[string]
            if (stringId == null) {
                stringId = visitedStrings.size
                visitedStrings[string] = stringId
                addressesAndLengths.add(stringDataSectionStart.toLong() or (string.length.toLong() shl 32))
                val constData = ConstantDataCharArray(string.toCharArray(), string.fitsLatin1)
                stringDataSectionBytes += constData.toBytes().toList()
                stringDataSectionStart += constData.sizeInBytes
            }
            symbol.bind(stringId)
        }

        // codegen-registered literals first, then the link-time ones (the
        // registry entry keys/chunks) — the same pool, the same segments
        forEachLinkerData { linkerData ->
            for ([string, symbol] in linkerData.stringLiteralId.entries) addString(string, symbol)
        }
        for ([string, symbol] in extraLiterals) addString(string, symbol)

        data.add(WasmData(WasmDataMode.Passive, stringDataSectionBytes.toByteArray()))
        val constDataAddressesAndLengths = ConstantDataIntegerArray(addressesAndLengths, LONG_SIZE_BYTES)
        data.add(WasmData(WasmDataMode.Passive, constDataAddressesAndLengths.toBytes()))

        return visitedStrings.size
    }

    private fun bindConstantArrayDataSegmentIds(data: MutableList<WasmData>) {
        forEachLinkerData { linkerData ->
            linkerData.constantArrayDataSegmentId.entries.forEach { [constantArraySegment, symbol] ->
                symbol.bind(data.size)
                val integerSize = when (constantArraySegment.second) {
                    WasmI8 -> BYTE_SIZE_BYTES
                    WasmI16 -> SHORT_SIZE_BYTES
                    WasmI32 -> INT_SIZE_BYTES
                    WasmI64 -> LONG_SIZE_BYTES
                    else -> TODO("type ${constantArraySegment.second} is not implemented")
                }
                val constData = ConstantDataIntegerArray(constantArraySegment.first, integerSize)
                data.add(WasmData(WasmDataMode.Passive, constData.toBytes()))
            }
        }
    }

    private fun bindUniqueJsFunNames() {
        val jsCodeCounter = mutableMapOf<String, Int>()
        forEachLinkerData { linkerData ->
            linkerData.jsFuns.forEach { jsCodeSnippet ->
                val jsFunName = jsCodeSnippet.value.importName.owner
                val counterValue = jsCodeCounter.getOrPut(jsFunName, defaultValue = { 0 })
                jsCodeCounter[jsFunName] = counterValue + 1
                if (counterValue > 0) {
                    jsCodeSnippet.value.importName.bind("${jsFunName}_$counterValue")
                }
            }
        }
    }

    private fun rebindEquivalentFunctions(allDefinedFunctions: MutableMap<IdSignature, WasmFunction>) {
        val equivalentFunctions = mutableMapOf<String, WasmFunction>()
        forEachLinkerData { linkerData ->
            for ([signatureString, idSignature] in linkerData.equivalentFunctions) {
                val func = equivalentFunctions[signatureString]
                if (func == null) {
                    // First occurrence of the adapter, register it (if not removed by DCE).
                    equivalentFunctions[signatureString] = allDefinedFunctions[idSignature] ?: continue
                } else {
                    // Adapter already exists, remove this one and use the existing adapter.
                    allDefinedFunctions[idSignature]?.let { duplicate ->
                        linkerData.exports.removeAll { it.field == duplicate }
                    }
                    linkerData.jsFuns.remove(idSignature)
                    linkerData.jsModuleImports.remove(idSignature)

                    // Rebind adapter function to the single instance
                    // There might not be any unbound references in case it's called only from JS side
                    allDefinedFunctions[idSignature] = func
                }
            }
        }
    }

    private fun <T : WasmNamedModuleField> rebindEquivalentDeclarations(
        allDefinedDeclarations: MutableMap<IdSignature, T>,
        equivalentDeclarationsSelector: (WasmCompiledLinkerDataFileFragment) -> List<Pair<String, IdSignature>>
    ) {
        val canonicalDeclarations = mutableMapOf<String, T>()
        forEachLinkerData { linkerData ->
            for ([equivalenceKey, idSignature] in equivalentDeclarationsSelector(linkerData)) {
                val canonical = canonicalDeclarations[equivalenceKey]
                if (canonical == null) {
                    // First occurrence, register it as canonical (if not removed by DCE).
                    canonicalDeclarations[equivalenceKey] = allDefinedDeclarations[idSignature] ?: continue
                } else {
                    // Already exists, rebind to the canonical instance.
                    allDefinedDeclarations[idSignature]?.let { duplicate ->
                        linkerData.exports.removeAll { it.field == duplicate }
                    }
                    allDefinedDeclarations[idSignature] = canonical
                }
            }
        }
    }
}

data class ClassAssociatedObjects(
    val klass: Long,
    val objects: List<AssociatedObject>
)

data class AssociatedObject(
    val obj: Long,
    val getterFunc: IdSignature,
    val isExternal: Boolean,
)

private fun WasmExpressionBuilder.buildCall(symbol: IdSignature, location: SourceLocation) {
    buildInstr(WasmOp.CALL, location, FuncSymbol(symbol))
}
