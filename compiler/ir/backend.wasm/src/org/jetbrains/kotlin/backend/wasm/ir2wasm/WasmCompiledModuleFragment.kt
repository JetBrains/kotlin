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
import org.jetbrains.kotlin.wasm.ir.*

class WasmCompiledModuleFragment(val irBuiltIns: IrBuiltIns) {
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
    val classIds =
        ReferencableElements<IrClassSymbol, Int>()
    val interfaceId =
        ReferencableElements<IrClassSymbol, Int>()
    val stringLiteralId =
        ReferencableElements<String, Int>()

    private val tagFuncType = WasmFunctionType(
        listOf(
            WasmRefNullType(WasmHeapType.Type(gcTypes.reference(irBuiltIns.throwableClass)))
        ),
        emptyList()
    )
    val tag = WasmTag(tagFuncType)

    val typeInfo = ReferencableAndDefinable<IrClassSymbol, ConstantDataElement>()

    val exports = mutableListOf<WasmExport<*>>()

    class JsCodeSnippet(val importName: String, val jsCode: String)

    val jsFuns = mutableListOf<JsCodeSnippet>()

    class FunWithPriority(val function: WasmFunction, val priority: String)

    val initFunctions = mutableListOf<FunWithPriority>()

    val scratchMemAddr = WasmSymbol<Int>()
    private val scratchMemSizeInBytes = 65_536

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

    @OptIn(ExperimentalUnsignedTypes::class)
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

        val klassIds = mutableMapOf<IrClassSymbol, Int>()
        var currentDataSectionAddress = 0
        for (typeInfoElement in typeInfo.elements) {
            val ir = typeInfo.wasmToIr.getValue(typeInfoElement)
            klassIds[ir] = currentDataSectionAddress
            currentDataSectionAddress += typeInfoElement.sizeInBytes
        }

        val stringDataSectionStart = currentDataSectionAddress
        val stringDataSectionBytes = mutableListOf<Byte>()
        val stringAddrs = mutableMapOf<String, Int>()
        for (str in stringLiteralId.unbound.keys) {
            val constData = ConstantDataCharArray("string_literal", str.toCharArray())
            stringDataSectionBytes += constData.toBytes().toList()
            stringAddrs[str] = currentDataSectionAddress
            currentDataSectionAddress += constData.sizeInBytes
        }

        // Reserve some memory to pass complex exported types (like strings). It's going to be accessible through 'unsafeGetScratchRawMemory'
        // runtime call from stdlib.
        currentDataSectionAddress = alignUp(currentDataSectionAddress, INT_SIZE_BYTES)
        scratchMemAddr.bind(currentDataSectionAddress)
        currentDataSectionAddress += scratchMemSizeInBytes

        bind(classIds.unbound, klassIds)
        bind(stringLiteralId.unbound, stringAddrs)
        interfaceId.unbound.onEachIndexed { index, entry -> entry.value.bind(index) }

        val data = typeInfo.buildData(address = { klassIds.getValue(it) }) +
                WasmData(WasmDataMode.Active(0, stringDataSectionStart), stringDataSectionBytes.toByteArray())

        val masterInitFunctionType = WasmFunctionType(emptyList(), emptyList())
        val masterInitFunction = WasmFunction.Defined("__init", WasmSymbol(masterInitFunctionType))
        with(WasmIrExpressionBuilder(masterInitFunction.instructions)) {
            initFunctions.sortedBy { it.priority }.forEach {
                buildCall(WasmSymbol(it.function))
            }
        }
        exports += WasmExport.Function("__init", masterInitFunction)

        val typeInfoSize = currentDataSectionAddress
        val memorySizeInPages = (typeInfoSize / 65_536) + 1
        val memory = WasmMemory(WasmLimits(memorySizeInPages.toUInt(), memorySizeInPages.toUInt()))

        // Need to export the memory in order to pass complex objects to the host language.
        exports += WasmExport.Memory("memory", memory)

        val importedFunctions = functions.elements.filterIsInstance<WasmFunction.Imported>()

        fun wasmTypeDeclarationOrderKey(declaration: WasmTypeDeclaration): Int {
            return when (declaration) {
                is WasmArrayDeclaration -> 0
                is WasmFunctionType -> 0
                is WasmStructDeclaration ->
                    // Subtype depth
                    declaration.superType?.let { wasmTypeDeclarationOrderKey(it.owner) + 1 } ?: 0
            }
        }

        val typeDeclarations = mutableListOf<WasmTypeDeclaration>()
        typeDeclarations.addAll(vTableGcTypes.elements)
        typeDeclarations.addAll(gcTypes.elements)
        typeDeclarations.addAll(classITableGcType.elements.distinct())
        typeDeclarations.sortBy(::wasmTypeDeclarationOrderKey)

        val globals = mutableListOf<WasmGlobal>()
        globals.addAll(globalFields.elements)
        globals.addAll(globalVTables.elements)
        globals.addAll(globalClassITables.elements.distinct())

        val module = WasmModule(
            functionTypes = canonicalFunctionTypes.values.toList() + tagFuncType + masterInitFunctionType,
            gcTypes = typeDeclarations,
            gcTypesInRecursiveGroup = true,
            importsInOrder = importedFunctions,
            importedFunctions = importedFunctions,
            definedFunctions = functions.elements.filterIsInstance<WasmFunction.Defined>() + masterInitFunction,
            tables = emptyList(),
            memories = listOf(memory),
            globals = globals,
            exports = exports,
            startFunction = null,  // Module is initialized via export call
            elements = emptyList(),
            data = data,
            tags = listOf(tag)
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

inline fun WasmCompiledModuleFragment.ReferencableAndDefinable<IrClassSymbol, ConstantDataElement>.buildData(address: (IrClassSymbol) -> Int): List<WasmData> {
    return elements.map {
        val id = address(wasmToIr.getValue(it))
        val offset = mutableListOf<WasmInstr>()
        WasmIrExpressionBuilder(offset).buildConstI32(id)
        WasmData(WasmDataMode.Active(0, offset), it.toBytes())
    }
}

fun alignUp(x: Int, alignment: Int): Int {
    assert(alignment and (alignment - 1) == 0) { "power of 2 expected" }
    return (x + alignment - 1) and (alignment - 1).inv()
}