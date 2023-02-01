/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation

class WasmCompiledModuleFragment(val irBuiltIns: IrBuiltIns) {
    val functions =
        ReferencableAndDefinable<IrFunctionSymbol, WasmFunction>()
    val hotswapFunctions =
        ReferencableAndDefinable<IrFunctionSymbol, WasmFunction>()
    val hotswapFunctionIndexes =
        ReferencableElements<IrFunctionSymbol, Int>()
    val globalFields =
        ReferencableAndDefinable<IrFieldSymbol, WasmGlobal>()
    val hotswapFieldGetter =
        ReferencableAndDefinable<IrFieldSymbol, WasmFunction>()
    val hotswapFieldSetter =
        ReferencableAndDefinable<IrFieldSymbol, WasmFunction>()
    val hotswapFieldGetterIndexes =
        ReferencableElements<IrFieldSymbol, Int>()
    val hotswapFieldSetterIndexes =
        ReferencableElements<IrFieldSymbol, Int>()
    val globalVTables =
        ReferencableAndDefinable<IrClassSymbol, WasmGlobal>()
    val globalClassITables =
        ReferencableAndDefinable<IrClassSymbol, WasmGlobal>()
    val functionTypes =
        ReferencableAndDefinable<IrFunctionSymbol, WasmFunctionType>()
    val hotSwapFieldGetterBridgesFunctionTypes =
        ReferencableAndDefinable<IrFieldSymbol, WasmFunctionType>()
    val hotSwapFieldSetterBridgesFunctionTypes =
        ReferencableAndDefinable<IrFieldSymbol, WasmFunctionType>()
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
    val stringLiteralAddress =
        ReferencableElements<String, Int>()
    val stringLiteralPoolId =
        ReferencableElements<String, Int>()
    val constantArrayDataSegmentId =
        ReferencableElements<Pair<List<Long>, WasmType>, Int>()

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
        bind(hotswapFunctions.unbound, hotswapFunctions.defined)
        bind(hotswapFieldGetter.unbound, hotswapFieldGetter.defined)
        bind(hotswapFieldSetter.unbound, hotswapFieldSetter.defined)
        bind(globalFields.unbound, globalFields.defined)
        bind(globalVTables.unbound, globalVTables.defined)
        bind(gcTypes.unbound, gcTypes.defined)
        bind(vTableGcTypes.unbound, vTableGcTypes.defined)
        bind(classITableGcType.unbound, classITableGcType.defined)
        bind(classITableInterfaceSlot.unbound, classITableInterfaceSlot.defined)
        bind(globalClassITables.unbound, globalClassITables.defined)

        val allFunctionTypeElements =
            functionTypes.elements + hotSwapFieldGetterBridgesFunctionTypes.elements + hotSwapFieldSetterBridgesFunctionTypes.elements

        // Associate function types to a single canonical function type
        val canonicalFunctionTypes =
            allFunctionTypeElements.associateWithTo(LinkedHashMap()) { it }

        functionTypes.unbound.forEach { (irSymbol, wasmSymbol) ->
            if (irSymbol !in functionTypes.defined)
                error("Can't link symbol ${irSymbolDebugDump(irSymbol)}")
            wasmSymbol.bind(canonicalFunctionTypes.getValue(functionTypes.defined.getValue(irSymbol)))
        }

        hotSwapFieldGetterBridgesFunctionTypes.unbound.forEach { (irSymbol, wasmSymbol) ->
            if (irSymbol !in hotSwapFieldGetterBridgesFunctionTypes.defined)
                error("Can't link symbol ${irSymbolDebugDump(irSymbol)}")
            wasmSymbol.bind(canonicalFunctionTypes.getValue(hotSwapFieldGetterBridgesFunctionTypes.defined.getValue(irSymbol)))
        }

        hotSwapFieldSetterBridgesFunctionTypes.unbound.forEach { (irSymbol, wasmSymbol) ->
            if (irSymbol !in hotSwapFieldSetterBridgesFunctionTypes.defined)
                error("Can't link symbol ${irSymbolDebugDump(irSymbol)}")
            wasmSymbol.bind(canonicalFunctionTypes.getValue(hotSwapFieldSetterBridgesFunctionTypes.defined.getValue(irSymbol)))
        }

        val oldSwapTableMap: MutableMap<String, Int> = mutableMapOf()
        val aaa = listOf(
            "<get-a> [(non-virtual) <get-a>() -> kotlin.Int][DELEGATED_PROPERTY_ACCESSOR]",
            "<set-count> [(non-virtual) <set-count>(kotlin.Int) -> kotlin.Unit][DEFAULT_PROPERTY_ACCESSOR]",
            "<get-count> [(non-virtual) <get-count>() -> kotlin.Int][DEFAULT_PROPERTY_ACCESSOR]",
            "<get-q> [(non-virtual) <get-q>() -> kotlin.Int][DEFAULT_PROPERTY_ACCESSOR]",
            "externLol__externalAdapter [(non-virtual) externLol__externalAdapter() -> kotlin.String][DEFINED]",
            "box [(non-virtual) box() -> kotlin.String][DEFINED]",
            "box__JsExportAdapter [(non-virtual) box__JsExportAdapter() -> kotlin.wasm.internal.ExternalInterfaceType?][DEFINED]",
            "appendElement [(non-virtual) (er: org.w3c.dom.HTMLElement) appendElement(kotlin.String) -> org.w3c.dom.Element][DEFINED]",
            "update [(non-virtual) update() -> kotlin.Unit][DEFINED]",
            "<get-a>\$ref.<init> [(non-virtual) <init>() -> <root>.<get-a>\$ref][GENERATED_MEMBER_IN_CALLABLE_REFERENCE]",
            "<get-a>\$ref.invoke [invoke() -> kotlin.Int][DEFINED]",
            "<get-a>\$ref.invoke [invoke() -> kotlin.Any?][BRIDGE]",
            "<get-a>\$ref.<get-name> [<get-name>() -> kotlin.String][DEFINED]",
            "a\$delegate\$lambda.<init> [(non-virtual) <init>() -> <root>.a\$delegate\$lambda][GENERATED_MEMBER_IN_CALLABLE_REFERENCE]",
            "a\$delegate\$lambda.invoke [invoke() -> kotlin.Int][DEFINED]",
            "a\$delegate\$lambda.invoke [invoke() -> kotlin.Any?][BRIDGE]",
            "box\$lambda\$lambda\$lambda.<init> [(non-virtual) <init>(org.w3c.dom.Element) -> <root>.box\$lambda\$lambda\$lambda][GENERATED_MEMBER_IN_CALLABLE_REFERENCE]",
            "box\$lambda\$lambda\$lambda.invoke [invoke(org.w3c.dom.events.MouseEvent) -> kotlin.Nothing?][DEFINED]",
            "box\$lambda\$lambda\$lambda.invoke [invoke(kotlin.Any?) -> kotlin.Any?][BRIDGE]",
            "box\$lambda\$lambda.<init> [(non-virtual) <init>() -> <root>.box\$lambda\$lambda][GENERATED_MEMBER_IN_CALLABLE_REFERENCE]",
            "box\$lambda\$lambda.invoke [invoke(org.w3c.dom.Element) -> kotlin.Unit][DEFINED]",
            "box\$lambda\$lambda.invoke [invoke(kotlin.Any?) -> kotlin.Any?][BRIDGE]",
            "box\$lambda\$lambda.<init> [(non-virtual) <init>(org.w3c.dom.Element) -> <root>.box\$lambda\$lambda][GENERATED_MEMBER_IN_CALLABLE_REFERENCE]",
            "box\$lambda\$lambda.invoke [invoke(org.w3c.dom.Element) -> kotlin.Unit][DEFINED]~1",
            "box\$lambda\$lambda.invoke [invoke(kotlin.Any?) -> kotlin.Any?][BRIDGE]~1",
            "box\$lambda.<init> [(non-virtual) <init>() -> <root>.box\$lambda][GENERATED_MEMBER_IN_CALLABLE_REFERENCE]",
            "box\$lambda.invoke [invoke(org.w3c.dom.Element) -> kotlin.Unit][DEFINED]",
            "box\$lambda.invoke [invoke(kotlin.Any?) -> kotlin.Any?][BRIDGE]",
            "appendElement\$lambda.<init> [(non-virtual) <init>() -> <root>.appendElement\$lambda][GENERATED_MEMBER_IN_CALLABLE_REFERENCE]",
            "appendElement\$lambda.invoke [invoke(org.w3c.dom.Element) -> kotlin.Unit][DEFINED]",
            "appendElement\$lambda.invoke [invoke(kotlin.Any?) -> kotlin.Any?][BRIDGE]",
            "<init properties surrogatePair.kt> [(non-virtual) <init properties surrogatePair.kt>() -> kotlin.Unit][SYNTHESIZED_DECLARATION]",
            "kotlin.wasm.internal.\$closureBox\$.<init> [(non-virtual) <init>(kotlin.Any?) -> kotlin.wasm.internal.\$closureBox\$][JS_CLOSURE_BOX_CLASS_DECLARATION]"
        )
//        val aaa = emptyList<String>()
        aaa.forEachIndexed { index, s -> oldSwapTableMap[s] = index }

        val importDescriptor = WasmImportDescriptor("hotswap_import", "hotswap_replacement_table")
            .takeIf { oldSwapTableMap.isNotEmpty() }

        val hotSwapTable = WasmTable(
            WasmLimits(0U, null),
            WasmFuncRef,
            importDescriptor
        )
        exports += WasmExport.Table("hotswap_table", hotSwapTable)

        val hotSwapGettersTable = WasmTable(
            WasmLimits(hotswapFieldGetter.defined.entries.size.toUInt(), hotswapFieldGetter.defined.entries.size.toUInt()),
            WasmFuncRef
        )

        val hotSwapSettersTable = WasmTable(
            WasmLimits(hotswapFieldSetter.defined.entries.size.toUInt(), hotswapFieldSetter.defined.entries.size.toUInt()),
            WasmFuncRef
        )

        val newSwapTableMap = mutableMapOf<String, Int>()
        val newSwapTableReplaceMap = mutableMapOf<Int, WasmFunction>()

        val functionIds = mutableSetOf<String>()
        fun IrFunctionSymbol.toId(): String {
            val signature = owner.wasmSignature(irBuiltIns).toString()
            val fqName = owner.kotlinFqName.asString()
            val origin = owner.origin.toString()
            val functionId = "$fqName $signature[$origin]"
            var functionIdWithIndex = functionId
            var functionIdIndex = 0
            while (!functionIds.add(functionIdWithIndex)) {
                functionIdIndex++
                functionIdWithIndex = "$functionId~$functionIdIndex"
            }
            return functionIdWithIndex
        }

        var newFunctionsCount = oldSwapTableMap.size
        for (entry in hotswapFunctions.defined.entries) {
            val functionId = entry.key.toId()
            val index = oldSwapTableMap[functionId] ?: newFunctionsCount++
            hotswapFunctionIndexes.reference(entry.key).bind(index)
            newSwapTableMap[functionId] = index
            newSwapTableReplaceMap[index] = entry.value
        }

        val rewriteFunctionTableType = WasmFunctionType(emptyList(), emptyList())
        val rewriteFunctionTable = WasmFunction.Defined("__rewriteFunctionTable", WasmSymbol(rewriteFunctionTableType))
        val hotSwapLocation = SourceLocation.NoLocation("Generated service code")
        with(WasmIrExpressionBuilder(rewriteFunctionTable.instructions)) {
            buildRefNull(WasmFuncRef.getHeapType(), hotSwapLocation)
            buildConstI32(newFunctionsCount - oldSwapTableMap.size, hotSwapLocation)
            buildInstr(WasmOp.TABLE_GROW, hotSwapLocation, WasmImmediate.TableIdx(0))
            buildDrop(hotSwapLocation)
            for (entry in newSwapTableReplaceMap) {
                buildConstI32(entry.key, hotSwapLocation)
                buildInstr(WasmOp.REF_FUNC, hotSwapLocation, WasmImmediate.FuncIdx(entry.value))
                buildInstr(WasmOp.TABLE_SET, hotSwapLocation, WasmImmediate.TableIdx(0))
            }
        }
        exports += WasmExport.Function("__rewriteFunctionTable", rewriteFunctionTable)

        hotswapFieldGetter.defined.entries.forEachIndexed { index, entry -> hotswapFieldGetterIndexes.reference(entry.key).bind(index) }
        hotswapFieldSetter.defined.entries.forEachIndexed { index, entry -> hotswapFieldSetterIndexes.reference(entry.key).bind(index) }

        val hotSwapElement = WasmElement(
            WasmFuncRef,
            hotswapFunctions.defined.entries.map { WasmTable.Value.Function(it.value) },
            WasmElement.Mode.Declarative
        )

        val hotSwapGettersElement = WasmElement(
            WasmFuncRef,
            hotswapFieldGetter.defined.entries.map { WasmTable.Value.Function(it.value) },
            WasmElement.Mode.Active(hotSwapGettersTable, listOf(WasmInstrWithoutLocation(WasmOp.I32_CONST, listOf(WasmImmediate.ConstI32(0)))))
        )

        val hotSwapSettersElement = WasmElement(
            WasmFuncRef,
            hotswapFieldSetter.defined.entries.map { WasmTable.Value.Function(it.value) },
            WasmElement.Mode.Active(hotSwapSettersTable, listOf(WasmInstrWithoutLocation(WasmOp.I32_CONST, listOf(WasmImmediate.ConstI32(0)))))
        )

        val klassIds = mutableMapOf<IrClassSymbol, Int>()
        var currentDataSectionAddress = 0
        for (typeInfoElement in typeInfo.elements) {
            val ir = typeInfo.wasmToIr.getValue(typeInfoElement)
            klassIds[ir] = currentDataSectionAddress
            currentDataSectionAddress += typeInfoElement.sizeInBytes
        }

        currentDataSectionAddress = alignUp(currentDataSectionAddress, INT_SIZE_BYTES)
        scratchMemAddr.bind(currentDataSectionAddress)

        bind(classIds.unbound, klassIds)
        interfaceId.unbound.onEachIndexed { index, entry -> entry.value.bind(index) }

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
        typeInfo.buildData(data, address = { klassIds.getValue(it) })

        val masterInitFunctionType = WasmFunctionType(emptyList(), emptyList())
        val masterInitFunction = WasmFunction.Defined("__init", WasmSymbol(masterInitFunctionType))
        with(WasmIrExpressionBuilder(masterInitFunction.instructions)) {
            buildCall(WasmSymbol(rewriteFunctionTable), SourceLocation.NoLocation("Generated service code"))
            initFunctions.sortedBy { it.priority }.forEach {
                buildCall(WasmSymbol(it.function), SourceLocation.NoLocation("Generated service code"))
            }
        }
        exports += WasmExport.Function("__init", masterInitFunction)

        val typeInfoSize = currentDataSectionAddress
        val memorySizeInPages = (typeInfoSize / 65_536) + 1
        val memory = WasmMemory(WasmLimits(memorySizeInPages.toUInt(), null /* "unlimited" */))

        // Need to export the memory in order to pass complex objects to the host language.
        // Export name "memory" is a WASI ABI convention.
        exports += WasmExport.Memory("memory", memory)

        exports += WasmExport.Table("hotswap_getters_table", hotSwapGettersTable)
        exports += WasmExport.Table("hotswap_setters_table", hotSwapSettersTable)

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

        val recGroupTypes = mutableListOf<WasmTypeDeclaration>()
        recGroupTypes.addAll(vTableGcTypes.elements)
        recGroupTypes.addAll(this.gcTypes.elements)
        recGroupTypes.addAll(classITableGcType.elements.distinct())
        recGroupTypes.sortBy(::wasmTypeDeclarationOrderKey)

        val globals = mutableListOf<WasmGlobal>()
        globals.addAll(globalFields.elements)
        globals.addAll(globalVTables.elements)
        globals.addAll(globalClassITables.elements.distinct())

        val allFunctionTypes = canonicalFunctionTypes.values.toList() + tagFuncType + masterInitFunctionType + rewriteFunctionTableType

        // Partition out function types that can't be recursive,
        // we don't need to put them into a rec group
        // so that they can be matched with function types from other Wasm modules.
        val (potentiallyRecursiveFunctionTypes, nonRecursiveFunctionTypes) =
            allFunctionTypes.partition { it.referencesTypeDeclarations() }
        recGroupTypes.addAll(potentiallyRecursiveFunctionTypes)


        val hotSwapReplacementTableToImport = hotSwapTable.takeIf { oldSwapTableMap.isNotEmpty() }
        if (hotSwapReplacementTableToImport != null) {
            jsModuleImports.add("hotswap_import")
        }

        val module = WasmModule(
            functionTypes = nonRecursiveFunctionTypes,
            recGroupTypes = recGroupTypes,
            importsInOrder = importedFunctions + listOfNotNull(hotSwapReplacementTableToImport),
            importedFunctions = importedFunctions,
            importedTables = listOfNotNull(hotSwapReplacementTableToImport),
            definedFunctions = (functions.elements + hotswapFunctions.elements + hotswapFieldGetter.elements + hotswapFieldSetter.elements).filterIsInstance<WasmFunction.Defined>() + masterInitFunction + rewriteFunctionTable,
            tables = listOfNotNull(hotSwapTable.takeIf { oldSwapTableMap.isEmpty() }, hotSwapGettersTable, hotSwapSettersTable),
            memories = listOf(memory),
            globals = globals,
            exports = exports,
            startFunction = null,  // Module is initialized via export call
            elements = listOf(hotSwapElement, hotSwapGettersElement, hotSwapSettersElement),
            data = data,
            dataCount = true,
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

inline fun WasmCompiledModuleFragment.ReferencableAndDefinable<IrClassSymbol, ConstantDataElement>.buildData(
    into: MutableList<WasmData>,
    address: (IrClassSymbol) -> Int
) {
    elements.mapTo(into) {
        val id = address(wasmToIr.getValue(it))
        val offset = mutableListOf<WasmInstr>()
        WasmIrExpressionBuilder(offset).buildConstI32(id, SourceLocation.NoLocation("Compile time data per class"))
        WasmData(WasmDataMode.Active(0, offset), it.toBytes())
    }
}

fun alignUp(x: Int, alignment: Int): Int {
    assert(alignment and (alignment - 1) == 0) { "power of 2 expected" }
    return (x + alignment - 1) and (alignment - 1).inv()
}