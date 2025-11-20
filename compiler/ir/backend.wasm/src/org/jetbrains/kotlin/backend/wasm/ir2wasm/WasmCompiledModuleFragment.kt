/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.backend.wasm.MultimoduleCompileOptions
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment.*
import org.jetbrains.kotlin.backend.wasm.utils.fitsLatin1
import org.jetbrains.kotlin.ir.backend.js.ic.IrICProgramFragment
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.WasmFunction
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

class BuiltinIdSignatures(
    val throwable: IdSignature?,
    val kotlinAny: IdSignature?,
    val tryGetAssociatedObject: IdSignature?,
    val jsToKotlinAnyAdapter: IdSignature?,
    val jsToKotlinStringAdapter: IdSignature?,
    val unitGetInstance: IdSignature?,
    val runRootSuites: IdSignature?,
    val createString: IdSignature?,
    val registerModuleDescriptor: IdSignature?,
)

class SpecialITableTypes(
    val wasmAnyArrayType: WasmSymbol<WasmArrayDeclaration> = WasmSymbol(),
    val specialSlotITableType: WasmSymbol<WasmStructDeclaration> = WasmSymbol(),
)

class RttiGlobal(
    val global: WasmGlobal,
    val classSignature: IdSignature,
    val superClassSignature: IdSignature?,
)

class RttiElements(
    val globals: MutableList<RttiGlobal> = mutableListOf(),
    val globalReferences: ReferencableElements<IdSignature, WasmGlobal> = ReferencableElements(),
    val rttiType: WasmSymbol<WasmStructDeclaration> = WasmSymbol()
)

class WasmStringsElements(
    var createStringLiteralUtf16: WasmSymbol<WasmFunction> = WasmSymbol(),
    var createStringLiteralLatin1: WasmSymbol<WasmFunction> = WasmSymbol(),
    var createStringLiteralJsString: WasmSymbol<WasmFunction> = WasmSymbol(),
    var createStringLiteralType: WasmSymbol<WasmFunctionType> = WasmSymbol(),
    var createStringLiteralJsStringType: WasmSymbol<WasmFunctionType> = WasmSymbol(),
)

class WasmCompiledFileFragment(
    val fragmentTag: String?,
    val functions: ReferencableAndDefinable<IdSignature, WasmFunction> = ReferencableAndDefinable(),
    val globalLiterals: ReferencableElements<String, WasmGlobal> = ReferencableElements(),
    val globalLiteralsIds: ReferencableElements<String, Int> = ReferencableElements(),
    val globalFields: ReferencableAndDefinable<IdSignature, WasmGlobal> = ReferencableAndDefinable(),
    val globalVTables: ReferencableAndDefinable<IdSignature, WasmGlobal> = ReferencableAndDefinable(),
    val globalClassITables: ReferencableAndDefinable<IdSignature, WasmGlobal> = ReferencableAndDefinable(),
    val functionTypes: ReferencableAndDefinable<IdSignature, WasmFunctionType> = ReferencableAndDefinable(),
    val gcTypes: ReferencableAndDefinable<IdSignature, WasmTypeDeclaration> = ReferencableAndDefinable(),
    val vTableGcTypes: ReferencableAndDefinable<IdSignature, WasmTypeDeclaration> = ReferencableAndDefinable(),
    val stringLiteralId: ReferencableElements<String, Int> = ReferencableElements(),
    val constantArrayDataSegmentId: ReferencableElements<Pair<List<Long>, WasmType>, Int> = ReferencableElements(),
    val jsFuns: MutableMap<IdSignature, JsCodeSnippet> = mutableMapOf(),
    val jsModuleImports: MutableMap<IdSignature, String> = mutableMapOf(),
    val jsBuiltinsPolyfills: MutableMap<String, String> = mutableMapOf(),
    val exports: MutableList<WasmExport<*>> = mutableListOf(),
    var wasmStringsElements: WasmStringsElements? = null,
    val mainFunctionWrappers: MutableList<IdSignature> = mutableListOf(),
    var testFunctionDeclarators: MutableList<IdSignature> = mutableListOf(),
    val equivalentFunctions: MutableList<Pair<String, IdSignature>> = mutableListOf(),
    val jsModuleAndQualifierReferences: MutableSet<JsModuleAndQualifierReference> = mutableSetOf(),
    val classAssociatedObjectsInstanceGetters: MutableList<ClassAssociatedObjects> = mutableListOf(),
    var classAssociatedObjectsGetterWrapper: WasmSymbol<WasmStructDeclaration>? = null,
    var builtinIdSignatures: BuiltinIdSignatures? = null,
    var specialITableTypes: SpecialITableTypes? = null,
    var rttiElements: RttiElements? = null,
    val objectInstanceFieldInitializers: MutableList<IdSignature> = mutableListOf(),
    val nonConstantFieldInitializers: MutableList<IdSignature> = mutableListOf(),
) : IrICProgramFragment()

enum class ExceptionTagType { WASM_TAG, JS_TAG, TRAP }

class WasmCompiledModuleFragment(
    private val wasmCompiledFileFragments: List<WasmCompiledFileFragment>,
    private val isWasmJsTarget: Boolean
) {
    // Used during linking
    private val serviceCodeLocation = SourceLocation.NoLocation("Generated service code")
    private val parameterlessNoReturnFunctionType = WasmFunctionType(emptyList(), emptyList())

    private val stringDataSectionIndex = WasmImmediate.DataIdx(0)
    private val stringAddressesAndLengthsIndex = WasmImmediate.DataIdx(1)

    private inline fun tryFindBuiltInFunction(select: (BuiltinIdSignatures) -> IdSignature?): WasmFunction? {
        for (fragment in wasmCompiledFileFragments) {
            val builtinSignatures = fragment.builtinIdSignatures ?: continue
            val signature = select(builtinSignatures) ?: continue
            return fragment.functions.defined[signature]
        }
        return null
    }

    private inline fun tryFindBuiltInType(select: (BuiltinIdSignatures) -> IdSignature?): WasmTypeDeclaration? {
        for (fragment in wasmCompiledFileFragments) {
            val builtinSignatures = fragment.builtinIdSignatures ?: continue
            val signature = select(builtinSignatures) ?: continue
            return fragment.gcTypes.defined[signature]
        }
        return null
    }

    class JsCodeSnippet(val importName: WasmSymbol<String>, val jsCode: String)

    open class ReferencableElements<Ir, Wasm : Any>(
        val unbound: MutableMap<Ir, WasmSymbol<Wasm>> = mutableMapOf()
    ) {
        fun reference(ir: Ir): WasmSymbol<Wasm> {
            val declaration = (ir as? IrSymbol)?.owner as? IrDeclarationWithName
            if (declaration != null) {
                val packageFragment = declaration.getPackageFragment()
                if (packageFragment is IrExternalPackageFragment) {
                    compilationException("Referencing declaration without package fragment", declaration)
                }
            }
            return unbound.getOrPut(ir) { WasmSymbol() }
        }
    }

    class ReferencableAndDefinable<Ir, Wasm : Any>(
        unbound: MutableMap<Ir, WasmSymbol<Wasm>> = mutableMapOf(),
        val defined: LinkedHashMap<Ir, Wasm> = LinkedHashMap(),
        val elements: MutableList<Wasm> = mutableListOf(),
        val wasmToIr: MutableMap<Wasm, Ir> = mutableMapOf()
    ) : ReferencableElements<Ir, Wasm>(unbound) {
        fun define(ir: Ir, wasm: Wasm) {
            if (ir in defined)
                compilationException("Trying to redefine element: IR: $ir Wasm: $wasm", type = null)

            elements += wasm
            defined[ir] = wasm
            wasmToIr[wasm] = ir
        }
    }

    private fun partitionDefinedAndImportedFunctions(): Pair<MutableList<WasmFunction.Defined>, MutableList<WasmFunction.Imported>> {
        val definedFunctions = mutableListOf<WasmFunction.Defined>()
        val importedFunctions = mutableListOf<WasmFunction.Imported>()
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.functions.elements.forEach { function ->
                when (function) {
                    is WasmFunction.Defined -> definedFunctions.add(function)
                    is WasmFunction.Imported -> importedFunctions.add(function)
                }
            }
        }
        return definedFunctions to importedFunctions
    }

    private fun createAndExportServiceFunctions(
        definedFunctions: MutableList<WasmFunction.Defined>,
        stringEntities: StringLiteralWasmEntities,
        additionalTypes: MutableList<WasmTypeDeclaration>,
        stringPoolSize: Int,
        initializeUnit: Boolean,
        stringPoolSizeWithGlobals: Int,
        wasmElements: MutableList<WasmElement>,
        exports: MutableList<WasmExport<*>>,
        globals: MutableList<WasmGlobal>,
    ) {
        val (stringAddressesAndLengthsGlobal, wasmLongArrayDeclaration) = stringAddressesAndLengthsField(additionalTypes)
        globals.add(stringAddressesAndLengthsGlobal)

        val fieldInitializerFunction =
            createFieldInitializerFunction(stringPoolSize, stringAddressesAndLengthsGlobal, wasmLongArrayDeclaration)
        definedFunctions.add(fieldInitializerFunction)

        val associatedObjectGetterAndWrapper = createAssociatedObjectGetterFunctionAndWrapper(wasmElements, additionalTypes)
        if (associatedObjectGetterAndWrapper != null) {
            definedFunctions.add(associatedObjectGetterAndWrapper.first)
            additionalTypes.add(associatedObjectGetterAndWrapper.second)
        }

        val masterInitFunction = createAndExportMasterInitFunction(
            fieldInitializerFunction = fieldInitializerFunction,
            tryGetAssociatedObjectAndWrapper = associatedObjectGetterAndWrapper,
            initializeUnit = initializeUnit
        )
        exports.add(WasmExport.Function("_initialize", masterInitFunction))
        definedFunctions.add(masterInitFunction)

        val stringPoolField = createStringPoolField(stringPoolSizeWithGlobals, stringEntities)
        globals.add(stringPoolField)

        if (isWasmJsTarget) {
            val stringLiteralFunctionJsString =
                createStringLiteralFunction(
                    stringPoolGlobalField = stringPoolField,
                    stringEntities = stringEntities,
                    additionalTypes = additionalTypes,
                    wasmElements = wasmElements,
                    stringAddressesAndLengthsGlobal = stringAddressesAndLengthsGlobal,
                    wasmLongArrayDeclaration = wasmLongArrayDeclaration,
                    stringLiteralType = StringLiteralType.JsString,
                )
            definedFunctions.add(stringLiteralFunctionJsString)
        }

        val stringLiteralFunctionLatin1 =
            createStringLiteralFunction(
                stringPoolGlobalField = stringPoolField,
                stringEntities = stringEntities,
                additionalTypes = additionalTypes,
                wasmElements = wasmElements,
                stringAddressesAndLengthsGlobal = stringAddressesAndLengthsGlobal,
                wasmLongArrayDeclaration = wasmLongArrayDeclaration,
                stringLiteralType = StringLiteralType.Latin1,
            )
        definedFunctions.add(stringLiteralFunctionLatin1)

        val stringLiteralFunctionUtf16 =
            createStringLiteralFunction(
                stringPoolGlobalField = stringPoolField,
                stringEntities = stringEntities,
                additionalTypes = additionalTypes,
                wasmElements = wasmElements,
                stringAddressesAndLengthsGlobal = stringAddressesAndLengthsGlobal,
                wasmLongArrayDeclaration = wasmLongArrayDeclaration,
                stringLiteralType = StringLiteralType.Utf16,
            )
        definedFunctions.add(stringLiteralFunctionUtf16)

        val startUnitTestsFunction = createStartUnitTestsFunction()
        if (startUnitTestsFunction != null) {
            exports.add(WasmExport.Function("startUnitTests", startUnitTestsFunction))
            definedFunctions.add(startUnitTestsFunction)
        }
    }

    class StringLiteralWasmEntities(
        val createStringFunction: WasmFunction,
        val kotlinStringType: WasmType,
        val wasmCharArrayType: WasmType,
        val wasmCharArrayDeclaration: WasmTypeDeclaration,
        val wasmStringArrayType: WasmArrayDeclaration,
        val stringLiteralFunctionType: WasmFunctionType,
        val stringLiteralJsFunctionType: WasmFunctionType,
    )

    fun getStringLiteralWasmEntities(
        canonicalFunctionTypes: Map<WasmFunctionType, WasmFunctionType>,
        syntheticTypes: MutableList<WasmTypeDeclaration>,
        additionalTypes: MutableList<WasmTypeDeclaration>
    ): StringLiteralWasmEntities {
        val createStringFunction = tryFindBuiltInFunction { it.createString }
            ?: compilationException("kotlin.createString is not file in fragments", null)
        val kotlinStringType = createStringFunction.type.owner.resultTypes[0]

        val wasmCharArrayType = createStringFunction.type.owner.parameterTypes[0]
        val wasmCharArrayDeclaration = (wasmCharArrayType.getHeapType() as WasmHeapType.Type).type.owner
        val wasmStringArrayDeclaration =
            WasmArrayDeclaration("string_array", WasmStructFieldDeclaration("string", kotlinStringType, true))
        additionalTypes.add(wasmStringArrayDeclaration)

        val newStringLiteralFunctionType = WasmFunctionType(listOf(WasmI32), listOf(kotlinStringType))
        val stringLiteralFunctionType = canonicalFunctionTypes[newStringLiteralFunctionType] ?: newStringLiteralFunctionType
        if (stringLiteralFunctionType === newStringLiteralFunctionType) {
            syntheticTypes.add(newStringLiteralFunctionType)
        }

        val newStringLiteralJsFunctionType =
            WasmFunctionType(listOf(WasmI32, WasmRefType(WasmHeapType.Simple.Extern)), listOf(kotlinStringType))
        val stringLiteralJsFunctionType = canonicalFunctionTypes[newStringLiteralJsFunctionType] ?: newStringLiteralJsFunctionType
        if (stringLiteralJsFunctionType === newStringLiteralJsFunctionType) {
            syntheticTypes.add(newStringLiteralJsFunctionType)
        }

        return StringLiteralWasmEntities(
            createStringFunction = createStringFunction,
            kotlinStringType = kotlinStringType,
            wasmCharArrayType = wasmCharArrayType,
            wasmCharArrayDeclaration = wasmCharArrayDeclaration,
            wasmStringArrayType = wasmStringArrayDeclaration,
            stringLiteralFunctionType = stringLiteralFunctionType,
            stringLiteralJsFunctionType = stringLiteralJsFunctionType,
        )
    }

    fun linkWasmCompiledFragments(
        multimoduleOptions: MultimoduleCompileOptions?,
        exceptionTagType: ExceptionTagType
    ): WasmModule {
        // TODO: Implement optimal ir linkage KT-71040
        bindUnboundSymbols()
        val canonicalFunctionTypes = bindUnboundFunctionTypes()

        val data = mutableListOf<WasmData>()
        val stringPoolSize = bindStringPoolSymbolsAndGetSize(data)
        bindConstantArrayDataSegmentIds(data)

        val (definedFunctions, importedFunctions) = partitionDefinedAndImportedFunctions()

        val exports = mutableListOf<WasmExport<*>>()
        wasmCompiledFileFragments.flatMapTo(exports) { it.exports }

        val memories = createAndExportMemory(exports, multimoduleOptions?.stdlibModuleNameForImport)
        val (importedMemories, definedMemories) = memories.partition { it.importPair != null }

        val additionalTypes = mutableListOf<WasmTypeDeclaration>()
        additionalTypes.add(parameterlessNoReturnFunctionType)

        val syntheticTypes = mutableListOf<WasmTypeDeclaration>()
        val stringEntities = getStringLiteralWasmEntities(canonicalFunctionTypes, syntheticTypes, additionalTypes)

        createAndBindSpecialITableTypes(syntheticTypes)
        createAndBindRttiTypeDeclaration(syntheticTypes, stringEntities)

        val globals = getGlobals()
        val stringPoolSizeWithGlobals = bindGlobalLiterals(globals, stringPoolSize)

        val elements = mutableListOf<WasmElement>()
        createAndExportServiceFunctions(
            definedFunctions = definedFunctions,
            stringEntities = stringEntities,
            additionalTypes = additionalTypes,
            stringPoolSize = stringPoolSize,
            initializeUnit = multimoduleOptions?.initializeUnit ?: true,
            stringPoolSizeWithGlobals = stringPoolSizeWithGlobals,
            wasmElements = elements,
            exports = exports,
            globals = globals
        )

        val tags = getTags(exceptionTagType)
        require(tags.size <= 1) { "Having more than 1 tag is not supported" }

        val (importedTags, definedTags) = tags.partition { it.importPair != null }
        tags.forEach { additionalTypes.add(it.type) }

        val (importedGlobals, definedGlobals) = globals.partition { it.importPair != null }

        val importsInOrder = mutableListOf<WasmNamedModuleField>()
        importsInOrder.addAll(importedFunctions)
        importsInOrder.addAll(importedTags)
        importsInOrder.addAll(importedGlobals)
        importsInOrder.addAll(importedMemories)

        val recursiveTypeGroups = getTypes(syntheticTypes, canonicalFunctionTypes, additionalTypes)

        return WasmModule(
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
            startFunction = null,  // Module is initialized via export call
            elements = elements,
            data = data,
            dataCount = true,
            tags = definedTags
        ).apply { calculateIds() }
    }

    private fun createAndBindSpecialITableTypes(syntheticTypes: MutableList<WasmTypeDeclaration>): MutableList<WasmTypeDeclaration> {
        val wasmAnyArrayType = WasmArrayDeclaration(
            name = "AnyArray",
            field = WasmStructFieldDeclaration("", WasmRefNullType(WasmHeapType.Simple.Any), false)
        )
        syntheticTypes.add(wasmAnyArrayType)

        val specialSlotITableTypeSlots = mutableListOf<WasmStructFieldDeclaration>()
        val wasmAnyRefStructField = WasmStructFieldDeclaration("", WasmAnyRef, false)
        repeat(WasmBackendContext.SPECIAL_INTERFACE_TABLE_SIZE) {
            specialSlotITableTypeSlots.add(wasmAnyRefStructField)
        }
        specialSlotITableTypeSlots.add(
            WasmStructFieldDeclaration(
                name = "",
                type = WasmRefNullType(WasmHeapType.Type(WasmSymbol(wasmAnyArrayType))),
                isMutable = false
            )
        )
        val specialSlotITableType = WasmStructDeclaration(
            name = "SpecialITable",
            fields = specialSlotITableTypeSlots,
            superType = null,
            isFinal = true
        )
        syntheticTypes.add(specialSlotITableType)

        wasmCompiledFileFragments.forEach { fragment ->
            fragment.specialITableTypes?.let { specialITableTypes ->
                specialITableTypes.wasmAnyArrayType.bind(wasmAnyArrayType)
                specialITableTypes.specialSlotITableType.bind(specialSlotITableType)
            }
        }

        return syntheticTypes
    }

    private fun getTags(exceptionTagType: ExceptionTagType): List<WasmTag> {
        val exceptionTag = when (exceptionTagType) {
            ExceptionTagType.TRAP -> null
            ExceptionTagType.JS_TAG -> {
                val jsExceptionTagFuncType = WasmFunctionType(
                    parameterTypes = listOf(WasmExternRef),
                    resultTypes = emptyList()
                )
                WasmTag(jsExceptionTagFuncType, WasmImportDescriptor("intrinsics", WasmSymbol("tag")))
            }
            ExceptionTagType.WASM_TAG -> {
                val throwableDeclaration = tryFindBuiltInType { it.throwable }
                    ?: compilationException("kotlin.Throwable is not found in fragments", null)

                val tagFuncType = WasmRefNullType(WasmHeapType.Type(WasmSymbol(throwableDeclaration)))

                val throwableTagFuncType = WasmFunctionType(
                    parameterTypes = listOf(tagFuncType),
                    resultTypes = emptyList()
                )

                WasmTag(throwableTagFuncType)
            }
        }
        return listOfNotNull(exceptionTag)
    }

    private fun getTypes(
        additionalRecGroupTypes: List<WasmTypeDeclaration>,
        canonicalFunctionTypes: Map<WasmFunctionType, WasmFunctionType>,
        additionalTypes: List<WasmTypeDeclaration>,
    ): List<RecursiveTypeGroup> {
        val gcTypes = mutableMapOf<WasmTypeDeclaration, IdSignature>()
        wasmCompiledFileFragments.forEach { fragment -> gcTypes.putAll(fragment.gcTypes.wasmToIr) }
        val vTableGcTypes = wasmCompiledFileFragments.flatMap { it.vTableGcTypes.elements }

        val recGroupTypes = buildList {
            addAll(additionalRecGroupTypes)
            addAll(gcTypes.keys)
            addAll(vTableGcTypes)
            addAll(canonicalFunctionTypes.values)
        }

        val recursiveGroups = createRecursiveTypeGroups(recGroupTypes)

        recursiveGroups.forEach { group ->
            if (group.singleOrNull() is WasmArrayDeclaration) {
                return@forEach
            }

            val needMixIn = group.any { it in gcTypes }
            val needStableSort = needMixIn || group.any { it in vTableGcTypes }

            canonicalSort(group, needStableSort)

            if (needMixIn) {
                val firstGroupGcTypeSignature = group.firstNotNullOfOrNull { gcTypes[it] }
                    ?: compilationException("The group should have gcType to have a mixin", null)

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

        additionalTypes.forEach { recursiveGroups.add(mutableListOf(it)) }
        return recursiveGroups
    }

    private fun createAndBindRttiTypeDeclaration(syntheticTypes: MutableList<WasmTypeDeclaration>, stringEntities: StringLiteralWasmEntities) {
        val wasmLongArray = WasmArrayDeclaration("LongArray", WasmStructFieldDeclaration("Long", WasmI64, false))
        syntheticTypes.add(wasmLongArray)

        val stringLiteralFunctionRef = WasmRefNullType(
            WasmHeapType.Type(
                WasmSymbol(
                    if (isWasmJsTarget)
                        stringEntities.stringLiteralJsFunctionType
                    else
                        stringEntities.stringLiteralFunctionType
                )
            )
        )

        val rttiTypeDeclarationSymbol = WasmSymbol<WasmStructDeclaration>()
        val fieldsList = mutableListOf(
            WasmStructFieldDeclaration("implementedIFaceIds", WasmRefNullType(WasmHeapType.Type(WasmSymbol(wasmLongArray))), false),
            WasmStructFieldDeclaration("superClassRtti", WasmRefNullType(WasmHeapType.Type(rttiTypeDeclarationSymbol)), false),
            WasmStructFieldDeclaration("packageNamePoolId", WasmI32, false),
            WasmStructFieldDeclaration("simpleNamePoolId", WasmI32, false),
            WasmStructFieldDeclaration("klassId", WasmI64, false),
            WasmStructFieldDeclaration("typeInfoFlag", WasmI32, false),
            WasmStructFieldDeclaration("qualifierStringLoader", stringLiteralFunctionRef, false),
            WasmStructFieldDeclaration("simpleNameStringLoader", stringLiteralFunctionRef, false),
        )
        if (isWasmJsTarget) {
            fieldsList += mutableListOf(
                WasmStructFieldDeclaration("packageNameGlobal", WasmRefType(WasmHeapType.Simple.Extern), false),
                WasmStructFieldDeclaration("simpleNameGlobal", WasmRefType(WasmHeapType.Simple.Extern), false),
            )
        }
        val rttiTypeDeclaration = WasmStructDeclaration(
            name = "RTTI",
            fields = fieldsList,
            superType = null,
            isFinal = true
        )
        rttiTypeDeclarationSymbol.bind(rttiTypeDeclaration)
        syntheticTypes.add(rttiTypeDeclaration)

        wasmCompiledFileFragments.forEach { fragment ->
            fragment.rttiElements?.rttiType?.bind(rttiTypeDeclaration)
        }
    }

    private fun getGlobals() = mutableListOf<WasmGlobal>().apply {
        wasmCompiledFileFragments.forEach { fragment ->
            addAll(fragment.globalFields.elements)
            addAll(fragment.globalVTables.elements)
            addAll(fragment.globalClassITables.elements.distinct())
        }


        val rttiGlobals = mutableMapOf<IdSignature, RttiGlobal>()
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.rttiElements?.globals?.forEach { global ->
                rttiGlobals[global.classSignature] = global
            }
        }

        wasmCompiledFileFragments.forEach { fragment ->
            fragment.rttiElements?.run {
                globalReferences.unbound.forEach { unbound ->
                    unbound.value.bind(rttiGlobals[unbound.key]?.global ?: error("A RttiGlobal was not found for ${unbound.key}"))
                }
            }
        }

        fun wasmRttiGlobalOrderKey(rttiGlobal: RttiGlobal?): Int =
            rttiGlobal?.superClassSignature?.let { wasmRttiGlobalOrderKey(rttiGlobals[it]) + 1 } ?: 0

        rttiGlobals.values.sortedBy(::wasmRttiGlobalOrderKey).mapTo(this) { it.global }
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

    private fun createAndExportMasterInitFunction(
        fieldInitializerFunction: WasmFunction,
        tryGetAssociatedObjectAndWrapper: Pair<WasmFunction.Defined, WasmStructDeclaration>?,
        initializeUnit: Boolean,
    ): WasmFunction.Defined {
        val masterInitFunction = WasmFunction.Defined("_initialize", WasmSymbol(parameterlessNoReturnFunctionType))
        with(WasmExpressionBuilder(masterInitFunction.instructions)) {
            if (initializeUnit) {
                val unitGetInstance = tryFindBuiltInFunction { it.unitGetInstance }
                    ?: compilationException("kotlin.Unit_getInstance is not file in fragments", null)
                buildCall(WasmSymbol(unitGetInstance), serviceCodeLocation)
            }

            buildCall(WasmSymbol(fieldInitializerFunction), serviceCodeLocation)

            if (tryGetAssociatedObjectAndWrapper != null) {
                // we do not register descriptor while no need in it
                val registerModuleDescriptor = tryFindBuiltInFunction { it.registerModuleDescriptor }
                    ?: compilationException("kotlin.registerModuleDescriptor is not file in fragments", null)
                buildInstr(WasmOp.REF_FUNC, serviceCodeLocation, WasmImmediate.FuncIdx(WasmSymbol(tryGetAssociatedObjectAndWrapper.first)))
                buildInstr(WasmOp.STRUCT_NEW, serviceCodeLocation, WasmImmediate.GcType(WasmSymbol(tryGetAssociatedObjectAndWrapper.second)))
                buildInstr(WasmOp.CALL, serviceCodeLocation, WasmImmediate.FuncIdx(WasmSymbol(registerModuleDescriptor)))
            }

            wasmCompiledFileFragments.forEach { fragment ->
                fragment.mainFunctionWrappers.forEach { signature ->
                    val wrapperFunction = fragment.functions.defined[signature]
                        ?: compilationException("Cannot find symbol for main wrapper", type = null)
                    buildCall(WasmSymbol(wrapperFunction), serviceCodeLocation)
                }
            }
            buildInstr(WasmOp.RETURN, serviceCodeLocation)
        }
        return masterInitFunction
    }

    private fun createAssociatedObjectGetterFunctionAndWrapper(
        wasmElements: MutableList<WasmElement>,
        additionalTypes: MutableList<WasmTypeDeclaration>
    ): Pair<WasmFunction.Defined, WasmStructDeclaration>? {
        // If AO accessor removed by DCE - we do not need it then
        if (tryFindBuiltInFunction { it.tryGetAssociatedObject } == null) return null

        val kotlinAny = tryFindBuiltInType { it.kotlinAny }
            ?: compilationException("kotlin.Any is not found in fragments", null)

        val nullableAnyWasmType = WasmRefNullType(WasmHeapType.Type(WasmSymbol(kotlinAny)))
        val associatedObjectGetterType = WasmFunctionType(listOf(WasmI64, WasmI64), listOf(nullableAnyWasmType))
        additionalTypes.add(associatedObjectGetterType)

        val associatedObjectGetter = WasmFunction.Defined("_associatedObjectGetter", WasmSymbol(associatedObjectGetterType))
        // Make this function possible to func.ref
        wasmElements.add(
            WasmElement(
                type = WasmFuncRef,
                values = listOf(WasmTable.Value.Function(WasmSymbol(associatedObjectGetter))),
                mode = WasmElement.Mode.Declarative
            )
        )

        val jsToKotlinAnyAdapter by lazy {
            tryFindBuiltInFunction { it.jsToKotlinAnyAdapter }
                ?: compilationException("kotlin.jsToKotlinAnyAdapter is not found in fragments", null)
        }

        val allDefinedFunctions = mutableMapOf<IdSignature, WasmFunction>()
        wasmCompiledFileFragments.forEach { allDefinedFunctions.putAll(it.functions.defined) }

        associatedObjectGetter.instructions.clear()
        with(WasmExpressionBuilder(associatedObjectGetter.instructions)) {
            wasmCompiledFileFragments.forEach { fragment ->
                for ((klassId, associatedObjectsInstanceGetters) in fragment.classAssociatedObjectsInstanceGetters) {
                    buildGetLocal(WasmLocal(0, "classId", WasmI64, true), serviceCodeLocation)
                    buildConstI64(klassId, serviceCodeLocation)
                    buildInstr(WasmOp.I64_EQ, serviceCodeLocation)
                    buildIf("Class matches")
                    associatedObjectsInstanceGetters.forEach { (keyId, getter, isExternal) ->
                        val getterFunction = allDefinedFunctions[getter]
                        if (getterFunction != null) { //Could be deleted with DCE
                            buildGetLocal(WasmLocal(1, "keyId", WasmI64, true), serviceCodeLocation)
                            buildConstI64(keyId, serviceCodeLocation)
                            buildInstr(WasmOp.I64_EQ, serviceCodeLocation)
                            buildIf("Object matches")
                            buildCall(WasmSymbol(getterFunction), serviceCodeLocation)
                            if (isExternal) {
                                buildCall(WasmSymbol(jsToKotlinAnyAdapter), serviceCodeLocation)
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
            WasmRefType(WasmHeapType.Type(WasmSymbol(associatedObjectGetterType)))

        val associatedObjectGetterWrapper = WasmStructDeclaration(
            name = "AssociatedObjectGetterWrapper",
            fields = listOf(WasmStructFieldDeclaration("getter", associatedObjectGetterTypeRef, false)),
            superType = null,
            isFinal = true
        )
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.classAssociatedObjectsGetterWrapper?.bind(associatedObjectGetterWrapper)
        }

        return associatedObjectGetter to associatedObjectGetterWrapper
    }

    private fun createStartUnitTestsFunction(): WasmFunction.Defined? {
        val runRootSuites = tryFindBuiltInFunction { it.runRootSuites } ?: return null
        val startUnitTestsFunction = WasmFunction.Defined("startUnitTests", WasmSymbol(parameterlessNoReturnFunctionType))
        with(WasmExpressionBuilder(startUnitTestsFunction.instructions)) {
            wasmCompiledFileFragments.forEach { fragment ->
                fragment.testFunctionDeclarators.forEach{ declarator ->
                    val declaratorFunction = fragment.functions.defined[declarator]
                        ?: compilationException("Cannot find symbol for test declarator", type = null)
                    buildCall(WasmSymbol(declaratorFunction), serviceCodeLocation)
                }
            }
            buildCall(WasmSymbol(runRootSuites), serviceCodeLocation)
        }
        return startUnitTestsFunction
    }

    private fun createFieldInitializerFunction(
        stringPoolSize: Int,
        stringAddressesAndLengthsGlobal: WasmGlobal,
        wasmLongArrayDeclaration: WasmArrayDeclaration
    ): WasmFunction.Defined {
        val fieldInitializerFunction = WasmFunction.Defined("_fieldInitialize", WasmSymbol(parameterlessNoReturnFunctionType))
        with(WasmExpressionBuilder(fieldInitializerFunction.instructions)) {
            buildConstI32(0, serviceCodeLocation)
            buildConstI32(stringPoolSize, serviceCodeLocation)
            buildInstr(
                WasmOp.ARRAY_NEW_DATA,
                serviceCodeLocation,
                WasmImmediate.GcType(WasmSymbol(wasmLongArrayDeclaration)),
                stringAddressesAndLengthsIndex,
            )
            buildSetGlobal(WasmSymbol(stringAddressesAndLengthsGlobal), serviceCodeLocation)

            wasmCompiledFileFragments.forEach { fragment ->
                fragment.objectInstanceFieldInitializers.forEach { objectInitializer ->
                    val functionSymbol = WasmSymbol(fragment.functions.defined[objectInitializer]!!)
                    buildCall(functionSymbol, serviceCodeLocation)
                }
            }

            wasmCompiledFileFragments.forEach { fragment ->
                fragment.nonConstantFieldInitializers.forEach { nonConstantInitializer ->
                    val functionSymbol = WasmSymbol(fragment.functions.defined[nonConstantInitializer]!!)
                    buildCall(functionSymbol, serviceCodeLocation)
                }
            }
        }
        return fieldInitializerFunction
    }

    private fun stringAddressesAndLengthsField(additionalTypes: MutableList<WasmTypeDeclaration>): Pair<WasmGlobal, WasmArrayDeclaration> {
        val wasmLongArrayDeclaration =
            WasmArrayDeclaration("long_array", WasmStructFieldDeclaration("long", WasmI64, false))
        additionalTypes.add(wasmLongArrayDeclaration)

        val stringAddressesAndLengthsInitializer = listOf(
            wasmInstrWithoutLocation(
                operator = WasmOp.REF_NULL,
                immediate1 = WasmImmediate.HeapType(WasmRefNullrefType)
            ),
        )

        val refAddressesAndLengthsType =
            WasmRefNullType(WasmHeapType.Type(WasmSymbol(wasmLongArrayDeclaration)))

        val global = WasmGlobal("_addressesAndLengths", refAddressesAndLengthsType, true, stringAddressesAndLengthsInitializer)
        return global to wasmLongArrayDeclaration
    }

    private fun createStringPoolField(stringPoolSize: Int, stringEntities: StringLiteralWasmEntities): WasmGlobal {
        val stringCacheFieldInitializer = listOf(
            wasmInstrWithoutLocation(
                operator = WasmOp.I32_CONST,
                immediate1 = WasmImmediate.ConstI32(stringPoolSize),
            ),
            wasmInstrWithoutLocation(
                operator = WasmOp.ARRAY_NEW_DEFAULT,
                immediate1 = WasmImmediate.GcType(stringEntities.wasmStringArrayType)
            ),
        )

        val refToArrayOfNullableStringsType =
            WasmRefType(WasmHeapType.Type(WasmSymbol(stringEntities.wasmStringArrayType)))

        return WasmGlobal("_stringPool", refToArrayOfNullableStringsType, false, stringCacheFieldInitializer)
    }

    private enum class StringLiteralType {
        JsString,
        Latin1,
        Utf16
    }

    private fun createStringLiteralFunction(
        stringPoolGlobalField: WasmGlobal,
        stringEntities: StringLiteralWasmEntities,
        additionalTypes: MutableList<WasmTypeDeclaration>,
        wasmElements: MutableList<WasmElement>,
        stringAddressesAndLengthsGlobal: WasmGlobal,
        wasmLongArrayDeclaration: WasmArrayDeclaration,
        stringLiteralType: StringLiteralType,
    ): WasmFunction.Defined {
        val isJsString = stringLiteralType == StringLiteralType.JsString
        val isLatin1 = stringLiteralType == StringLiteralType.Latin1

        val byteArray = WasmArrayDeclaration("byte_array", WasmStructFieldDeclaration("byte", WasmI8, false))
        additionalTypes.add(byteArray)

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
                stringEntities.stringLiteralJsFunctionType
            else
                stringEntities.stringLiteralFunctionType

        val stringLiteralFunction = WasmFunction.Defined(
            name = "_stringLiteral${stringLiteralType.name}",
            type = WasmSymbol(stringLiteralFunctionType),
            locals = listOfNotNull(poolIdLocal, jsString, startAddress, length, addressAndLength, temporary).toMutableList()
        )
        with(WasmExpressionBuilder(stringLiteralFunction.instructions)) {
            buildBlock("cache_check", stringEntities.kotlinStringType) { blockResult ->
                buildGetGlobal(WasmSymbol(stringPoolGlobalField), serviceCodeLocation)
                buildGetLocal(poolIdLocal, serviceCodeLocation)
                buildInstr(
                    WasmOp.ARRAY_GET,
                    serviceCodeLocation,
                    WasmImmediate.TypeIdx(stringEntities.wasmStringArrayType)
                )
                buildBrInstr(WasmOp.BR_ON_NON_NULL, blockResult, serviceCodeLocation)

                // cache miss
                if (isJsString) {
                    buildGetLocal(jsString ?: error("jsString is not set"), serviceCodeLocation)
                    val jsToKotlinStringAdapter = tryFindBuiltInFunction { it.jsToKotlinStringAdapter }
                    buildCall(WasmSymbol(jsToKotlinStringAdapter), serviceCodeLocation)
                } else {
                    buildGetGlobal(WasmSymbol(stringAddressesAndLengthsGlobal), serviceCodeLocation)
                    buildGetLocal(poolIdLocal, serviceCodeLocation)
                    buildInstr(
                        op = WasmOp.ARRAY_GET,
                        location = serviceCodeLocation,
                        WasmImmediate.TypeIdx(wasmLongArrayDeclaration)
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
                            WasmImmediate.GcType(stringEntities.wasmCharArrayDeclaration), stringDataSectionIndex
                        )
                    } else {
                        val iterator = WasmLocal(localIter++, "intIterator", WasmI32, false)
                        val wasmByteArray = WasmLocal(localIter++, "byteArray", WasmRefType(WasmHeapType.Type(WasmSymbol(byteArray))), false)
                        val wasmCharArray = WasmLocal(localIter++, "charArray", stringEntities.wasmCharArrayType, false)
                        stringLiteralFunction.locals.addAll(listOf(iterator, wasmByteArray, wasmCharArray))

                        buildInstr(
                            op = WasmOp.ARRAY_NEW_DATA,
                            location = serviceCodeLocation,
                            WasmImmediate.GcType(byteArray), stringDataSectionIndex
                        )
                        buildSetLocal(wasmByteArray, serviceCodeLocation)

                        buildGetLocal(length, serviceCodeLocation)
                        buildInstr(
                            op = WasmOp.ARRAY_NEW_DEFAULT,
                            location = serviceCodeLocation,
                            WasmImmediate.GcType(stringEntities.wasmCharArrayDeclaration)
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
                                buildInstr(WasmOp.ARRAY_GET_U, serviceCodeLocation, WasmImmediate.GcType(byteArray))

                                buildInstr(WasmOp.ARRAY_SET, serviceCodeLocation, WasmImmediate.GcType(stringEntities.wasmCharArrayDeclaration))

                                buildGetLocal(iterator, serviceCodeLocation)
                                buildConstI32(1, serviceCodeLocation)
                                buildInstr(WasmOp.I32_ADD, serviceCodeLocation)
                                buildSetLocal(iterator, serviceCodeLocation)
                                buildBr(loop, serviceCodeLocation)
                            }
                        }
                        buildGetLocal(wasmCharArray, serviceCodeLocation)
                    }

                    buildCall(WasmSymbol(stringEntities.createStringFunction), serviceCodeLocation)
                }
                buildSetLocal(temporary, serviceCodeLocation)

                //remember and return string
                buildGetGlobal(WasmSymbol(stringPoolGlobalField), serviceCodeLocation)
                buildGetLocal(poolIdLocal, serviceCodeLocation)
                buildGetLocal(temporary, serviceCodeLocation)
                buildInstr(
                    WasmOp.ARRAY_SET,
                    serviceCodeLocation,
                    WasmImmediate.TypeIdx(stringEntities.wasmStringArrayType)
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

        wasmCompiledFileFragments.forEach { fragment ->
            if (isJsString) {
                fragment.wasmStringsElements?.createStringLiteralJsString?.bind(stringLiteralFunction)
                fragment.wasmStringsElements?.createStringLiteralJsStringType?.bind(stringEntities.stringLiteralJsFunctionType)
            } else if (isLatin1) {
                fragment.wasmStringsElements?.createStringLiteralLatin1?.bind(stringLiteralFunction)
                fragment.wasmStringsElements?.createStringLiteralType?.bind(stringEntities.stringLiteralFunctionType)
            } else {
                fragment.wasmStringsElements?.createStringLiteralUtf16?.bind(stringLiteralFunction)
                fragment.wasmStringsElements?.createStringLiteralType?.bind(stringEntities.stringLiteralFunctionType)
            }
        }

        return stringLiteralFunction
    }

    private fun bindUnboundSymbols() {
        bindFileFragments(wasmCompiledFileFragments, { it.functions.unbound }, { it.functions.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.globalFields.unbound }, { it.globalFields.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.globalVTables.unbound }, { it.globalVTables.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.gcTypes.unbound }, { it.gcTypes.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.vTableGcTypes.unbound }, { it.vTableGcTypes.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.globalClassITables.unbound }, { it.globalClassITables.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.functionTypes.unbound }, { it.functionTypes.defined })
        rebindEquivalentFunctions()
        bindUniqueJsFunNames()
    }

    private fun <IrSymbolType, WasmDeclarationType : Any, WasmSymbolType : WasmSymbol<WasmDeclarationType>> bindFileFragments(
        fragments: List<WasmCompiledFileFragment>,
        unboundSelector: (WasmCompiledFileFragment) -> Map<IrSymbolType, WasmSymbolType>,
        definedSelector: (WasmCompiledFileFragment) -> Map<IrSymbolType, WasmDeclarationType>,
    ) {
        val allDefined = mutableMapOf<IrSymbolType, WasmDeclarationType>()
        fragments.forEach { fragment ->
            definedSelector(fragment).forEach { defined ->
                check(!allDefined.containsKey(defined.key)) {
                    "Redeclaration of symbol ${defined.key}"
                }
                allDefined[defined.key] = defined.value
            }
        }
        for (fragment in fragments) {
            val unbound = unboundSelector(fragment)
            bind(unbound, allDefined)
        }
    }

    private fun bindUnboundFunctionTypes(): Map<WasmFunctionType, WasmFunctionType> {
        // Associate function types to a single canonical function type
        val canonicalFunctionTypes = LinkedHashMap<WasmFunctionType, WasmFunctionType>()
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.functionTypes.elements.associateWithTo(canonicalFunctionTypes) { it }
        }
        // Rebind symbol to canonical
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.functionTypes.unbound.forEach { (_, wasmSymbol) ->
                wasmSymbol.bind(canonicalFunctionTypes.getValue(wasmSymbol.owner))
            }
        }
        return canonicalFunctionTypes
    }

    private fun bindGlobalLiterals(globals: MutableList<WasmGlobal>, stringPoolSize: Int): Int {
        var literalCounter = stringPoolSize
        val literalGlobalSymbolMap = mutableMapOf<String, WasmGlobal>()
        val literalGlobalIdMap = mutableMapOf<String, Int>()
        wasmCompiledFileFragments.forEach { fragment ->
            var globalCounter = 0
            for ((stringValue, stringLiteralSymbol) in fragment.globalLiterals.unbound) {
                var literalGlobal = literalGlobalSymbolMap[stringValue]
                if (literalGlobal == null) {
                    literalGlobal = WasmGlobal(
                        name = "global_${globalCounter++}",
                        type = WasmRefType(WasmHeapType.Simple.Extern),
                        isMutable = false,
                        init = emptyList(),
                        importPair = WasmImportDescriptor("'", WasmSymbol(stringValue))
                    )
                    literalGlobalSymbolMap[stringValue] = literalGlobal
                }
                stringLiteralSymbol.bind(literalGlobal)
            }
            for ((stringValue, literalIdSymbol) in fragment.globalLiteralsIds.unbound) {
                var stringId = literalGlobalIdMap[stringValue]
                if (stringId == null) {
                    stringId = literalCounter
                    literalGlobalIdMap[stringValue] = stringId
                    literalCounter++
                }
                literalIdSymbol.bind(stringId)
            }
        }
        // Add distinct globals to avoid duplicates
        globals.addAll(
            wasmCompiledFileFragments
                .flatMap { it.globalLiterals.unbound.values }
                .map { it.owner }
                .distinct()
        )
        return literalCounter
    }

    private fun bindStringPoolSymbolsAndGetSize(data: MutableList<WasmData>): Int {
        val stringDataSectionBytes = mutableListOf<Byte>()
        var stringDataSectionStart = 0
        val visitedStrings = mutableMapOf<String, Int>()
        val addressesAndLengths = mutableListOf<Long>()
        wasmCompiledFileFragments.forEach { fragment ->
            for ((string, literalIdSymbol) in fragment.stringLiteralId.unbound) {
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
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.constantArrayDataSegmentId.unbound.forEach { (constantArraySegment, symbol) ->
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
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.jsFuns.forEach { jsCodeSnippet ->
                val jsFunName = jsCodeSnippet.value.importName.owner
                val counterValue = jsCodeCounter.getOrPut(jsFunName, defaultValue = { 0 })
                jsCodeCounter[jsFunName] = counterValue + 1
                if (counterValue > 0) {
                    jsCodeSnippet.value.importName.bind("${jsFunName}_$counterValue")
                }
            }
        }
    }

    private fun rebindEquivalentFunctions() {
        val equivalentFunctions = mutableMapOf<String, WasmFunction>()
        wasmCompiledFileFragments.forEach { fragment ->
            for ((signatureString, idSignature) in fragment.equivalentFunctions) {
                val func = equivalentFunctions[signatureString]
                if (func == null) {
                    // First occurrence of the adapter, register it (if not removed by DCE).
                    val functionToUse = fragment.functions.defined[idSignature] ?: continue
                    equivalentFunctions[signatureString] = functionToUse
                } else {
                    // Adapter already exists, remove this one and use the existing adapter.
                    fragment.functions.defined.remove(idSignature)?.let { duplicate ->
                        fragment.functions.elements.remove(duplicate)
                        fragment.functions.wasmToIr.remove(duplicate)
                        fragment.exports.removeAll { it.field == duplicate }
                    }
                    fragment.jsFuns.remove(idSignature)
                    fragment.jsModuleImports.remove(idSignature)

                    // Rebind adapter function to the single instance
                    // There might not be any unbound references in case it's called only from JS side
                    fragment.functions.unbound[idSignature]?.bind(func)
                }
            }
        }
    }
}

fun <IrSymbolType, WasmDeclarationType : Any, WasmSymbolType : WasmSymbol<WasmDeclarationType>> bind(
    unbound: Map<IrSymbolType, WasmSymbolType>,
    defined: Map<IrSymbolType, WasmDeclarationType>
) {
    unbound.forEach { (irSymbol, wasmSymbol) ->
        if (irSymbol !in defined)
            compilationException("Can't link symbol ${irSymbolDebugDump(irSymbol)}", type = null)
        if (!wasmSymbol.isBound()) {
            wasmSymbol.bind(defined.getValue(irSymbol))
        }
    }
}

private fun irSymbolDebugDump(symbol: Any?): String =
    when (symbol) {
        is IrFunctionSymbol -> "function ${symbol.owner.fqNameWhenAvailable}"
        is IrClassSymbol -> "class ${symbol.owner.fqNameWhenAvailable}"
        else -> symbol.toString()
    }

fun alignUp(x: Int, alignment: Int): Int {
    assert(alignment and (alignment - 1) == 0) { "power of 2 expected" }
    return (x + alignment - 1) and (alignment - 1).inv()
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