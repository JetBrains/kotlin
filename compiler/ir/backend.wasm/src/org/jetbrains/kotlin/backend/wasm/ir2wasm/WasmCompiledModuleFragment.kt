/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment.*
import org.jetbrains.kotlin.backend.wasm.utils.DisjointUnions
import org.jetbrains.kotlin.ir.backend.js.ic.IrProgramFragment
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

class WasmCompiledFileFragment(
    val functions: ReferencableAndDefinable<IdSignature, WasmFunction> = ReferencableAndDefinable(),
    val globalFields: ReferencableAndDefinable<IdSignature, WasmGlobal> = ReferencableAndDefinable(),
    val globalVTables: ReferencableAndDefinable<IdSignature, WasmGlobal> = ReferencableAndDefinable(),
    val globalClassITables: ReferencableAndDefinable<IdSignature, WasmGlobal> = ReferencableAndDefinable(),
    val functionTypes: ReferencableAndDefinable<IdSignature, WasmFunctionType> = ReferencableAndDefinable(),
    val gcTypes: ReferencableAndDefinable<IdSignature, WasmTypeDeclaration> = ReferencableAndDefinable(),
    val vTableGcTypes: ReferencableAndDefinable<IdSignature, WasmTypeDeclaration> = ReferencableAndDefinable(),
    val classITableGcType: ReferencableElements<IdSignature, WasmTypeDeclaration> = ReferencableElements(),
    val classITableInterfaceSlot: ReferencableElements<IdSignature, Int> = ReferencableElements(),
    val classITableInterfaceTableSize: ReferencableElements<IdSignature, Int> = ReferencableElements(),
    val classITableInterfaceHasImplementors: ReferencableElements<IdSignature, Int> = ReferencableElements(),
    val typeInfo: MutableMap<IdSignature, ConstantDataElement> = mutableMapOf(),
    val classIds: ReferencableElements<IdSignature, Int> = ReferencableElements(),
    val interfaceIds: ReferencableElements<IdSignature, Int> = ReferencableElements(),
    val stringLiteralAddress: ReferencableElements<String, Int> = ReferencableElements(),
    val stringLiteralPoolId: ReferencableElements<String, Int> = ReferencableElements(),
    val constantArrayDataSegmentId: ReferencableElements<Pair<List<Long>, WasmType>, Int> = ReferencableElements(),
    val interfaceUnions: MutableList<List<IdSignature>> = mutableListOf(),
    val jsFuns: MutableList<JsCodeSnippet> = mutableListOf(),
    val jsModuleImports: MutableSet<String> = mutableSetOf(),
    val exports: MutableList<WasmExport<*>> = mutableListOf(),
    val scratchMemAddr: WasmSymbol<Int> = WasmSymbol(),
    val stringPoolSize: WasmSymbol<Int> = WasmSymbol(),
    val throwableTagIndex: WasmSymbol<Int> = WasmSymbol<Int>(),
    val jsExceptionTagIndex: WasmSymbol<Int> = WasmSymbol<Int>(),
    val fieldInitializers: MutableList<FieldInitializer> = mutableListOf(),
    val mainFunctionWrappers: MutableList<IdSignature> = mutableListOf(),
    var testFun: IdSignature? = null,
    val closureCallExports: MutableList<Pair<String, IdSignature>> = mutableListOf(),
    val jsModuleAndQualifierReferences: MutableSet<JsModuleAndQualifierReference> = mutableSetOf(),
    val classAssociatedObjectsInstanceGetters: MutableList<ClassAssociatedObjects> = mutableListOf(),
    var tryGetAssociatedObjectFun: IdSignature? = null,
) : IrProgramFragment()

class WasmCompiledModuleFragment(
    private val wasmCompiledFileFragments: List<WasmCompiledFileFragment>,
    private val generateTrapsInsteadOfExceptions: Boolean,
    private val itsPossibleToCatchJsErrorSeparately: Boolean
) {

    // Used during linking
    private val serviceCodeLocation = SourceLocation.NoLocation("Generated service code")
    private val parameterlessNoReturnFunctionType = WasmFunctionType(emptyList(), emptyList())
    private val canonicalFunctionTypes = LinkedHashMap<WasmFunctionType, WasmFunctionType>()
    private val classIds = mutableMapOf<IdSignature, Int>()
    private val data = mutableListOf<WasmData>()
    private val exports = mutableListOf<WasmExport<*>>()
    private val fieldInitializerFunction = WasmFunction.Defined("_fieldInitialize", WasmSymbol(parameterlessNoReturnFunctionType))
    private val masterInitFunction = WasmFunction.Defined("_initialize", WasmSymbol(parameterlessNoReturnFunctionType))
    private val startUnitTestsFunction = WasmFunction.Defined("kotlin.test.startUnitTests", WasmSymbol(parameterlessNoReturnFunctionType))
    private var memory: WasmMemory = WasmMemory(WasmLimits(0U, 0U), null)
    private var currentDataSectionAddress = 0
    private val tryGetAssociatedObjectFunction = run {
        // If null, then removed by DCE
        val fragment = wasmCompiledFileFragments.firstOrNull { it.tryGetAssociatedObjectFun != null }
        fragment?.functions?.defined?.get(fragment.tryGetAssociatedObjectFun)
    } as? WasmFunction.Defined


    class JsCodeSnippet(val importName: WasmSymbol<String>, val jsCode: String)

    open class ReferencableElements<Ir, Wasm : Any>(
        val unbound: MutableMap<Ir, WasmSymbol<Wasm>> = mutableMapOf()
    ) {
        fun reference(ir: Ir): WasmSymbol<Wasm> {
            val declaration = (ir as? IrSymbol)?.owner as? IrDeclarationWithName
            if (declaration != null) {
                val packageFragment = declaration.getPackageFragment()
                if (packageFragment is IrExternalPackageFragment) {
                    error("Referencing declaration without package fragment ${declaration.fqNameWhenAvailable}")
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
                error("Trying to redefine element: IR: $ir Wasm: $wasm")

            elements += wasm
            defined[ir] = wasm
            wasmToIr[wasm] = ir
        }
    }

    fun createInterfaceTablesAndLinkTableSymbols() {
        val disjointUnions = DisjointUnions<IdSignature>()
        for (fileFragment in wasmCompiledFileFragments) {
            for (iFaces in fileFragment.interfaceUnions) {
                disjointUnions.addUnion(iFaces)
            }
        }

        disjointUnions.compress()

        val iSlots = mutableMapOf<IdSignature, Int>()
        val iTableGcType = mutableMapOf<IdSignature, WasmStructDeclaration>()

        val iTableGcTypesRefs = ReferencableElements<IdSignature, WasmTypeDeclaration>()

        for (union in disjointUnions.allUnions()) {
            val fields = union.mapIndexed { index, unionIFace ->
                iSlots[unionIFace] = index
                WasmStructFieldDeclaration(
                    name = "${unionIFace.packageFqName().asString()}.itable",
                    type = WasmRefNullType(WasmHeapType.Type(iTableGcTypesRefs.reference(unionIFace))),
                    isMutable = false
                )
            }

            val struct = WasmStructDeclaration(
                name = "classITable",
                fields = fields,
                superType = null,
                isFinal = true,
            )

            union.forEach {
                iTableGcType[it] = struct
            }
        }

        // Binding iTable structures
        for (fileFragment in wasmCompiledFileFragments) {
            for (unbound in fileFragment.classITableInterfaceHasImplementors.unbound) {
                unbound.value.bind(if (unbound.key in disjointUnions) 1 else 0)
            }
            for (unbound in fileFragment.classITableInterfaceTableSize.unbound) {
                if (unbound.key in disjointUnions) {
                    unbound.value.bind(disjointUnions[unbound.key].size)
                }
            }
            for (unbound in fileFragment.classITableInterfaceSlot.unbound) {
                val iSlot = iSlots[unbound.key]
                if (iSlot != null) {
                    unbound.value.bind(iSlot)
                }
            }
            for (unbound in fileFragment.classITableGcType.unbound) {
                val gcType = iTableGcType[unbound.key]
                if (gcType != null) {
                    unbound.value.bind(gcType)
                }
            }

            for (unbound in iTableGcTypesRefs.unbound) {
                val vTable = fileFragment.vTableGcTypes.defined[unbound.key]
                if (vTable != null) {
                    unbound.value.bind(vTable)
                }
            }
        }
    }

    fun linkWasmCompiledFragments(): WasmModule {
        bindUnboundSymbols()
        addCompileTimePerClassData()
        handleExports()
        createTryGetAssociatedObjectFunction()

        val (recGroupTypes, nonRecursiveFunctionTypes, importedFunctions, importsInOrder, definedTags) = getTypes()

        return WasmModule(
            functionTypes = nonRecursiveFunctionTypes,
            recGroupTypes = recGroupTypes,
            importsInOrder = importsInOrder,
            importedFunctions = importedFunctions,
            definedFunctions = getDefinedFunctions(),
            tables = emptyList(),
            memories = listOf(memory),
            globals = getGlobals(),
            exports = exports,
            startFunction = null,  // Module is initialized via export call
            elements = emptyList(),
            data = data,
            dataCount = true,
            tags = definedTags
        ).apply { calculateIds() }
    }

    private fun getDefinedFunctions() = wasmCompiledFileFragments.flatMap {
        it.functions.elements.filterIsInstance<WasmFunction.Defined>()
    } + fieldInitializerFunction + masterInitFunction + startUnitTestsFunction

    private fun getImportedFunctions() = wasmCompiledFileFragments.flatMap {
        it.functions.elements.filterIsInstance<WasmFunction.Imported>()
    }

    private fun getTypes(): WasmTypes {
        val recGroupTypes = getRecGroupTypesWithoutPotentiallyRecursiveFunctionTypes()

        val tagFuncType = getThrowableRefType()

        val throwableTagFuncType = WasmFunctionType(
            parameterTypes = listOf(tagFuncType),
            resultTypes = emptyList()
        )
        val jsExceptionTagFuncType = WasmFunctionType(
            parameterTypes = listOf(WasmExternRef),
            resultTypes = emptyList()
        )
        val tags = listOfNotNull(
            runIf(!generateTrapsInsteadOfExceptions && itsPossibleToCatchJsErrorSeparately) {
                WasmTag(jsExceptionTagFuncType, WasmImportDescriptor("intrinsics", WasmSymbol("js_error_tag")))
            },
            runIf(!generateTrapsInsteadOfExceptions) { WasmTag(throwableTagFuncType) }
        )
        val throwableTagIndex = tags.indexOfFirst { it.type === throwableTagFuncType }
        wasmCompiledFileFragments.forEach {
            it.throwableTagIndex.bind(throwableTagIndex)
        }
        val jsExceptionTagIndex = tags.indexOfFirst { it.type === jsExceptionTagFuncType }
        wasmCompiledFileFragments.forEach {
            it.jsExceptionTagIndex.bind(jsExceptionTagIndex)
        }

        val (importedTags, definedTags) = tags.partition { it.importPair != null }
        val importedFunctions = getImportedFunctions()
        val importsInOrder = importedFunctions + importedTags

        val allFunctionTypes = canonicalFunctionTypes.values.toList() + parameterlessNoReturnFunctionType + throwableTagFuncType + jsExceptionTagFuncType

        // Partition out function types that can't be recursive that don't need to be put into a
        //  rec group so that they can be matched with function types from other Wasm modules.
        val (potentiallyRecursiveFunctionTypes, nonRecursiveFunctionTypes) = allFunctionTypes.partition { it.referencesTypeDeclarations() }
        recGroupTypes.addAll(potentiallyRecursiveFunctionTypes)

        return WasmTypes(recGroupTypes, nonRecursiveFunctionTypes, importedFunctions, importsInOrder, definedTags)
    }

    private fun getThrowableRefType(): WasmType {
        //OPT
        //TODO(FIND THROWABLE)
        val throwableDeclaration = wasmCompiledFileFragments.firstNotNullOfOrNull { fragment ->
            fragment.gcTypes.defined.values.find { it.name == "kotlin.Throwable" }
        }
        check(throwableDeclaration != null)
        return WasmRefNullType(WasmHeapType.Type(WasmSymbol(throwableDeclaration)))
    }

    private fun getRecGroupTypesWithoutPotentiallyRecursiveFunctionTypes(): MutableList<WasmTypeDeclaration> {
        fun wasmTypeDeclarationOrderKey(declaration: WasmTypeDeclaration): Int {
            return when (declaration) {
                is WasmArrayDeclaration -> 0
                is WasmFunctionType -> 0
                is WasmStructDeclaration ->
                    // Subtype depth
                    declaration.superType?.let { wasmTypeDeclarationOrderKey(it.owner) + 1 } ?: 0
            }
        }

        val recGroupTypes = mutableSetOf<WasmTypeDeclaration>()
        wasmCompiledFileFragments.forEach { fragment ->
            recGroupTypes.addAll(fragment.vTableGcTypes.elements)
            recGroupTypes.addAll(fragment.gcTypes.elements)
            recGroupTypes.addAll(fragment.classITableGcType.unbound.values.mapNotNull { it.takeIf { it.isBound() }?.owner })
        }

        val recGroupTypesList = recGroupTypes.toMutableList()
        recGroupTypesList.sortBy(::wasmTypeDeclarationOrderKey)
        return recGroupTypesList
    }

    private fun getGlobals() = mutableListOf<WasmGlobal>().apply {
        wasmCompiledFileFragments.forEach { fragment ->
            addAll(fragment.globalFields.elements)
            addAll(fragment.globalVTables.elements)
            addAll(fragment.globalClassITables.elements.distinct())
        }
    }

    private fun createAndExportMemory() {
        val typeInfoSize = currentDataSectionAddress
        val memorySizeInPages = (typeInfoSize / 65_536) + 1
        memory = WasmMemory(WasmLimits(memorySizeInPages.toUInt(), null /* "unlimited" */))

        // Need to export the memory in order to pass complex objects to the host language.
        // Export name "memory" is a WASI ABI convention.
        exports += WasmExport.Memory("memory", memory)
    }

    private fun resolveExportedFunctionsClashes() {
        //TODO Better way to resolve clashed exports (especially for adapters)
        val exportNames = mutableMapOf<String, Int>()
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.exports.forEach { export ->
                if (export is WasmExport.Function) {
                    val exportNumber = exportNames[export.name]
                    if (exportNumber == null) {
                        exports.add(export)
                        exportNames[export.name] = 1
                    } else {
                        val renamedExport = WasmExport.Function("${export.name}_$exportNumber", export.field)
                        exports.add(renamedExport)
                        exportNames[export.name] = exportNumber + 1
                    }
                } else {
                    exports.add(export)
                }
            }
        }
    }

    private fun handleExports() {
        // The clashes are resolved first because clashes are currently resolved by renaming functions and
        //  this will be problematic for the exported functions that are expected to be with an exact name
        resolveExportedFunctionsClashes()
        createMasterInitFunction()
        createStartUnitTestsFunction()
        exports += WasmExport.Function("_initialize", masterInitFunction)
        exports += WasmExport.Function("startUnitTests", startUnitTestsFunction)
        createAndExportMemory()
    }

    private fun createMasterInitFunction() {
        createFieldInitializerFunction()

        masterInitFunction.instructions.clear()
        with(WasmIrExpressionBuilder(masterInitFunction.instructions)) {
            buildCall(WasmSymbol(fieldInitializerFunction), serviceCodeLocation)
            wasmCompiledFileFragments.forEach { fragment ->
                fragment.mainFunctionWrappers.forEach { signature ->
                    val wrapperFunction = fragment.functions.defined[signature] ?: error("Cannot find symbol for main wrapper")
                    buildCall(WasmSymbol(wrapperFunction), serviceCodeLocation)
                }
            }
            buildInstr(WasmOp.RETURN, serviceCodeLocation)
        }
    }

    private fun createTryGetAssociatedObjectFunction() {
        if (tryGetAssociatedObjectFunction == null) {
            // Removed by DCE
            return
        }
        val location = serviceCodeLocation

        val allDefinedFunctions = mutableMapOf<IdSignature, WasmFunction>()
        wasmCompiledFileFragments.forEach { allDefinedFunctions.putAll(it.functions.defined) }

        tryGetAssociatedObjectFunction.instructions.clear()
        with(WasmIrExpressionBuilder(tryGetAssociatedObjectFunction.instructions)) {
            wasmCompiledFileFragments.forEach { fragment ->
                for ((klass, associatedObjectsInstanceGetters) in fragment.classAssociatedObjectsInstanceGetters) {
                    val klassId = classIds[klass]!!
                    buildGetLocal(WasmLocal(0, "classId", WasmI32, true), location)
                    buildConstI32(klassId, location)
                    buildInstr(WasmOp.I32_EQ, location)
                    buildIf("Class matches")
                    associatedObjectsInstanceGetters.forEach { (obj, getter) ->
                        val keyId = classIds[obj]!!
                        buildGetLocal(WasmLocal(1, "keyId", WasmI32, true), location)
                        buildConstI32(keyId, location)
                        buildInstr(WasmOp.I32_EQ, location)
                        buildIf("Object matches")
                        buildCall(WasmSymbol(allDefinedFunctions[getter]!!), location)
                        buildInstr(WasmOp.RETURN, location)
                        buildEnd()
                    }
                    buildEnd()
                }
            }
            buildRefNull(WasmHeapType.Simple.None, location)
            buildInstr(WasmOp.RETURN, serviceCodeLocation)
        }
    }

    private fun createStartUnitTestsFunction() {
        startUnitTestsFunction.instructions.clear()
        with(WasmIrExpressionBuilder(startUnitTestsFunction.instructions)) {
            wasmCompiledFileFragments.forEach { fragment ->
                val signature = fragment.testFun
                if (signature != null) {
                    val testRunner = fragment.functions.defined[signature] ?: error("Cannot find symbol for test runner")
                    buildCall(WasmSymbol(testRunner), serviceCodeLocation)
                }
            }
        }
    }

    private fun createFieldInitializerFunction() {
        fieldInitializerFunction.instructions.clear()
        with(WasmIrExpressionBuilder(fieldInitializerFunction.instructions)) {
            var stringPoolInitializer: Pair<FieldInitializer, WasmSymbol<WasmGlobal>>? = null
            wasmCompiledFileFragments.forEach { fragment ->
                fragment.fieldInitializers.forEach { initializer ->
                    val fieldSymbol = WasmSymbol(fragment.globalFields.defined[initializer.field])
                    if (fieldSymbol.owner.name == "kotlin.wasm.internal.stringPool") {
                        stringPoolInitializer = initializer to fieldSymbol
                    } else {
                        if (initializer.isObjectInstanceField) {
                            expression.add(0, WasmInstrWithoutLocation(WasmOp.GLOBAL_SET, listOf(WasmImmediate.GlobalIdx(fieldSymbol))))
                            expression.addAll(0, initializer.instructions)
                        } else {
                            expression.addAll(initializer.instructions)
                            buildSetGlobal(fieldSymbol, serviceCodeLocation)
                        }
                    }
                }
            }
            stringPoolInitializer?.let {
                expression.add(0, WasmInstrWithoutLocation(WasmOp.GLOBAL_SET, listOf(WasmImmediate.GlobalIdx(it.second))))
                expression.addAll(0, it.first.instructions)
            } ?: error("stringPool initializer not found!")
        }
    }

    private fun addCompileTimePerClassData() {
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.typeInfo.forEach { (referenceKey, typeInfo) ->
                val instructions = mutableListOf<WasmInstr>()
                WasmIrExpressionBuilder(instructions).buildConstI32(
                    classIds.getValue(referenceKey),
                    SourceLocation.NoLocation("Compile time data per class")
                )
                val typeData = WasmData(
                    WasmDataMode.Active(0, instructions),
                    typeInfo.toBytes()
                )
                data.add(typeData)
            }
        }
    }

    private fun bindUnboundSymbols() {
        bindFileFragments(wasmCompiledFileFragments, { it.functions.unbound }, { it.functions.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.globalFields.unbound }, { it.globalFields.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.globalVTables.unbound }, { it.globalVTables.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.gcTypes.unbound }, { it.gcTypes.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.vTableGcTypes.unbound }, { it.vTableGcTypes.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.globalClassITables.unbound }, { it.globalClassITables.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.functionTypes.unbound }, { it.functionTypes.defined })
        bindUnboundFunctionTypes()
        bindInterfaceIds()
        bindClassIds()
        bindScratchMemAddr()
        bindStringPoolSymbols()
        bindConstantArrayDataSegmentIds()
        bindUniqueJsFunNames()
        bindClosureCallsToSingleAdapterAcrossFiles()
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

    private fun bindUnboundFunctionTypes() {
        // Associate function types to a single canonical function type
        canonicalFunctionTypes.clear()
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.functionTypes.elements.associateWithTo(canonicalFunctionTypes) { it }
        }
        // Rebind symbol to canonical
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.functionTypes.unbound.forEach { (_, wasmSymbol) ->
                wasmSymbol.bind(canonicalFunctionTypes.getValue(wasmSymbol.owner))
            }
        }
    }

    private fun bindInterfaceIds() {
        var interfaceId = 0
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.interfaceIds.unbound.values.forEach { wasmSymbol ->
                wasmSymbol.bind(interfaceId--)
            }
        }
    }

    private fun bindClassIds() {
        classIds.clear()
        currentDataSectionAddress = 0
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.typeInfo.forEach { (referenceKey, dataElement) ->
                classIds[referenceKey] = currentDataSectionAddress
                currentDataSectionAddress += dataElement.sizeInBytes
            }
        }
        wasmCompiledFileFragments.forEach { fragment ->
            bind(fragment.classIds.unbound, classIds)
        }
    }

    private fun bindScratchMemAddr() {
        currentDataSectionAddress = alignUp(currentDataSectionAddress, INT_SIZE_BYTES)
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.scratchMemAddr.bind(currentDataSectionAddress)
        }
    }

    private fun bindStringPoolSymbols() {
        data.clear()
        val stringDataSectionBytes = mutableListOf<Byte>()
        var stringDataSectionStart = 0
        val stringAddressAndId = mutableMapOf<String, Pair<Int, Int>>()
        wasmCompiledFileFragments.forEach { fragment ->
            for ((string, literalAddressSymbol) in fragment.stringLiteralAddress.unbound) {
                val currentStringAddress: Int
                val currentStringId: Int
                val addressAndId = stringAddressAndId[string]
                if (addressAndId == null) {
                    currentStringAddress = stringDataSectionStart
                    currentStringId = stringAddressAndId.size
                    stringAddressAndId[string] = currentStringAddress to currentStringId

                    val constData = ConstantDataCharArray("string_literal", string.toCharArray())
                    stringDataSectionBytes += constData.toBytes().toList()
                    stringDataSectionStart += constData.sizeInBytes
                } else {
                    currentStringAddress = addressAndId.first
                    currentStringId = addressAndId.second
                }

                val literalPoolIdSymbol = fragment.stringLiteralPoolId.unbound[string] ?: error("String symbol expected")
                literalAddressSymbol.bind(currentStringAddress)
                literalPoolIdSymbol.bind(currentStringId)
            }
        }

        wasmCompiledFileFragments.forEach { fragment ->
            fragment.stringPoolSize.bind(stringAddressAndId.size)
        }

        data.add(WasmData(WasmDataMode.Passive, stringDataSectionBytes.toByteArray()))
    }

    private fun bindConstantArrayDataSegmentIds() {
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
                val constData = ConstantDataIntegerArray("constant_array", constantArraySegment.first, integerSize)
                data.add(WasmData(WasmDataMode.Passive, constData.toBytes()))
            }
        }
    }

    private fun bindUniqueJsFunNames() {
        val jsCodeCounter = mutableMapOf<String, Int>()
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.jsFuns.forEach { jsCodeSnippet ->
                val jsFunName = jsCodeSnippet.importName.owner
                val counterValue = jsCodeCounter.getOrPut(jsFunName, defaultValue = { 0 })
                jsCodeCounter[jsFunName] = counterValue + 1
                if (counterValue > 0) {
                    jsCodeSnippet.importName.bind("${jsFunName}_$counterValue")
                }
            }
        }
    }

    /**
     * private fun foo(f: (String) -> String): String = js("f() + 1")
     * private fun bar(h: (String) -> String): String = js("h() + 2")
     * For the two functions above, an adapter for the closures f and h will be
     *  created for them to be callable from JS code. Since the signature of both
     *  f and h are the same, the same adapter will be used for both of them. This
     *  is fine within a single file, but when dealing with multiple files, it's a
     *  problem if the same adapter is defined more than once.
     * Given that adapters with same signature are similar across files, this function
     *  binds all calls to a closure adapter in all file fragments to a single adapter.
     * For more details about the per-file adapter generation, take a look at
     *  [org.jetbrains.kotlin.backend.wasm.lower.JsInteropFunctionsLowering]
     */
    private fun bindClosureCallsToSingleAdapterAcrossFiles() {
        val existingClosureCallAdapters = mutableMapOf<String, WasmSymbol<WasmFunction>>()
        wasmCompiledFileFragments.forEach { fragment ->
            for ((signatureString, idSignature) in fragment.closureCallExports) {
                var symbol = existingClosureCallAdapters[signatureString]
                if (symbol == null) {
                    // First occurrence of the adapter, register it (if not removed by DCE).
                    val func = fragment.functions.defined[idSignature] ?: continue
                    symbol = WasmSymbol(func)
                    existingClosureCallAdapters[signatureString] = symbol
                } else {
                    // Adapter already exists, remove this one and use the existing adapter.
                    fragment.functions.defined.remove(idSignature)?.let { duplicate ->
                        fragment.functions.elements.remove(duplicate)
                        fragment.functions.wasmToIr.remove(duplicate)
                        fragment.exports.removeAll { it.field == duplicate }
                    }
                }
                // Rebind adapter function to the single instance
                // There might not be any unbound references in case it's called only from JS side
                fragment.functions.unbound[idSignature]?.bind(symbol)
            }
        }
    }

    private data class WasmTypes(
        val recGroupTypes: List<WasmTypeDeclaration>,
        val nonRecursiveFunctionTypes: List<WasmFunctionType>,
        val importedFunctions: List<WasmFunction.Imported>,
        val importsInOrder: List<WasmNamedModuleField>,
        val definedTags: List<WasmTag>
    )
}

fun <IrSymbolType, WasmDeclarationType : Any, WasmSymbolType : WasmSymbol<WasmDeclarationType>> bind(
    unbound: Map<IrSymbolType, WasmSymbolType>,
    defined: Map<IrSymbolType, WasmDeclarationType>
) {
    unbound.forEach { (irSymbol, wasmSymbol) ->
        if (irSymbol !in defined)
            error("Can't link symbol ${irSymbolDebugDump(irSymbol)}")
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

class FieldInitializer(
    val field: IdSignature,
    val instructions: List<WasmInstr>,
    val isObjectInstanceField: Boolean
)

data class ClassAssociatedObjects(
    val klass: IdSignature,
    val objects: List<AssociatedObject>
)

data class AssociatedObject(
    val obj: IdSignature,
    val getterFunc: IdSignature,
)