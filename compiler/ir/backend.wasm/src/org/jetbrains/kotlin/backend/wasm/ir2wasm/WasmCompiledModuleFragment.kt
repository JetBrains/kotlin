/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

class WasmCompiledModuleFragment(
    val irBuiltIns: IrBuiltIns,
    generateTrapsInsteadOfExceptions: Boolean,
    itsPossibleToCatchJsErrorSeparately: Boolean
) {
    val functions =
        ReferencableAndDefinable<IrFunctionSymbol, WasmFunction>()
    val globalFields =
        ReferencableAndDefinable<IrFieldSymbol, WasmGlobal>()
    val globalVTables =
        ReferencableAndDefinable<IrClassSymbol, WasmGlobal>()
    val globalClassITables =
        ReferencableAndDefinable<IrClassSymbol, WasmGlobal>()
    val functionTypes =
        ReferencableAndDefinable<IrFunctionSymbol, WasmFunctionType>()
    val gcTypes =
        ReferencableAndDefinable<IrClassSymbol, WasmTypeDeclaration>()
    val vTableGcTypes =
        ReferencableAndDefinable<IrClassSymbol, WasmTypeDeclaration>()
    val classITableGcType =
        ReferencableAndDefinable<IrClassSymbol, WasmTypeDeclaration>()
    val classITableInterfaceSlot =
        ReferencableAndDefinable<IrClassSymbol, Int>()
    val typeIds =
        ReferencableElements<IrClassSymbol, Int>()
    val stringLiteralAddress =
        ReferencableElements<String, Int>()
    val stringLiteralPoolId =
        ReferencableElements<String, Int>()
    val constantArrayDataSegmentId =
        ReferencableElements<Pair<List<Long>, WasmType>, Int>()

    internal val throwableTagFuncType = WasmFunctionType(
        listOf(
            WasmRefNullType(WasmHeapType.Type(gcTypes.reference(irBuiltIns.throwableClass)))
        ),
        emptyList()
    )

    internal val jsExceptionTagFuncType = WasmFunctionType(
        listOf(WasmExternRef),
        emptyList()
    )

    val tags = listOfNotNull(
        runIf(!generateTrapsInsteadOfExceptions && itsPossibleToCatchJsErrorSeparately) {
            WasmTag(jsExceptionTagFuncType, WasmImportDescriptor("intrinsics", WasmSymbol("js_error_tag")))
        },
        runIf(!generateTrapsInsteadOfExceptions) { WasmTag(throwableTagFuncType) }
    )

    val typeInfo = ReferencableAndDefinable<IrClassSymbol, ConstantDataElement>()

    val exports = mutableListOf<WasmExport<*>>()

    class JsCodeSnippet(val importName: String, val jsCode: String)

    val jsFuns = mutableListOf<JsCodeSnippet>()
    val jsModuleImports = mutableSetOf<String>()

    class FunWithPriority(val function: WasmFunction, val priority: String)

    val initFunctions = mutableListOf<FunWithPriority>()

    val scratchMemAddr = WasmSymbol<Int>()

    val stringPoolSize = WasmSymbol<Int>()

    open class ReferencableElements<Ir, Wasm : Any> {
        val unbound = mutableMapOf<Ir, WasmSymbol<Wasm>>()
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

    class ReferencableAndDefinable<Ir, Wasm : Any> : ReferencableElements<Ir, Wasm>() {
        fun define(ir: Ir, wasm: Wasm) {
            if (ir in defined)
                error("Trying to redefine element: IR: $ir Wasm: $wasm")

            elements += wasm
            defined[ir] = wasm
            wasmToIr[wasm] = ir
        }

        val defined = LinkedHashMap<Ir, Wasm>()
        val elements = mutableListOf<Wasm>()

        val wasmToIr = mutableMapOf<Wasm, Ir>()
    }

    fun linkWasmCompiledFragments(): WasmModule {
        bind(functions.unbound, functions.defined)
        bind(globalFields.unbound, globalFields.defined)
        bind(globalVTables.unbound, globalVTables.defined)
        bind(gcTypes.unbound, gcTypes.defined)
        bind(vTableGcTypes.unbound, vTableGcTypes.defined)
        bind(classITableGcType.unbound, classITableGcType.defined)
        bind(classITableInterfaceSlot.unbound, classITableInterfaceSlot.defined)
        bind(globalClassITables.unbound, globalClassITables.defined)

        // Associate function types to a single canonical function type
        val canonicalFunctionTypes =
            functionTypes.elements.associateWithTo(LinkedHashMap()) { it }

        functionTypes.unbound.forEach { (irSymbol, wasmSymbol) ->
            if (irSymbol !in functionTypes.defined)
                error("Can't link symbol ${irSymbolDebugDump(irSymbol)}")
            wasmSymbol.bind(canonicalFunctionTypes.getValue(functionTypes.defined.getValue(irSymbol)))
        }

        var currentDataSectionAddress = 0
        var interfaceId = 0
        typeIds.unbound.forEach { (klassSymbol, wasmSymbol) ->
            if (klassSymbol.owner.isInterface) {
                interfaceId--
                wasmSymbol.bind(interfaceId)
            } else {
                wasmSymbol.bind(currentDataSectionAddress)
                currentDataSectionAddress += typeInfo.defined.getValue(klassSymbol).sizeInBytes
            }
        }
        currentDataSectionAddress = alignUp(currentDataSectionAddress, INT_SIZE_BYTES)
        scratchMemAddr.bind(currentDataSectionAddress)

        val stringDataSectionBytes = mutableListOf<Byte>()
        var stringDataSectionStart = 0
        var stringLiteralCount = 0
        for ((string, symbol) in stringLiteralAddress.unbound) {
            symbol.bind(stringDataSectionStart)
            stringLiteralPoolId.reference(string).bind(stringLiteralCount)
            val constData = ConstantDataCharArray("string_literal", string.toCharArray())
            stringDataSectionBytes += constData.toBytes().toList()
            stringDataSectionStart += constData.sizeInBytes
            stringLiteralCount++
        }
        stringPoolSize.bind(stringLiteralCount)

        val data = mutableListOf<WasmData>()
        data.add(WasmData(WasmDataMode.Passive, stringDataSectionBytes.toByteArray()))
        constantArrayDataSegmentId.unbound.forEach { (constantArraySegment, symbol) ->
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

        typeIds.unbound.forEach { (klassSymbol, typeId) ->
            if (!klassSymbol.owner.isInterface) {
                val instructions = mutableListOf<WasmInstr>()
                WasmIrExpressionBuilder(instructions).buildConstI32(
                    typeId.owner,
                    SourceLocation.NoLocation("Compile time data per class")
                )
                val typeData = WasmData(
                    WasmDataMode.Active(0, instructions),
                    typeInfo.defined.getValue(klassSymbol).toBytes()
                )
                data.add(typeData)
            }
        }

        val masterInitFunctionType = WasmFunctionType(emptyList(), emptyList())
        val masterInitFunction = WasmFunction.Defined("_initialize", WasmSymbol(masterInitFunctionType))
        with(WasmIrExpressionBuilder(masterInitFunction.instructions)) {
            initFunctions.sortedBy { it.priority }.forEach {
                buildCall(WasmSymbol(it.function), SourceLocation.NoLocation("Generated service code"))
            }
        }
        exports += WasmExport.Function("_initialize", masterInitFunction)

        val typeInfoSize = currentDataSectionAddress
        val memorySizeInPages = (typeInfoSize / 65_536) + 1
        val memory = WasmMemory(WasmLimits(memorySizeInPages.toUInt(), null /* "unlimited" */))

        // Need to export the memory in order to pass complex objects to the host language.
        // Export name "memory" is a WASI ABI convention.
        exports += WasmExport.Memory("memory", memory)

        val (importedTags, definedTags) = tags.partition { it.importPair != null }
        val importedFunctions = functions.elements.filterIsInstance<WasmFunction.Imported>()
        val importsInOrder = importedFunctions + importedTags

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
        recGroupTypes.addAll(vTableGcTypes.elements)
        recGroupTypes.addAll(this.gcTypes.elements)
        recGroupTypes.addAll(classITableGcType.elements.distinct())
        recGroupTypes.sortBy(::wasmTypeDeclarationOrderKey)

        val globals = mutableListOf<WasmGlobal>()
        globals.addAll(globalFields.elements)
        globals.addAll(globalVTables.elements)
        globals.addAll(globalClassITables.elements.distinct())

        val allFunctionTypes = canonicalFunctionTypes.values.toList() + throwableTagFuncType + jsExceptionTagFuncType + masterInitFunctionType

        // Partition out function types that can't be recursive,
        // we don't need to put them into a rec group
        // so that they can be matched with function types from other Wasm modules.
        val (potentiallyRecursiveFunctionTypes, nonRecursiveFunctionTypes) =
            allFunctionTypes.partition { it.referencesTypeDeclarations() }
        recGroupTypes.addAll(potentiallyRecursiveFunctionTypes)

        val module = WasmModule(
            functionTypes = nonRecursiveFunctionTypes,
            recGroupTypes = recGroupTypes,
            importsInOrder = importsInOrder,
            importedFunctions = importedFunctions,
            importedTags = importedTags,
            definedFunctions = functions.elements.filterIsInstance<WasmFunction.Defined>() + masterInitFunction,
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