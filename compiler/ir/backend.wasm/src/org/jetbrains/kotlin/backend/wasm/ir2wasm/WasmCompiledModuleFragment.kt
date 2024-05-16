/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment.*
import org.jetbrains.kotlin.backend.wasm.utils.DisjointUnions
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPackageFragment
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
    val classITableGcType: ReferencableAndDefinable<IdSignature, WasmTypeDeclaration> = ReferencableAndDefinable(),
    val classITableInterfaceSlot: ReferencableAndDefinable<IdSignature, Int> = ReferencableAndDefinable(),
    val classITableInterfaceTableSize: ReferencableAndDefinable<IdSignature, Int> = ReferencableAndDefinable(),
    val classITableInterfaceHasImplementors: ReferencableAndDefinable<IdSignature, Int> = ReferencableAndDefinable(),
    val typeInfo: MutableMap<IdSignature, ConstantDataElement> = mutableMapOf(),
    val classIds: ReferencableElements<IdSignature, Int> = ReferencableElements(),
    val interfaceIds: ReferencableElements<IdSignature, Int> = ReferencableElements(),
    val stringLiteralAddress: ReferencableElements<String, Int> = ReferencableElements(),
    val stringLiteralPoolId: ReferencableElements<String, Int> = ReferencableElements(),
    val constantArrayDataSegmentId: ReferencableElements<Pair<List<Long>, WasmType>, Int> = ReferencableElements(),
    val interfaceUnions: MutableList<List<IdSignature>> = mutableListOf(),
    val declaredInterfaces: MutableList<IdSignature> = mutableListOf(),
    val initFunctions: MutableList<FunWithPriority> = mutableListOf(),
    val uniqueJsFunNames: ReferencableElements<String, String> = ReferencableElements(),
    val jsFuns: MutableList<JsCodeSnippet> = mutableListOf(),
    val jsModuleImports: MutableSet<String> = mutableSetOf(),
    val exports: MutableList<WasmExport<*>> = mutableListOf(),
    val scratchMemAddr: WasmSymbol<Int> = WasmSymbol(),
    val stringPoolSize: WasmSymbol<Int> = WasmSymbol()
)

class WasmCompiledModuleFragment(
    private val wasmCompiledFileFragments: List<WasmCompiledFileFragment>,
    private val irBuiltIns: IrBuiltIns,
    private val generateTrapsInsteadOfExceptions: Boolean,
) {
    class JsCodeSnippet(val importName: WasmSymbolReadOnly<String>, val jsCode: String)

    class FunWithPriority(val function: WasmFunction, val priority: String)

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

    fun createInterfaceTables(idSignatureRetriever: IdSignatureRetriever) {
        val disjointUnions = DisjointUnions<IdSignature>()
        for (fileFragment in wasmCompiledFileFragments) {
            for (iFaces in fileFragment.interfaceUnions) {
                disjointUnions.addUnion(iFaces)
            }
        }
        disjointUnions.compress()

        for (fileFragment in wasmCompiledFileFragments) {
            createInterfaceTables(
                fileFragment.declaredInterfaces,
                WasmFileCodegenContext(fileFragment, idSignatureRetriever),
                disjointUnions
            )
        }
    }

    private fun createInterfaceTables(
        declaredInterfaces: List<IdSignature>,
        context: WasmFileCodegenContext,
        disjointUnions: DisjointUnions<IdSignature>
    ) {
        val visited = mutableSetOf<IdSignature>()

        val invalidStruct = WasmStructDeclaration("<INVALID>", emptyList(), null, false)

        for (iFace in declaredInterfaces) {
            if (visited.contains(iFace)) continue

            if (iFace !in disjointUnions) {
                context.defineClassITableGcType(iFace, invalidStruct)
                context.defineClassITableInterfaceHasImplementors(iFace, 0)
                context.defineClassITableInterfaceTableSize(iFace, -1)
                context.defineClassITableInterfaceSlot(iFace, -1)
                continue
            }

            val disjointUnion = disjointUnions[iFace]

            val fields = disjointUnion.mapIndexed { index, unionIFace ->
                context.defineClassITableInterfaceSlot(unionIFace, index)
                WasmStructFieldDeclaration(
                    name = "${unionIFace.packageFqName().asString()}.itable",
                    type = WasmRefNullType(WasmHeapType.Type(context.referenceVTableGcType(unionIFace))),
                    isMutable = false
                )
            }

            val struct = WasmStructDeclaration(
                name = "classITable",
                fields = fields,
                superType = null,
                isFinal = true,
            )

            disjointUnion.forEach {
                context.defineClassITableGcType(it, struct)
                context.defineClassITableInterfaceTableSize(it, disjointUnion.size)
                context.defineClassITableInterfaceHasImplementors(it, 1)
                visited.add(it)
            }
        }
    }



    fun linkWasmCompiledFragments(): WasmModule {
        bindFileFragments(wasmCompiledFileFragments, { it.functions.unbound }, { it.functions.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.globalFields.unbound }, { it.globalFields.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.globalVTables.unbound }, { it.globalVTables.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.gcTypes.unbound }, { it.gcTypes.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.vTableGcTypes.unbound }, { it.vTableGcTypes.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.classITableGcType.unbound }, { it.classITableGcType.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.classITableInterfaceSlot.unbound }, { it.classITableInterfaceSlot.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.globalClassITables.unbound }, { it.globalClassITables.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.classITableInterfaceTableSize.unbound }, { it.classITableInterfaceTableSize.defined })
        bindFileFragments(wasmCompiledFileFragments, { it.classITableInterfaceHasImplementors.unbound }, { it.classITableInterfaceHasImplementors.defined })

        bindFileFragments(wasmCompiledFileFragments, { it.functionTypes.unbound }, { it.functionTypes.defined })

        // Associate function types to a single canonical function type
        val canonicalFunctionTypes = LinkedHashMap<WasmFunctionType, WasmFunctionType>()
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.functionTypes.elements.associateWithTo(canonicalFunctionTypes) { it }
        }
        // Rebind symbol to canonical
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.functionTypes.unbound.forEach { (irSymbol, wasmSymbol) ->
//                if (irSymbol !in fragment.functionTypes.defined)
//                    error("Can't link symbol ${irSymbolDebugDump(irSymbol)}")
                wasmSymbol.bind(canonicalFunctionTypes.getValue(wasmSymbol.owner))
            }
        }

        //FILEWISE
        var interfaceId = 0
        wasmCompiledFileFragments.forEach { fragment ->
            fragment.interfaceIds.unbound.values.forEach { wasmSymbol ->
                wasmSymbol.bind(interfaceId--)
            }
        }

        //FILEWISE
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
        var stringLiteralCount = 0
        //OPT!
        val allStrings = mutableSetOf<String>()
        wasmCompiledFileFragments.flatMapTo(allStrings) { it.stringLiteralAddress.unbound.keys }
        allStrings.forEach { string ->
            wasmCompiledFileFragments.forEach { fragment ->
                val literalAddress = fragment.stringLiteralAddress.unbound[string]
                if (literalAddress != null) {
                    val literalPoolId = fragment.stringLiteralPoolId.unbound[string] ?: error("String symbol expected")
                    literalAddress.bind(stringDataSectionStart)
                    literalPoolId.bind(stringLiteralCount)
                    val constData = ConstantDataCharArray("string_literal", string.toCharArray())
                    stringDataSectionBytes += constData.toBytes().toList()
                    stringDataSectionStart += constData.sizeInBytes
                    stringLiteralCount++
                }
            }
        }

        wasmCompiledFileFragments.forEach { fragment ->
            fragment.stringPoolSize.bind(stringLiteralCount)
        }

        val data = mutableListOf<WasmData>()
        data.add(WasmData(WasmDataMode.Passive, stringDataSectionBytes.toByteArray()))

        //FILEWISE
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

        //FILE WISE
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

        val masterInitFunctionType = WasmFunctionType(emptyList(), emptyList())
        val masterInitFunction = WasmFunction.Defined("_initialize", WasmSymbol(masterInitFunctionType))
        with(WasmIrExpressionBuilder(masterInitFunction.instructions)) {
            wasmCompiledFileFragments.forEach { fragment ->
                fragment.initFunctions.sortedBy { it.priority }.forEach {
                    buildCall(WasmSymbol(it.function), SourceLocation.NoLocation("Generated service code"))
                }
            }
        }

        val exports = mutableListOf<WasmExport<*>>()
        wasmCompiledFileFragments.flatMapTo(exports) { it.exports }
        exports += WasmExport.Function("_initialize", masterInitFunction)

        val typeInfoSize = currentDataSectionAddress
        val memorySizeInPages = (typeInfoSize / 65_536) + 1
        val memory = WasmMemory(WasmLimits(memorySizeInPages.toUInt(), null /* "unlimited" */))

        // Need to export the memory in order to pass complex objects to the host language.
        // Export name "memory" is a WASI ABI convention.
        exports += WasmExport.Memory("memory", memory)

        val importedFunctions = wasmCompiledFileFragments.flatMap {
            it.functions.elements.filterIsInstance<WasmFunction.Imported>()
        }

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
            recGroupTypes.addAll(fragment.classITableGcType.elements.distinct())
            globals.addAll(fragment.globalFields.elements)
            globals.addAll(fragment.globalVTables.elements)
            globals.addAll(fragment.globalClassITables.elements.distinct())
        }
        recGroupTypes.sortBy(::wasmTypeDeclarationOrderKey)

        //OPT
        val throwableSignature = irBuiltIns.throwableClass.signature!!
        val throwableDeclaration = wasmCompiledFileFragments.firstNotNullOfOrNull { fragment -> fragment.gcTypes.defined[throwableSignature] }
        check(throwableDeclaration != null)
        val tagFuncType = WasmFunctionType(
            listOf(WasmRefNullType(WasmHeapType.Type(WasmSymbol(throwableDeclaration)))),
            emptyList()
        )
        val tags = if (generateTrapsInsteadOfExceptions) emptyList() else listOf(WasmTag(tagFuncType))


        val allFunctionTypes = canonicalFunctionTypes.values.toList() + tagFuncType + masterInitFunctionType

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
            importsInOrder = importedFunctions,
            importedFunctions = importedFunctions,
            definedFunctions = definedFunctions + masterInitFunction,
            tables = emptyList(),
            memories = listOf(memory),
            globals = globals,
            exports = exports,
            startFunction = null,  // Module is initialized via export call
            elements = emptyList(),
            data = data,
            dataCount = true,
            tags = tags
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