/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.wasm.lower.WasmSignature
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.wasm.ir.*

class WasmCompiledModuleFragment {
    val functions =
        ReferencableAndDefinable<IrFunctionSymbol, WasmFunction>()
    val globals =
        ReferencableAndDefinable<IrFieldSymbol, WasmGlobal>()
    val functionTypes =
        ReferencableAndDefinable<IrFunctionSymbol, WasmFunctionType>()
    val gcTypes =
        ReferencableAndDefinable<IrClassSymbol, WasmTypeDeclaration>()
    val classIds =
        ReferencableElements<IrClassSymbol, Int>()
    val interfaceId =
        ReferencableElements<IrClassSymbol, Int>()
    val virtualFunctionId =
        ReferencableElements<IrFunctionSymbol, Int>()
    val signatureId =
        ReferencableElements<WasmSignature, Int>()
    val stringLiteralId =
        ReferencableElements<String, Int>()

    val runtimeTypes =
        ReferencableAndDefinable<IrClassSymbol, WasmGlobal>()

    val classes = mutableListOf<IrClassSymbol>()
    val interfaces = mutableListOf<IrClassSymbol>()
    val virtualFunctions = mutableListOf<IrSimpleFunctionSymbol>()
    val signatures = LinkedHashSet<WasmSignature>()
    val stringLiterals = mutableListOf<String>()

    val typeInfo =
        ReferencableAndDefinable<IrClassSymbol, ConstantDataElement>()

    // Wasm table for each method of each interface.
    val interfaceMethodTables =
        ReferencableAndDefinable<IrFunctionSymbol, WasmTable>()

    // Defined class interface tables
    val definedClassITableData =
        ReferencableAndDefinable<IrClassSymbol, ConstantDataElement>()

    // Address of class interface table in linear memory
    val referencedClassITableAddresses =
        ReferencableElements<IrClassSymbol, Int>()

    // Sequential number of an implementation (class, object, etc.) for a particular interface
    // Used as index in table for interface method dispatch
    val referencedInterfaceImplementationId =
        ReferencableElements<InterfaceImplementation, Int>()

    val interfaceImplementationsMethods =
        LinkedHashMap<InterfaceImplementation, Map<IrFunctionSymbol, WasmSymbol<WasmFunction>>>()

    val exports = mutableListOf<WasmExport<*>>()

    class JsCodeSnippet(val importName: String, val jsCode: String)

    val jsFuns = mutableListOf<JsCodeSnippet>()

    var startFunction: WasmFunction? = null

    open class ReferencableElements<Ir, Wasm : Any> {
        val unbound = mutableMapOf<Ir, WasmSymbol<Wasm>>()
        fun reference(ir: Ir): WasmSymbol<Wasm> {
            val declaration = (ir as? IrSymbol)?.owner as? IrDeclarationWithName
            if (declaration != null) {
                val packageFragment = declaration.getPackageFragment()
                    ?: error("Referencing declaration without package fragment ${declaration.fqNameWhenAvailable}")
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
        bind(globals.unbound, globals.defined)
        bind(functionTypes.unbound, functionTypes.defined)
        bind(gcTypes.unbound, gcTypes.defined)
        bind(runtimeTypes.unbound, runtimeTypes.defined)

        val klassIds = mutableMapOf<IrClassSymbol, Int>()
        var currentDataSectionAddress = 0
        for (typeInfoElement in typeInfo.elements) {
            val ir = typeInfo.wasmToIr.getValue(typeInfoElement)
            klassIds[ir] = currentDataSectionAddress
            currentDataSectionAddress += typeInfoElement.sizeInBytes
        }

        val interfaceTableAddresses = mutableMapOf<IrClassSymbol, Int>()
        for (typeInfoElement in definedClassITableData.elements) {
            val ir = definedClassITableData.wasmToIr.getValue(typeInfoElement)
            interfaceTableAddresses[ir] = currentDataSectionAddress
            currentDataSectionAddress += typeInfoElement.sizeInBytes
        }

        bind(classIds.unbound, klassIds)
        bind(referencedClassITableAddresses.unbound, interfaceTableAddresses)
        bindIndices(virtualFunctionId.unbound, virtualFunctions)
        bindIndices(signatureId.unbound, signatures.toList())
        bindIndices(interfaceId.unbound, interfaces)
        bindIndices(stringLiteralId.unbound, stringLiterals)

        val interfaceImplementationIds = mutableMapOf<InterfaceImplementation, Int>()
        val numberOfInterfaceImpls = mutableMapOf<IrClassSymbol, Int>()
        for (interfaceImplementation in interfaceImplementationsMethods.keys) {
            val prev = numberOfInterfaceImpls.getOrPut(interfaceImplementation.irInterface) { 0 }
            interfaceImplementationIds[interfaceImplementation] = prev
            numberOfInterfaceImpls[interfaceImplementation.irInterface] = prev + 1
        }

        bind(referencedInterfaceImplementationId.unbound, interfaceImplementationIds)
        bind(interfaceMethodTables.unbound, interfaceMethodTables.defined)

        val data =
            typeInfo.buildData(address = { klassIds.getValue(it) }) +
                    definedClassITableData.buildData(address = { interfaceTableAddresses.getValue(it) })

        val logTypeInfo = false
        if (logTypeInfo) {
            println("Signatures: ")
            for ((index, signature: WasmSignature) in signatures.withIndex()) {
                println("  -- $index $signature")
            }

            println("Interfaces: ")
            for ((index, iface: IrClassSymbol) in interfaces.withIndex()) {
                println("  -- $index ${iface.owner.fqNameWhenAvailable}")
            }

            println("Virtual functions: ")
            for ((index, vf: IrSimpleFunctionSymbol) in virtualFunctions.withIndex()) {
                println("  -- $index ${vf.owner.fqNameWhenAvailable}")
            }

            println(
                ConstantDataStruct("typeInfo", typeInfo.elements).dump("", 0)
            )
        }

        val table = WasmTable(
            limits = WasmLimits(virtualFunctions.size.toUInt(), virtualFunctions.size.toUInt()),
            elementType = WasmFuncRef,
        )

        val offsetExpr = mutableListOf<WasmInstr>()
        WasmIrExpressionBuilder(offsetExpr).buildConstI32(0)

        val elements = WasmElement(
            WasmFuncRef,
            values = virtualFunctions.map {
                WasmTable.Value.Function(functions.defined.getValue(it))
            },
            WasmElement.Mode.Active(table, offsetExpr)
        )

        val interfaceTableElementsLists = interfaceMethodTables.defined.keys.associateWith {
            mutableMapOf<Int, WasmSymbol<WasmFunction>>()
        }

        interfaceImplementationIds.forEach { ii: InterfaceImplementation, implId: Int ->
            for ((interfaceFunction: IrFunctionSymbol, wasmFunction: WasmSymbol<WasmFunction>) in interfaceImplementationsMethods[ii]!!) {
                interfaceTableElementsLists[interfaceFunction]!![implId] = wasmFunction
            }
        }

        val interfaceTableElements = interfaceTableElementsLists.map { (interfaceFunction, methods) ->
            val type = interfaceMethodTables.defined[interfaceFunction]!!.elementType
            val functions = MutableList(methods.size) { idx ->
                val wasmFunc = methods[idx]!!
                val expression = buildWasmExpression {
                    buildInstr(WasmOp.REF_FUNC, WasmImmediate.FuncIdx(wasmFunc))
                }
                WasmTable.Value.Expression(expression)
            }
            WasmElement(
                type,
                values = functions,
                WasmElement.Mode.Active(interfaceMethodTables.defined[interfaceFunction]!!, offsetExpr)
            )
        }

        interfaceMethodTables.defined.forEach { (function, table) ->
            val size = interfaceTableElementsLists[function]!!.size.toUInt()
            table.limits = WasmLimits(size, size)
        }

        val typeInfoSize = currentDataSectionAddress
        val memorySizeInPages = (typeInfoSize / 65_536) + 1
        val memory = WasmMemory(WasmLimits(memorySizeInPages.toUInt(), memorySizeInPages.toUInt()))

        val importedFunctions = functions.elements.filterIsInstance<WasmFunction.Imported>()

        // Sorting by depth for a valid init order
        val sortedRttGlobals = runtimeTypes.elements.sortedBy { (it.type as WasmRtt).depth }

        val module = WasmModule(
            functionTypes = functionTypes.elements,
            gcTypes = gcTypes.elements,
            importsInOrder = importedFunctions,
            importedFunctions = importedFunctions,
            definedFunctions = functions.elements.filterIsInstance<WasmFunction.Defined>(),
            tables = listOf(table) + interfaceMethodTables.elements,
            memories = listOf(memory),
            globals = globals.elements + sortedRttGlobals,
            exports = exports,
            startFunction = startFunction!!,
            elements = listOf(elements) + interfaceTableElements,
            data = data
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

fun <IrSymbolType> bindIndices(
    unbound: Map<IrSymbolType, WasmSymbol<Int>>,
    ordered: List<IrSymbolType>
) {
    unbound.forEach { (irSymbol, wasmSymbol) ->
        val index = ordered.indexOf(irSymbol)
        if (index == -1)
            error("Can't link symbol with indices ${irSymbolDebugDump(irSymbol)}")
        wasmSymbol.bind(index)
    }
}

inline fun WasmCompiledModuleFragment.ReferencableAndDefinable<IrClassSymbol, ConstantDataElement>.buildData(address: (IrClassSymbol) -> Int): List<WasmData> {
    return elements.map {
        val id = address(wasmToIr.getValue(it))
        val offset = mutableListOf<WasmInstr>()
        WasmIrExpressionBuilder(offset).buildConstI32(id)
        WasmData(WasmDataMode.Active(0, offset), it.toBytes())
    }
}


data class InterfaceImplementation(
    val irInterface: IrClassSymbol,
    val irClass: IrClassSymbol
)