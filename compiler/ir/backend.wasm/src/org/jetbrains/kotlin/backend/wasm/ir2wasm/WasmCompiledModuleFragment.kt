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
import org.jetbrains.kotlin.backend.wasm.MultimoduleCompileOptions
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.importedStringConstants
import org.jetbrains.kotlin.backend.wasm.wasmStartExportName
import org.jetbrains.kotlin.backend.wasm.utils.fitsLatin1
import org.jetbrains.kotlin.backend.wasm.wasmInitializeExportName
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.WasmFunction
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import java.util.IdentityHashMap
import kotlin.collections.MutableMap
import kotlin.collections.mutableMapOf
import kotlin.collections.set

enum class ExceptionTagType { WASM_TAG, JS_TAG, TRAP }

class WasmCompiledModuleFragment(
    private val wasmCompiledFileFragments: List<WasmCompiledFileFragment>,
    private val isWasmJsTarget: Boolean
) {
    // Used during linking
    private val serviceCodeLocation = SourceLocation.NoLocation("Generated service code")

    private val stringDataSectionIndex = WasmImmediate.DataIdx(0)
    private val stringAddressesAndLengthsIndex = WasmImmediate.DataIdx(1)

    private inline fun forEachServiceData(body: (WasmCompiledServiceFileFragment) -> Unit) {
        wasmCompiledFileFragments.forEach { fragment ->
            (fragment as? WasmCompiledCodeFileFragment)?.let { body(it.serviceData) }
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
    ) {
        stringAddressesAndLengthsField(
            definedDeclarations = definedDeclarations,
        )

        createFieldInitializerFunction(definedDeclarations = definedDeclarations, stringPoolSize = stringPoolSize)

        createAssociatedObjectGetter(definedDeclarations = definedDeclarations, wasmElements = wasmElements)

        val mainFunctionSymbols = mutableListOf<WasmFunction>()
        forEachServiceData {
            it.mainFunctionWrappers.mapTo(mainFunctionSymbols) { signature ->
                definedDeclarations.functions[signature]
                    ?: compilationException("Cannot find symbol for main wrapper", type = null)
            }
        }

        val mainFunctionsExportName = if (wasmCommandModuleInitialization)
            wasmInitializeExportName
        else
            wasmStartExportName

        exports.addAll(mainFunctionSymbols.map { WasmExport.Function(mainFunctionsExportName, it) })

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
        wasmCommandModuleInitialization: Boolean
    ): WasmModule {
        val definedDeclarations = getDefinedDeclarationsFromFragments()

        val data = mutableListOf<WasmData>()
        val stringPoolSize = bindStringPoolSymbolsAndGetSize(data)
        val stringPoolSizeWithGlobals = bindGlobalLiterals(definedDeclarations, stringPoolSize)

        bindConstantArrayDataSegmentIds(data)

        val exports = mutableListOf<WasmExport<*>>()
        forEachServiceData {
            exports.addAll(it.exports)
        }

        val memories = createAndExportMemory(exports, multimoduleOptions?.stdlibModuleNameForImport)
        val (importedMemories, definedMemories) = memories.partition { it.importPair != null }

        val parameterlessNoReturnFunctionType = WasmFunctionType(emptyList(), emptyList())
        definedDeclarations.functionTypes[Synthetics.FunctionHeapTypes.parameterlessNoReturnFunctionType.type] = parameterlessNoReturnFunctionType

        val stringEntities = getStringLiteralWasmEntities(definedDeclarations)

        createAndBindSpecialITableTypes(definedDeclarations)
        createAndBindRttiTypeDeclaration(definedDeclarations)

        val elements = mutableListOf<WasmElement>()
        createAndExportServiceFunctions(
            definedDeclarations = definedDeclarations,
            stringEntities = stringEntities,
            stringPoolSize = stringPoolSize,
            stringPoolSizeWithGlobals = stringPoolSizeWithGlobals,
            wasmElements = elements,
            exports = exports,
            wasmCommandModuleInitialization = wasmCommandModuleInitialization,
        )

        val masterInitFunction = createMasterInitFunction(
            definedDeclarations = definedDeclarations,
            initializeUnit = multimoduleOptions?.initializeUnit ?: true
        )

        val globals = getGlobals(definedDeclarations)

        val tags = getTags(definedDeclarations, exceptionTagType)
        require(tags.size <= 1) { "Having more than 1 tag is not supported" }

        val (importedTags, definedTags) = tags.partition { it.importPair != null }

        val (importedGlobals, definedGlobals) = globals.partition { it.importPair != null }

        val (definedFunctions, importedFunctions) = partitionDefinedAndImportedFunctions(definedDeclarations)

        val importsInOrder = mutableListOf<WasmNamedModuleField>()
        importsInOrder.addAll(importedFunctions)
        importsInOrder.addAll(importedTags)
        importsInOrder.addAll(importedGlobals)
        importsInOrder.addAll(importedMemories)

        val recursiveTypeGroups = getTypes(definedDeclarations)

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
            startFunction = masterInitFunction,
            elements = elements,
            data = data,
            dataCount = true,
            tags = definedTags
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

    private fun getTags(definedDeclarations: DefinedDeclarationsResolver, exceptionTagType: ExceptionTagType): List<WasmTag> {
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
        }
        return listOfNotNull(exceptionTag)
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
            addTypes(definedDeclarations.gcTypes.values)
            addTypes(definedDeclarations.vTableGcTypes.values)
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

    private fun getGlobals(definedDeclarations: DefinedDeclarationsResolver) = mutableListOf<WasmGlobal>().apply {
        addAll(definedDeclarations.globalFields.values)
        addAll(definedDeclarations.globalVTables.values)
        addAll(definedDeclarations.globalClassITables.values)

        val rttiGlobals = mutableMapOf<IdSignature, WasmGlobal>()
        val rttiSuperTypes = mutableMapOf<IdSignature, IdSignature?>()

        wasmCompiledFileFragments.forEach { fragment ->
            rttiGlobals.putAll(fragment.definedDeclarations.definedRttiGlobal)
            rttiSuperTypes.putAll(fragment.definedDeclarations.definedRttiSuperType)
        }

        fun wasmRttiGlobalOrderKey(superType: IdSignature?): Int =
            superType?.let { wasmRttiGlobalOrderKey(rttiSuperTypes[it]) + 1 } ?: 0

        rttiGlobals.keys.sortedBy(::wasmRttiGlobalOrderKey).mapTo(this) { rttiGlobals[it]!! }

        addAll(definedDeclarations.globalLiteralGlobals.values)
    }

    private fun createAndExportMemory(exports: MutableList<WasmExport<*>>, stdlibModuleNameForImport: String?): List<WasmMemory> {
        val memorySizeInPages = 0
        val importPair = stdlibModuleNameForImport?.let { WasmImportDescriptor(it, WasmSymbol("memory")) }
        val memory = WasmMemory(WasmLimits(memorySizeInPages.toUInt(), null/* "unlimited" */), importPair)

        // Need to export the memory in order to pass complex objects to the host language.
        // Export name "memory" is a WASI ABI convention.
        val exportMemory = WasmExport.Memory("memory", memory)
        exports.add(exportMemory)
        return listOf(memory)
    }

    private fun createMasterInitFunction(
        definedDeclarations: DefinedDeclarationsResolver,
        initializeUnit: Boolean,
    ): WasmFunction.Defined {
        val masterInitFunction = WasmFunction.Defined("_initializeModule", Synthetics.FunctionHeapTypes.parameterlessNoReturnFunctionType)
        with(WasmExpressionBuilder(masterInitFunction.instructions)) {
            if (initializeUnit) {
                buildCall(Synthetics.Functions.unitGetInstanceBuiltIn, serviceCodeLocation)
            }

            buildCall(Synthetics.Functions.fieldInitializerFunction, serviceCodeLocation)

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

    private fun createAssociatedObjectGetter(
        definedDeclarations: DefinedDeclarationsResolver,
        wasmElements: MutableList<WasmElement>,
    ) {
        // If AO accessor removed by DCE - we do not need it then
        if (!definedDeclarations.functions.containsKey(Synthetics.Functions.associatedObjectGetter.value)) return
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
            forEachServiceData { serviceData ->
                for ((klassId, associatedObjectsInstanceGetters) in serviceData.classAssociatedObjectsInstanceGetters) {
                    buildGetLocal(classIdLocal, serviceCodeLocation)
                    buildConstI64(klassId, serviceCodeLocation)
                    buildInstr(WasmOp.I64_EQ, serviceCodeLocation)
                    buildIf("Class matches")
                    associatedObjectsInstanceGetters.forEach { (keyId, getter, isExternal) ->
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

        val startUnitTestsFunction = WasmFunction.Defined("startUnitTests", Synthetics.FunctionHeapTypes.parameterlessNoReturnFunctionType)
        with(WasmExpressionBuilder(startUnitTestsFunction.instructions)) {
            forEachServiceData { serviceData ->
                serviceData.testFunctionDeclarators.forEach { declarator ->
                    buildCall(declarator, serviceCodeLocation)
                }
            }
            buildCall(Synthetics.Functions.runRootSuitesBuiltIn, serviceCodeLocation)
        }
        exports.add(WasmExport.Function("startUnitTests", startUnitTestsFunction))
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

            forEachServiceData { serviceData ->
                serviceData.objectInstanceFieldInitializers.forEach { objectInitializer ->
                    buildCall(objectInitializer, serviceCodeLocation)
                }
            }

            forEachServiceData { serviceData ->
                serviceData.nonConstantFieldInitializers.forEach { nonConstantInitializer ->
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
            putAllChecked(fragmentTypes.definedFunctionTypes, resolver.functionTypes, "functionTypes")
        }

        rebindEquivalentFunctions(resolver.functions)
        bindUniqueJsFunNames()
        return resolver
    }

    private fun bindGlobalLiterals(definedDeclarations: DefinedDeclarationsResolver, stringPoolSize: Int): Int {
        var literalCounter = stringPoolSize
        val literalGlobalIdMap = mutableMapOf<String, Int>()

        forEachServiceData { serviceData ->
            var globalCounter = 0
            for (symbol in serviceData.globalLiterals) {
                definedDeclarations.globalLiteralGlobals.computeIfAbsent(symbol.value) { string ->
                    WasmGlobal(
                        name = "string_${globalCounter++}",
                        type = WasmRefType(WasmHeapType.Simple.Extern),
                        isMutable = false,
                        init = emptyList(),
                        importPair = WasmImportDescriptor(importedStringConstants, WasmSymbol(string))
                    )
                }
            }
            for ((stringValue, literalIdSymbol) in serviceData.globalLiteralsId) {
                var stringId = literalGlobalIdMap[stringValue]
                if (stringId == null) {
                    stringId = literalCounter
                    literalGlobalIdMap[stringValue] = stringId
                    literalCounter++
                }
                literalIdSymbol.bind(stringId)
            }
        }
        return literalCounter
    }

    private fun bindStringPoolSymbolsAndGetSize(data: MutableList<WasmData>): Int {
        val stringDataSectionBytes = mutableListOf<Byte>()
        var stringDataSectionStart = 0
        val visitedStrings = mutableMapOf<String, Int>()
        val addressesAndLengths = mutableListOf<Long>()
        forEachServiceData { serviceData ->
            for ((string, literalIdSymbol) in serviceData.stringLiteralId.entries) {
                val visitedStringId = visitedStrings[string]
                val stringId: Int
                if (visitedStringId == null) {
                    stringId = visitedStrings.size
                    visitedStrings[string] = stringId

                    addressesAndLengths.add(stringDataSectionStart.toLong() or (string.length.toLong() shl 32))
                    val constData = ConstantDataCharArray(string.toCharArray(), string.fitsLatin1)
                    stringDataSectionBytes += constData.toBytes().toList()
                    stringDataSectionStart += constData.sizeInBytes
                } else {
                    stringId = visitedStringId
                }
                literalIdSymbol.bind(stringId)
            }
        }

        data.add(WasmData(WasmDataMode.Passive, stringDataSectionBytes.toByteArray()))
        val constDataAddressesAndLengths = ConstantDataIntegerArray(addressesAndLengths, LONG_SIZE_BYTES)
        data.add(WasmData(WasmDataMode.Passive, constDataAddressesAndLengths.toBytes()))

        return visitedStrings.size
    }

    private fun bindConstantArrayDataSegmentIds(data: MutableList<WasmData>) {
        forEachServiceData { serviceData ->
            serviceData.constantArrayDataSegmentId.entries.forEach { (constantArraySegment, symbol) ->
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
        forEachServiceData { serviceData ->
            serviceData.jsFuns.forEach { jsCodeSnippet ->
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
        forEachServiceData { serviceData ->
            for ((signatureString, idSignature) in serviceData.equivalentFunctions) {
                val func = equivalentFunctions[signatureString]
                if (func == null) {
                    // First occurrence of the adapter, register it (if not removed by DCE).
                    equivalentFunctions[signatureString] = allDefinedFunctions[idSignature] ?: continue
                } else {
                    // Adapter already exists, remove this one and use the existing adapter.
                    allDefinedFunctions[idSignature]?.let { duplicate ->
                        serviceData.exports.removeAll { it.field == duplicate }
                    }
                    serviceData.jsFuns.remove(idSignature)
                    serviceData.jsModuleImports.remove(idSignature)

                    // Rebind adapter function to the single instance
                    // There might not be any unbound references in case it's called only from JS side
                    allDefinedFunctions[idSignature] = func
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