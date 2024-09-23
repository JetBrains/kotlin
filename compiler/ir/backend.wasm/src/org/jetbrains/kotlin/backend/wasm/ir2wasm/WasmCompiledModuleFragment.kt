/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment.*
import org.jetbrains.kotlin.backend.wasm.utils.DisjointUnions
import org.jetbrains.kotlin.ir.backend.js.ic.IrICProgramFragment
import org.jetbrains.kotlin.backend.common.serialization.Hash128Bits
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

class BuiltinIdSignatures(
    val throwable: IdSignature?,
    val tryGetAssociatedObject: IdSignature?,
    val jsToKotlinAnyAdapter: IdSignature?,
    val unitGetInstance: IdSignature?,
    val runRootSuites: IdSignature?,
)

class WasmCompiledFileFragment(
    val fragmentTag: String?,
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
    val jsFuns: MutableMap<IdSignature, JsCodeSnippet> = mutableMapOf(),
    val jsModuleImports: MutableMap<IdSignature, String> = mutableMapOf(),
    val exports: MutableList<WasmExport<*>> = mutableListOf(),
    var scratchMemAddr: WasmSymbol<Int>? = null,
    var stringPoolSize: WasmSymbol<Int>? = null,
    var throwableTagIndex: WasmSymbol<Int>? = null,
    var jsExceptionTagIndex: WasmSymbol<Int>? = null,
    val fieldInitializers: MutableList<FieldInitializer> = mutableListOf(),
    val mainFunctionWrappers: MutableList<IdSignature> = mutableListOf(),
    var testFunctionDeclarators: MutableList<IdSignature> = mutableListOf(),
    val equivalentFunctions: MutableList<Pair<String, IdSignature>> = mutableListOf(),
    val jsModuleAndQualifierReferences: MutableSet<JsModuleAndQualifierReference> = mutableSetOf(),
    val classAssociatedObjectsInstanceGetters: MutableList<ClassAssociatedObjects> = mutableListOf(),
    var builtinIdSignatures: BuiltinIdSignatures? = null,
) : IrICProgramFragment()

class WasmCompiledModuleFragment(
    private val wasmCompiledFileFragments: List<WasmCompiledFileFragment>,
    private val generateTrapsInsteadOfExceptions: Boolean,
    private val itsPossibleToCatchJsErrorSeparately: Boolean
) {
    // Used during linking
    private val serviceCodeLocation = SourceLocation.NoLocation("Generated service code")
    private val parameterlessNoReturnFunctionType = WasmFunctionType(emptyList(), emptyList())

    private inline fun tryFindBuiltInFunction(select: (BuiltinIdSignatures) -> IdSignature?): WasmFunction.Defined? {
        for (fragment in wasmCompiledFileFragments) {
            val builtinSignatures = fragment.builtinIdSignatures ?: continue
            val signature = select(builtinSignatures) ?: continue
            return fragment.functions.defined[signature] as? WasmFunction.Defined
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

    private fun createAndExportServiceFunctions(definedFunctions: MutableList<WasmFunction.Defined>, exports: MutableList<WasmExport<*>>) {
        val fieldInitializerFunction = createFieldInitializerFunction()
        definedFunctions.add(fieldInitializerFunction)

        val masterInitFunction = createAndExportMasterInitFunction(fieldInitializerFunction)
        exports.add(WasmExport.Function("_initialize", masterInitFunction))
        definedFunctions.add(masterInitFunction)

        val startUnitTestsFunction = createStartUnitTestsFunction()
        if (startUnitTestsFunction != null) {
            exports.add(WasmExport.Function("startUnitTests", startUnitTestsFunction))
            definedFunctions.add(startUnitTestsFunction)
        }
    }

    fun linkWasmCompiledFragments(): WasmModule {
        // TODO: Implement optimal ir linkage KT-71040
        bindUnboundSymbols()
        val canonicalFunctionTypes = bindUnboundFunctionTypes()

        val (typeIds, scratchAddress) = bindTypeIds()
        bindScratchMemAddr(scratchAddress)
        createTryGetAssociatedObjectFunction(typeIds)

        val data = mutableListOf<WasmData>()
        bindStringPoolSymbols(data)
        bindConstantArrayDataSegmentIds(data)
        addCompileTimePerClassData(data, typeIds)

        val (definedFunctions, importedFunctions) = partitionDefinedAndImportedFunctions()

        val exports = mutableListOf<WasmExport<*>>()
        wasmCompiledFileFragments.flatMapTo(exports) { it.exports }

        val memory = createAndExportMemory(scratchAddress, exports)

        createAndExportServiceFunctions(definedFunctions, exports)

        val tags = getTags()
        val (importedTags, definedTags) = tags.partition { it.importPair != null }
        val importsInOrder = importedFunctions + importedTags

        val additionalTypes = mutableListOf<WasmTypeDeclaration>()
        additionalTypes.add(parameterlessNoReturnFunctionType)
        tags.forEach { additionalTypes.add(it.type) }

        val recursiveTypeGroups = getTypes(canonicalFunctionTypes, additionalTypes)

        return WasmModule(
            recGroups = recursiveTypeGroups,
            importsInOrder = importsInOrder,
            importedFunctions = importedFunctions,
            definedFunctions = definedFunctions,
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

    private fun getTags(): List<WasmTag> {
        val throwableDeclaration = tryFindBuiltInType { it.throwable }
            ?: compilationException("kotlin.Throwable is not found in fragments", null)
        val tagFuncType = WasmRefNullType(WasmHeapType.Type(WasmSymbol(throwableDeclaration)))

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
            it.throwableTagIndex?.bind(throwableTagIndex)
        }
        val jsExceptionTagIndex = tags.indexOfFirst { it.type === jsExceptionTagFuncType }
        wasmCompiledFileFragments.forEach {
            it.jsExceptionTagIndex?.bind(jsExceptionTagIndex)
        }

        return tags
    }

    private fun getTypes(
        canonicalFunctionTypes: Map<WasmFunctionType, WasmFunctionType>,
        additionalTypes: RecursiveTypeGroup,
    ): List<RecursiveTypeGroup> {
        val vTablesAndGcTypes =
            wasmCompiledFileFragments.flatMap { it.vTableGcTypes.elements } +
                    wasmCompiledFileFragments.flatMap { it.gcTypes.elements }

        val recGroupTypes = sequence {
            yieldAll(vTablesAndGcTypes)
            wasmCompiledFileFragments.forEach { fragment ->
                yieldAll(fragment.classITableGcType.unbound.values.mapNotNull { it.takeIf { it.isBound() }?.owner })
            }
            yieldAll(canonicalFunctionTypes.values)
        }

        val recursiveGroups = createRecursiveTypeGroups(recGroupTypes)

        val mixInIndexesForGroups = mutableMapOf<Hash128Bits, Int>()
        val groupsWithMixIns = mutableListOf<RecursiveTypeGroup>()
        recursiveGroups.mapTo(groupsWithMixIns) { group ->
            if (group.all { it !in vTablesAndGcTypes }) {
                group
            } else {
                addMixInGroup(group, mixInIndexesForGroups)
            }
        }

        groupsWithMixIns.add(additionalTypes)

        return groupsWithMixIns
    }

    private fun getGlobals() = mutableListOf<WasmGlobal>().apply {
        wasmCompiledFileFragments.forEach { fragment ->
            addAll(fragment.globalFields.elements)
            addAll(fragment.globalVTables.elements)
            addAll(fragment.globalClassITables.elements.distinct())
        }
    }

    private fun createAndExportMemory(scratchAddress: Int, exports: MutableList<WasmExport<*>>): WasmMemory {
        val memorySizeInPages = (scratchAddress / 65_536) + 1
        val memory = WasmMemory(WasmLimits(memorySizeInPages.toUInt(), null /* "unlimited" */))

        // Need to export the memory in order to pass complex objects to the host language.
        // Export name "memory" is a WASI ABI convention.
        val exportMemory = WasmExport.Memory("memory", memory)
        exports.add(exportMemory)
        return memory
    }

    private fun createAndExportMasterInitFunction(fieldInitializerFunction: WasmFunction): WasmFunction.Defined {
        val unitGetInstance = tryFindBuiltInFunction { it.unitGetInstance }
            ?: compilationException("kotlin.Unit_getInstance is not file in fragments", null)

        val masterInitFunction = WasmFunction.Defined("_initialize", WasmSymbol(parameterlessNoReturnFunctionType))
        with(WasmExpressionBuilder(masterInitFunction.instructions)) {
            buildCall(WasmSymbol(unitGetInstance), serviceCodeLocation)
            buildCall(WasmSymbol(fieldInitializerFunction), serviceCodeLocation)
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

    private fun createTryGetAssociatedObjectFunction(typeIds: Map<IdSignature, Int>) {
        val tryGetAssociatedObject = tryFindBuiltInFunction { it.tryGetAssociatedObject } ?: return // Removed by DCE
        val jsToKotlinAnyAdapter by lazy {
            tryFindBuiltInFunction { it.jsToKotlinAnyAdapter }
                ?: compilationException("kotlin.jsToKotlinAnyAdapter is not found in fragments", null)
        }

        val allDefinedFunctions = mutableMapOf<IdSignature, WasmFunction>()
        wasmCompiledFileFragments.forEach { allDefinedFunctions.putAll(it.functions.defined) }

        tryGetAssociatedObject.instructions.clear()
        with(WasmExpressionBuilder(tryGetAssociatedObject.instructions)) {
            wasmCompiledFileFragments.forEach { fragment ->
                for ((klass, associatedObjectsInstanceGetters) in fragment.classAssociatedObjectsInstanceGetters) {
                    val klassId = typeIds[klass] ?: continue // Can be removed by dce so no reflection for this
                    buildGetLocal(WasmLocal(0, "classId", WasmI32, true), serviceCodeLocation)
                    buildConstI32(klassId, serviceCodeLocation)
                    buildInstr(WasmOp.I32_EQ, serviceCodeLocation)
                    buildIf("Class matches")
                    associatedObjectsInstanceGetters.forEach { (obj, getter, isExternal) ->
                        val keyId = typeIds[obj]!!
                        buildGetLocal(WasmLocal(1, "keyId", WasmI32, true), serviceCodeLocation)
                        buildConstI32(keyId, serviceCodeLocation)
                        buildInstr(WasmOp.I32_EQ, serviceCodeLocation)
                        buildIf("Object matches")
                        buildCall(WasmSymbol(allDefinedFunctions[getter]!!), serviceCodeLocation)
                        if (isExternal) {
                            buildCall(WasmSymbol(jsToKotlinAnyAdapter), serviceCodeLocation)
                        }
                        buildInstr(WasmOp.RETURN, serviceCodeLocation)
                        buildEnd()
                    }
                    buildEnd()
                }
            }
            buildRefNull(WasmHeapType.Simple.None, serviceCodeLocation)
            buildInstr(WasmOp.RETURN, serviceCodeLocation)
        }
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

    private fun createFieldInitializerFunction(): WasmFunction.Defined {
        val fieldInitializerFunction = WasmFunction.Defined("_fieldInitialize", WasmSymbol(parameterlessNoReturnFunctionType))
        with(WasmExpressionBuilder(fieldInitializerFunction.instructions)) {
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
            } ?: compilationException("stringPool initializer not found!", type = null)
        }
        return fieldInitializerFunction
    }

    private fun addCompileTimePerClassData(data: MutableList<WasmData>, typeIds: Map<IdSignature, Int>) {
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.typeInfo.forEach { (referenceKey, typeInfo) ->
                val instructions = mutableListOf<WasmInstr>()
                WasmExpressionBuilder(instructions).buildConstI32(
                    typeIds.getValue(referenceKey),
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

    private fun bindTypeIds(): Pair<Map<IdSignature, Int>, Int> {
        val typeIds = mutableMapOf<IdSignature, Int>()
        var currentDataSectionAddress = 0

        wasmCompiledFileFragments.forEach { fragment ->
            fragment.typeInfo.forEach { (referenceKey, dataElement) ->
                typeIds[referenceKey] = currentDataSectionAddress
                currentDataSectionAddress += dataElement.sizeInBytes
            }
        }

        wasmCompiledFileFragments.forEach { fragment ->
            bind(fragment.classIds.unbound, typeIds)
        }

        var interfaceId = 0
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.interfaceIds.unbound.forEach { (signature, symbol) ->
                val id = typeIds.getOrPut(signature) { interfaceId-- }
                symbol.bind(id)
            }
        }

        currentDataSectionAddress = alignUp(currentDataSectionAddress, INT_SIZE_BYTES)
        return typeIds to currentDataSectionAddress
    }

    private fun bindScratchMemAddr(scratchAddress: Int) {
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.scratchMemAddr?.bind(scratchAddress)
        }
    }

    private fun bindStringPoolSymbols(data: MutableList<WasmData>) {
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

                    val constData = ConstantDataCharArray(string.toCharArray())
                    stringDataSectionBytes += constData.toBytes().toList()
                    stringDataSectionStart += constData.sizeInBytes
                } else {
                    currentStringAddress = addressAndId.first
                    currentStringId = addressAndId.second
                }

                val literalPoolIdSymbol = fragment.stringLiteralPoolId.unbound[string]
                    ?: compilationException("String symbol expected", type = null)
                literalAddressSymbol.bind(currentStringAddress)
                literalPoolIdSymbol.bind(currentStringId)
            }
        }

        wasmCompiledFileFragments.forEach { fragment ->
            fragment.stringPoolSize?.bind(stringAddressAndId.size)
        }

        data.add(WasmData(WasmDataMode.Passive, stringDataSectionBytes.toByteArray()))
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
    val isExternal: Boolean,
)