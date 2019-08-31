/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.wasm.ast.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable

class WasmCompiledModuleFragment {
    val functions = ReferencableElements<IrFunctionSymbol, WasmFunction>()
    val globals = ReferencableElements<IrFieldSymbol, WasmGlobal>()
    val functionTypes = ReferencableElements<IrFunctionSymbol, WasmFunctionType>()
    val structTypes = ReferencableElements<IrClassSymbol, WasmStructType>()

    val classIds = ReferencableElements<IrClassSymbol, Int>()
    val interfaceId = ReferencableElements<IrClassSymbol, Int>()
    val virtualFunctionId = ReferencableElements<IrFunctionSymbol, Int>()

    val classes = mutableListOf<IrClassSymbol>()
    val interfaces = mutableListOf<IrClassSymbol>()
    val virtualFunctions = mutableListOf<IrFunctionSymbol>()

    val typeInfo = ReferencableElements<IrClassSymbol, BinaryDataElement>()
    val exports = mutableListOf<WasmExport>()

    val stringLiterals = mutableListOf<String>()
    var startFunction: WasmStart? = null

    class ReferencableElements<Ir : IrSymbol, Wasm : Any> {
        val elements = mutableListOf<Wasm>()
        val defined = mutableMapOf<Ir, Wasm>()
        val wasmToIr = mutableMapOf<Wasm, Ir>()

        val unbound = mutableMapOf<Ir, WasmSymbol<Wasm>>()

        fun define(ir: Ir, wasm: Wasm) {
            if (ir in defined)
                error("Trying to redefine element: IR: $ir Wasm: $wasm")

            elements += wasm
            defined[ir] = wasm
            wasmToIr[wasm] = ir
        }

        fun reference(ir: Ir): WasmSymbol<Wasm> =
            unbound.getOrPut(ir) { WasmSymbol.unbound() }

    }

    fun linkWasmCompiledFragments(): WasmModule {
        bind(functions.unbound, functions.defined)
        bind(globals.unbound, globals.defined)
        bind(functionTypes.unbound, functionTypes.defined)
        bind(structTypes.unbound, structTypes.defined)

        val klassIds = mutableMapOf<IrClassSymbol, Int>()
        var classId = 0
        for (typeInfoElement in typeInfo.elements) {
            val ir = typeInfo.wasmToIr.getValue(typeInfoElement)
            klassIds[ir] = classId
            classId += typeInfoElement.sizeInBytes
        }

        bind(classIds.unbound, klassIds)
        bindIndices(virtualFunctionId.unbound, virtualFunctions)
        bindIndices(interfaceId.unbound, interfaces)

        val data = typeInfo.elements.map {
            val ir = typeInfo.wasmToIr.getValue(it)
            val id = klassIds.getValue(ir)
            WasmData(id, it.toBytes())
        }

        println("Interfaces: ")
        interfaces.forEachIndexed { index, irClassSymbol ->
            println("  -- $index ${irClassSymbol.owner.fqNameWhenAvailable}")
        }

        println(BinaryDataStruct("typeInfo", typeInfo.elements).dump("", 0))

        val table = WasmTable(virtualFunctions.map { functions.defined.getValue(it) })
        val typeInfoSize = classId
        val memorySizeInPages = (typeInfoSize / 65_536) + 1
        val memory = WasmMemory(memorySizeInPages, memorySizeInPages)

        val module = WasmModule(
            functionTypes = functionTypes.elements,
            structTypes = structTypes.elements,
            importedFunctions = functions.elements.filterIsInstance<WasmImportedFunction>(),
            definedFunctions = functions.elements.filterIsInstance<WasmDefinedFunction>(),
            table = table,
            memory = memory,
            globals = globals.elements,
            exports = exports,
            start = startFunction!!,
            data = data
        )
        return module
    }
}


fun <IrSymbolType, WasmDeclarationType, WasmSymbolType : WasmSymbol<WasmDeclarationType>> bind(
    unbound: Map<IrSymbolType, WasmSymbolType>,
    defined: Map<IrSymbolType, WasmDeclarationType>
) {
    unbound.forEach { (irSymbol, wasmSymbol) ->
        if (irSymbol !in defined)
            error("Can't link symbol $irSymbol")
        wasmSymbol.bind(defined.getValue(irSymbol))
    }
}

fun <IrSymbolType> bindIndices(
    unbound: Map<IrSymbolType, WasmSymbol<Int>>,
    ordered: List<IrSymbolType>
) {
    unbound.forEach { (irSymbol, wasmSymbol) ->
        val index = ordered.indexOf(irSymbol)
        if (index == -1)
            error("Can't link symbol with indices $irSymbol")
        wasmSymbol.bind(index)
    }
}