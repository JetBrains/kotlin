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
    val uniqueJsFunNames: ReferencableElements<String, String> = ReferencableElements(),
    val jsFuns: MutableList<JsCodeSnippet> = mutableListOf(),
    val jsModuleImports: MutableSet<String> = mutableSetOf(),
    val exports: MutableList<WasmExport<*>> = mutableListOf(),
    val scratchMemAddr: WasmSymbol<Int> = WasmSymbol(),
    val stringPoolSize: WasmSymbol<Int> = WasmSymbol(),
    val fieldInitializers: MutableList<Pair<IdSignature, List<WasmInstr>>> = mutableListOf(),
    val mainFunctionWrappers: MutableList<IdSignature> = mutableListOf(),
    var testFun: IdSignature? = null,
    val closureCallExports: MutableList<Pair<String, IdSignature>> = mutableListOf(),
    val jsModuleAndQualifierReferences: Set<JsModuleAndQualifierReference> = mutableSetOf(),
    val throwableTagIndex: WasmSymbol<Int> = WasmSymbol<Int>(),
    val jsExceptionTagIndex: WasmSymbol<Int> = WasmSymbol<Int>(),
) : IrProgramFragment()

class WasmCompiledModuleFragment(
    private val wasmCompiledFileFragments: List<WasmCompiledFileFragment>,
    private val generateTrapsInsteadOfExceptions: Boolean,
    private val itsPossibleToCatchJsErrorSeparately: Boolean
) {
    class JsCodeSnippet(val importName: WasmSymbolReadOnly<String>, val jsCode: String)

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

    private fun <IrSymbolType, WasmDeclarationType : Any, WasmSymbolType : WasmSymbol<WasmDeclarationType>> bindFileFragments(
        fragments: List<WasmCompiledFileFragment>,
        unboundSelector: (WasmCompiledFileFragment) -> Map<IrSymbolType, WasmSymbolType>,
        definedSelector: (WasmCompiledFileFragment) -> Map<IrSymbolType, WasmDeclarationType>,
    ) {
        val allDefined = mutableMapOf<IrSymbolType, WasmDeclarationType>()
        fragments.forEach { allDefined.putAll(definedSelector(it)) }
        for (fragment in fragments) {
            val unbound = unboundSelector(fragment)
            bind(unbound, allDefined)
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
        bindFileFragments(wasmCompiledFileFragments, { it.functions.unbound }, { it.functions.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.globalFields.unbound }, { it.globalFields.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.globalVTables.unbound }, { it.globalVTables.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.gcTypes.unbound }, { it.gcTypes.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.vTableGcTypes.unbound }, { it.vTableGcTypes.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.globalClassITables.unbound }, { it.globalClassITables.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.functionTypes.unbound }, { it.functionTypes.defined })

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

        var interfaceId = 0
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.interfaceIds.unbound.values.forEach { wasmSymbol ->
                wasmSymbol.bind(interfaceId--)
            }
        }

        var currentDataSectionAddress = 0
        val classIds = mutableMapOf<IdSignature, Int>()
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.typeInfo.forEach { (referenceKey, dataElement) ->
                classIds[referenceKey] = currentDataSectionAddress
                currentDataSectionAddress += dataElement.sizeInBytes
            }
        }
        wasmCompiledFileFragments.forEach { fragment ->
            bind(fragment.classIds.unbound, classIds)
        }

        currentDataSectionAddress = alignUp(currentDataSectionAddress, INT_SIZE_BYTES)
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.scratchMemAddr.bind(currentDataSectionAddress)
        }

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

        val data = mutableListOf<WasmData>()
        data.add(WasmData(WasmDataMode.Passive, stringDataSectionBytes.toByteArray()))

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

        val jsCodeCounter = mutableMapOf<String, Int>()
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.uniqueJsFunNames.unbound.forEach { (jsFunName, symbol) ->
                val counterValue = jsCodeCounter.getOrPut(jsFunName, defaultValue = { 0 })
                jsCodeCounter[jsFunName] = counterValue + 1
                val counterSuffix = if (counterValue == 0 && jsFunName.lastOrNull()?.isDigit() == false) "" else "_$counterValue"
                symbol.bind("$jsFunName$counterSuffix")
            }
        }

        val serviceCodeLocation = SourceLocation.NoLocation("Generated service code")

        val parameterlessNoReturnFunctionType = WasmFunctionType(emptyList(), emptyList())

        val fieldInitializerFunction = WasmFunction.Defined("_fieldInitialize", WasmSymbol(parameterlessNoReturnFunctionType))
        with(WasmIrExpressionBuilder(fieldInitializerFunction.instructions)) {
            wasmCompiledFileFragments.forEach { fragment ->
                fragment.fieldInitializers.forEach { (field, initializer) ->
                    val fieldSymbol = WasmSymbol(fragment.globalFields.defined[field])
                    if (fieldSymbol.owner.name == "kotlin.wasm.internal.stringPool") {
                        expression.add(0, WasmInstrWithoutLocation(WasmOp.GLOBAL_SET, listOf(WasmImmediate.GlobalIdx(fieldSymbol))))
                        expression.addAll(0, initializer)
                    } else {
                        expression.addAll(initializer)
                        buildSetGlobal(fieldSymbol, serviceCodeLocation)
                    }
                }
            }
        }

        val masterInitFunction = WasmFunction.Defined("_initialize", WasmSymbol(parameterlessNoReturnFunctionType))
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

        //closureCallExports
//        val visitedClosureCallExports = mutableMapOf<String, WasmSymbol<WasmFunction>>()
//        wasmCompiledFileFragments.forEach { fragment ->
//            fragment.closureCallExports.forEach { (exportSignature, exportFunction) ->
//                val symbol = visitedClosureCallExports.getOrPut(exportSignature) {
//                    val wasmExportFunction = fragment.functions.defined[exportFunction] ?: error("Cannot find export function")
//                    WasmSymbol(wasmExportFunction)
//                }
//                //Rebind export function
//                fragment.functions.unbound[exportFunction]!!.bind(symbol)
//            }
//        }

        //OPT
        //TODO(CREATE NEW STARTUNITTEST?)
        val startUnitTests = wasmCompiledFileFragments.firstNotNullOfOrNull { fragment ->
            fragment.functions.defined.values.find { it.name == "kotlin.test.startUnitTests" }
        }
        check(startUnitTests is WasmFunction.Defined)
        with(WasmIrExpressionBuilder(startUnitTests.instructions)) {
            wasmCompiledFileFragments.forEach { fragment ->
                val signature = fragment.testFun
                if (signature != null) {
                    val testRunner = fragment.functions.defined[signature] ?: error("Cannot find symbol for test runner")
                    buildCall(WasmSymbol(testRunner), serviceCodeLocation)
                }
            }
        }

        val exports = mutableListOf<WasmExport<*>>()

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

        exports += WasmExport.Function("_initialize", masterInitFunction)

        val typeInfoSize = currentDataSectionAddress
        val memorySizeInPages = (typeInfoSize / 65_536) + 1
        val memory = WasmMemory(WasmLimits(memorySizeInPages.toUInt(), null /* "unlimited" */))

        // Need to export the memory in order to pass complex objects to the host language.
        // Export name "memory" is a WASI ABI convention.
        exports += WasmExport.Memory("memory", memory)

        fun wasmTypeDeclarationOrderKey(declaration: WasmTypeDeclaration): Int {
            return when (declaration) {
                is WasmArrayDeclaration -> 0
                is WasmFunctionType -> 0
                is WasmStructDeclaration ->
                    // Subtype depth
                    declaration.superType?.let { wasmTypeDeclarationOrderKey(it.owner) + 1 } ?: 0
            }
        }

        val recGroupTypes = mutableListOf<WasmTypeDeclaration>()
        val globals = mutableListOf<WasmGlobal>()
        wasmCompiledFileFragments.forEach { fragment ->
            recGroupTypes.addAll(fragment.vTableGcTypes.elements)
            recGroupTypes.addAll(fragment.gcTypes.elements)
            recGroupTypes.addAll(fragment.classITableGcType.unbound.values.mapNotNull { it.takeIf { it.isBound() }?.owner })
            globals.addAll(fragment.globalFields.elements)
            globals.addAll(fragment.globalVTables.elements)
            globals.addAll(fragment.globalClassITables.elements.distinct())
        }
        recGroupTypes.sortBy(::wasmTypeDeclarationOrderKey)

        //OPT
        //TODO(FIND THROWABLE)
        val throwableDeclaration = wasmCompiledFileFragments.firstNotNullOfOrNull { fragment ->
            fragment.gcTypes.defined.values.find { it.name == "kotlin.Throwable" }
        }
        check(throwableDeclaration != null)
        val throwableTagFuncType = WasmFunctionType(
            listOf(
                WasmRefNullType(WasmHeapType.Type(WasmSymbol(throwableDeclaration)))
            ),
            emptyList()
        )
        val jsExceptionTagFuncType = WasmFunctionType(
            listOf(WasmExternRef),
            emptyList()
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
        val importedFunctions = wasmCompiledFileFragments.flatMap {
            it.functions.elements.filterIsInstance<WasmFunction.Imported>()
        }
        val importsInOrder = importedFunctions + importedTags

        val allFunctionTypes = canonicalFunctionTypes.values.toList() + parameterlessNoReturnFunctionType + throwableTagFuncType + jsExceptionTagFuncType

        // Partition out function types that can't be recursive,
        // we don't need to put them into a rec group
        // so that they can be matched with function types from other Wasm modules.
        val (potentiallyRecursiveFunctionTypes, nonRecursiveFunctionTypes) =
            allFunctionTypes.partition { it.referencesTypeDeclarations() }
        recGroupTypes.addAll(potentiallyRecursiveFunctionTypes)

        val definedFunctions = wasmCompiledFileFragments.flatMap { it.functions.elements.filterIsInstance<WasmFunction.Defined>() }

        val module = WasmModule(
            functionTypes = nonRecursiveFunctionTypes,
            recGroupTypes = recGroupTypes,
            importsInOrder = importsInOrder,
            importedFunctions = importedFunctions,
            importedTags = importedTags,
            definedFunctions = definedFunctions + fieldInitializerFunction + masterInitFunction,
            tables = emptyList(),
            memories = listOf(memory),
            globals = globals,
            exports = exports,
            startFunction = null,  // Module is initialized via export call
            elements = emptyList(),
            data = data,
            dataCount = true,
            tags = definedTags
        )
        module.calculateIds()
        return module
    }
}

fun <IrSymbolType, WasmDeclarationType : Any, WasmSymbolType : WasmSymbol<WasmDeclarationType>> bind(
    unbound: Map<IrSymbolType, WasmSymbolType>,
    defined: Map<IrSymbolType, WasmDeclarationType>
) {
    unbound.forEach { (irSymbol, wasmSymbol) ->
        if (irSymbol !in defined)
            error("Can't link symbol ${irSymbolDebugDump(irSymbol)}")
        wasmSymbol.bind(defined.getValue(irSymbol))
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