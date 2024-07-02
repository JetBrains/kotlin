/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.serialization

import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment.ReferencableElements
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.newLinkedHashMapWithExpectedSize
import org.jetbrains.kotlin.utils.newLinkedHashSetWithExpectedSize
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.convertors.MyByteReader
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet

/**
 * This class is the exact opposite of [WasmSerializer]. See [WasmSerializer] for details.
 *
 * When deserializing an object serialized with [WasmSerializer.withFlags], use [withFlags]
 * When deserializing an object serialized with [WasmSerializer.withTag], use [withTag]
 * When deserializing an object serialized with [WasmSerializer.serializeAsReference],
 *  use [deserializeReference]
 */

class WasmDeserializer(inputStream: InputStream, private val skipLocalNames: Boolean = false, private val skipSourceLocations: Boolean) {

    private val input: MyByteReader = MyByteReader(inputStream)

    private var b: MyByteReader = input

    private val referenceTable = mutableListOf<Symbol>()

    companion object {
        private val OPCODE_TO_WASM_OP by lazy { enumValues<WasmOp>().associateBy { it.opcode } }
    }

    fun deserialize(): WasmCompiledFileFragment {
        // Step 1: load the size of the reference table
        val referenceTableSize = deserializeInt()

        // Step 2: load the elements of the reference table as bytes
        repeat(referenceTableSize) {
            val slotSize = deserializeInt()
            val bytes = b.readBytes(slotSize)
            referenceTable.add(Symbol(bytes))
        }

        // Step 3: read the rest of the input
        return deserializeCompiledFileFragment()
    }

    private fun deserializeFunction() =
        deserializeNamedModuleField { name ->
            val type = deserializeSymbol(::deserializeFunctionType)
            withTag { tag ->
                when (tag) {
                    0 -> {
                        val locals = deserializeList(::deserializeLocal)
                        val instructions = deserializeList(::deserializeInstr)
                        WasmFunction.Defined(
                            name,
                            type,
                            locals,
                            instructions
                        )
                    }
                    1 -> WasmFunction.Imported(
                        name,
                        type,
                        deserializeImportDescriptor()
                    )
                    else -> tagError()
                }
            }
        }

    private fun deserializeGlobal() =
        deserializeNamedModuleField { name, flags ->
            val type = deserializeType()
            val isMutable = flags.consume()
            val init = deserializeList(::deserializeInstr)
            val importPair = if (flags.consume()) null else deserializeImportDescriptor()
            WasmGlobal(name, type, isMutable, init, importPair)
        }

    private fun deserializeFunctionType() =
        deserializeNamedModuleField { _, _ ->
            val parameterTypes = deserializeList(::deserializeType)
            val resultTypes = deserializeList(::deserializeType)
            WasmFunctionType(parameterTypes, resultTypes)
        }

    private fun deserializeTypeDeclaration(): WasmTypeDeclaration =
        withTag { tag ->
            when (tag) {
                0 -> deserializeFunctionType()
                1 -> deserializeStructDeclaration()
                2 -> deserializeArrayDeclaration()
                else -> tagError()
            }
        }

    private fun deserializeStructDeclaration(): WasmStructDeclaration =
        deserializeNamedModuleField { name, flags ->
            val fields = deserializeList(::deserializeStructFieldDeclaration)
            val superType = if (flags.consume()) null else deserializeSymbol(::deserializeTypeDeclaration)
            val isFinal = flags.consume()
            WasmStructDeclaration(
                name,
                fields,
                superType,
                isFinal
            )
        }

    private fun deserializeArrayDeclaration(): WasmArrayDeclaration =
        deserializeNamedModuleField { name, flags ->
            val fieldName = deserializeString()
            val fieldType = deserializeType()
            val isMutable = flags.consume()
            WasmArrayDeclaration(
                name,
                WasmStructFieldDeclaration(
                    fieldName,
                    fieldType,
                    isMutable
                )
            )
        }

    private fun deserializeMemory(): WasmMemory =
        deserializeNamedModuleField { _, flags ->
            val limits = deserializeLimits()
            val importPair = if (flags.consume()) null else deserializeImportDescriptor()
            WasmMemory(limits, importPair)
        }

    private fun deserializeTag(): WasmTag =
        deserializeNamedModuleField { _, flags ->
            val type = deserializeFunctionType()
            val importPair = if (flags.consume()) null else deserializeImportDescriptor()
            WasmTag(type, importPair)
        }

    private fun deserializeStructFieldDeclaration(): WasmStructFieldDeclaration {
        val name = deserializeString()
        val type = deserializeType()
        val isMutable = b.readUByte().toBoolean()
        return WasmStructFieldDeclaration(name, type, isMutable)
    }

    private fun deserializeType() =
        withTag { tag ->
            when (tag) {
                0 -> WasmRefType(deserializeHeapType())
                1 -> WasmRefNullType(deserializeHeapType())
                else -> WASM_TYPE_OBJECTS.getOrElse(tag - 2) { tagError() }
            }
        }

    private fun deserializeHeapType() =
        withTag { tag ->
            when (tag) {
                0 -> WasmHeapType.Simple.Any
                1 -> WasmHeapType.Simple.Eq
                2 -> WasmHeapType.Simple.Extern
                3 -> WasmHeapType.Simple.Func
                4 -> WasmHeapType.Simple.NoExtern
                5 -> WasmHeapType.Simple.None
                6 -> WasmHeapType.Simple.Struct
                7 -> WasmHeapType.Type(deserializeSymbol(::deserializeTypeDeclaration))
                else -> tagError()
            }
        }

    private fun deserializeLocal(): WasmLocal {
        val id = deserializeInt()
        val name: String
        if (skipLocalNames) {
            skipString()
            name = ""
        } else {
            name = deserializeString()
        }
        val type = deserializeType()
        val isParameter = b.readUByte().toBoolean()
        return WasmLocal(id, name, type, isParameter)
    }

    private fun deserializeInstr(): WasmInstr {
        val opcode = b.readUInt16().toInt()
        return withTag { tag ->
            val op = when (opcode) {
                0xFFFF - 0 -> WasmOp.PSEUDO_COMMENT_PREVIOUS_INSTR
                0xFFFF - 1 -> WasmOp.PSEUDO_COMMENT_GROUP_START
                0xFFFF - 2 -> WasmOp.PSEUDO_COMMENT_GROUP_END
                0xFFFF - 3 -> WasmOp.MACRO_IF
                0xFFFF - 4 -> WasmOp.MACRO_ELSE
                0xFFFF - 5 -> WasmOp.MACRO_END_IF
                0xFFFF - 6 -> WasmOp.MACRO_TABLE
                0xFFFF - 7 -> WasmOp.MACRO_TABLE_INDEX
                0xFFFF - 8 -> WasmOp.MACRO_TABLE_END
                else -> OPCODE_TO_WASM_OP.getOrElse(opcode) { error("Unknown opcode $opcode") }
            }
            val immediates = deserializeList(::deserializeImmediate)
            when (tag) {
                0 -> WasmInstrWithLocation(op, immediates, deserializeSourceLocation())
                1 -> WasmInstrWithoutLocation(op, immediates)
                else -> tagError()
            }
        }
    }

    private fun deserializeImmediate(): WasmImmediate =
        withTag { tag ->
            when (tag) {
                0  -> WasmImmediate.BlockType.Function(deserializeFunctionType())
                1  -> WasmImmediate.BlockType.Value(deserializeType())
                2  -> deserializeImmediateCatch()
                3  -> WasmImmediate.ConstF32(b.readUInt32())
                4  -> WasmImmediate.ConstF64(b.readUInt64())
                5  -> WasmImmediate.ConstI32(b.readUInt32().toInt())
                6  -> WasmImmediate.ConstI64(b.readUInt64().toLong())
                7  -> WasmImmediate.ConstString(deserializeString())
                8  -> WasmImmediate.ConstU8(b.readUByte())
                9  -> WasmImmediate.DataIdx(deserializeSymbol(::deserializeInt))
                10 -> WasmImmediate.ElemIdx(deserializeElement())
                11 -> WasmImmediate.FuncIdx(deserializeSymbol(::deserializeFunction))
                12 -> WasmImmediate.GcType(deserializeSymbol(::deserializeTypeDeclaration))
                13 -> WasmImmediate.GlobalIdx(deserializeSymbol(::deserializeGlobal))
                14 -> WasmImmediate.HeapType(deserializeHeapType())
                15 -> WasmImmediate.LabelIdx(deserializeInt())
                16 -> WasmImmediate.LabelIdxVector(deserializeList(::deserializeInt))
                17 -> WasmImmediate.LocalIdx(deserializeSymbol(::deserializeLocal))
                18 -> { val align = b.readUInt32(); val offset = b.readUInt32(); WasmImmediate.MemArg(align, offset) }
                19 -> WasmImmediate.MemoryIdx(deserializeInt())
                20 -> WasmImmediate.StructFieldIdx(deserializeSymbol(::deserializeInt))
                21 -> WasmImmediate.SymbolI32(deserializeSymbol(::deserializeInt))
                22 -> WasmImmediate.TableIdx(deserializeSymbol(::deserializeInt))
                23 -> WasmImmediate.TagIdx(deserializeSymbol(::deserializeInt))
                24 -> WasmImmediate.TypeIdx(deserializeSymbol(::deserializeTypeDeclaration))
                25 -> WasmImmediate.ValTypeVector(deserializeList(::deserializeType))
                // This is the special case of BlockType.Value, which accepts a nullable WasmType. If is null, MSB is set to 1.
                129 -> WasmImmediate.BlockType.Value(null)
                else -> tagError()
            }
        }

    private fun deserializeImmediateCatch(): WasmImmediate.Catch {
        val type = when (b.readByte().toInt()) {
            0 -> WasmImmediate.Catch.CatchType.CATCH
            1 -> WasmImmediate.Catch.CatchType.CATCH_REF
            2 -> WasmImmediate.Catch.CatchType.CATCH_ALL
            3 -> WasmImmediate.Catch.CatchType.CATCH_ALL_REF
            else -> tagError()
        }
        val immediates = deserializeList(::deserializeImmediate)
        return WasmImmediate.Catch(type, immediates)
    }

    private fun deserializeTable() =
        deserializeNamedModuleField { _, flags ->
            val min = b.readUInt32()
            val max = if (flags.consume()) null else b.readUInt32()
            val elementType = deserializeType()
            val ip = deserializeImportDescriptor()
            WasmTable(WasmLimits(min, max), elementType, ip)
        }

    private fun deserializeTableValue(): WasmTable.Value =
        withTag { tag ->
            when (tag) {
                0 -> WasmTable.Value.Expression(deserializeList(::deserializeInstr))
                1 -> WasmTable.Value.Function(deserializeSymbol(::deserializeFunction))
                else -> tagError()
            }
        }

    private fun deserializeElement() =
        deserializeNamedModuleField { _ ->
            val type = deserializeType()
            val values = deserializeList(::deserializeTableValue)
            val mode = deserializeElementMode()
            WasmElement(type, values, mode)
        }

    private fun deserializeElementMode(): WasmElement.Mode =
        withTag { tag ->
            when (tag) {
                0 -> {
                    val table = deserializeTable()
                    val offset = deserializeList(::deserializeInstr)
                    WasmElement.Mode.Active(table, offset)
                }
                1 -> WasmElement.Mode.Declarative
                2 -> WasmElement.Mode.Passive
                else -> tagError()
            }
        }

    private fun deserializeExport(): WasmExport<*> {
        // The name is deserialized before the tag.
        val name = deserializeString()
        return withTag { tag ->
            when (tag) {
                0 -> WasmExport.Function(name, deserializeFunction())
                1 -> WasmExport.Table(name, deserializeTable())
                2 -> WasmExport.Memory(name, deserializeMemory())
                3 -> WasmExport.Global(name, deserializeGlobal())
                4 -> WasmExport.Tag(name, deserializeTag())
                else -> tagError()
            }
        }
    }

    private fun deserializeLimits(): WasmLimits =
        withFlags { flags ->
            val minSize = b.readUInt32()
            val maxSize = if (flags.consume()) null else b.readUInt32()
            WasmLimits(minSize, maxSize)
        }

    private fun deserializeImportDescriptor(): WasmImportDescriptor {
        val moduleName = deserializeString()
        val declarationName = deserializeSymbol(::deserializeString)
        return WasmImportDescriptor(moduleName, declarationName)
    }

    private inline fun <A, B> deserializePair(deserializeAFunc: () -> A, deserializeBFunc: () -> B): Pair<A, B> {
        val a = deserializeAFunc()
        val b = deserializeBFunc()
        return Pair(a, b)
    }

    private inline fun <T> deserializeList(itemDeserializeFunc: () -> T): MutableList<T> {
        val size = deserializeInt()
        val list = ArrayList<T>(size)
        repeat(size) {
            list.add(itemDeserializeFunc())
        }
        return list
    }

    private inline fun <T> deserializeSet(itemDeserializeFunc: () -> T): LinkedHashSet<T> {
        val size = deserializeInt()
        val set = newLinkedHashSetWithExpectedSize<T>(size)
        repeat(size) {
            set.add(itemDeserializeFunc())
        }
        return set
    }

    private inline fun <K, V> deserializeMap(deserializeKeyFunc: () -> K, deserializeValueFunc: () -> V): LinkedHashMap<K, V> {
        val size = deserializeInt()
        val map = newLinkedHashMapWithExpectedSize<K, V>(size)
        repeat(size) {
            val key = deserializeKeyFunc()
            val value = deserializeValueFunc()
            map[key] = value
        }
        return map
    }

    private fun deserializeSourceLocation() =
        withTag { tag ->
            when (tag) {
                0 -> SourceLocation.NoLocation
                else -> {
                    if (skipSourceLocations) {
                        skipString()
                        skipString()
                        skipInt()
                        skipInt()
                        SourceLocation.NoLocation
                    } else {
                        val module = deserializeString()
                        val file = deserializeString()
                        val line = deserializeInt()
                        val column = deserializeInt()
                        when (tag) {
                            1 -> SourceLocation.Location(module, file, line, column)
                            2 -> SourceLocation.IgnoredLocation(module, file, line, column)
                            else -> tagError()
                        }
                    }
                }
            }
        }

    private inline fun <T> deserializeNullable(crossinline deserializeFunc: () -> T): T? = withTag {
        when (it) {
            0 -> null
            1 -> deserializeFunc()
            else -> tagError()
        }
    }

    private fun deserializeIdSignature(): IdSignature =
        withTag { tag ->
            when (tag) {
                0 -> deserializeAccessorSignature()
                1 -> deserializeCommonSignature()
                2 -> deserializeCompositeSignature()
                3 -> deserializeFileLocalSignature()
                4 -> deserializeLocalSignature()
                5 -> deserializeLoweredDeclarationSignature()
                6 -> deserializeScopeLocalDeclaration()
                7 -> deserializeSpecialFakeOverrideSignature()
                8 -> IdSignature.FileSignature(deserializeString(), FqName(""), "")
                else -> tagError()
            }
        }

    private fun deserializeAccessorSignature(): IdSignature.AccessorSignature {
        val propertySignature = deserializeIdSignature()
        val accessorSignature = deserializeCommonSignature()
        return IdSignature.AccessorSignature(propertySignature, accessorSignature)
    }

    private fun deserializeCommonSignature(): IdSignature.CommonSignature =
        withFlags { flags ->
            val packageFqName = deserializeString()
            val declarationFqName = deserializeString()
            val id = if (flags.consume()) null else b.readUInt64().toLong()
            val mask = b.readUInt64().toLong()
            val description = if (flags.consume()) null else deserializeString()
            IdSignature.CommonSignature(packageFqName, declarationFqName, id, mask, description)
        }

    private fun deserializeCompositeSignature(): IdSignature.CompositeSignature {
        val container = deserializeIdSignature()
        val inner = deserializeIdSignature()
        return IdSignature.CompositeSignature(container, inner)
    }

    private fun deserializeFileLocalSignature(): IdSignature.FileLocalSignature =
        withFlags { flags ->
            val container = deserializeIdSignature()
            val id = b.readUInt64().toLong()
            val description = if (flags.consume()) null else deserializeString()
            IdSignature.FileLocalSignature(container, id, description)
        }

    private fun deserializeLocalSignature(): IdSignature.LocalSignature =
        withFlags { flags ->
            val localFqn = deserializeString()
            val hashSig = if (flags.consume()) null else b.readUInt64().toLong()
            val description = if (flags.consume()) null else deserializeString()
            IdSignature.LocalSignature(localFqn, hashSig, description)
        }

    private fun deserializeLoweredDeclarationSignature(): IdSignature.LoweredDeclarationSignature {
        val original = deserializeIdSignature()
        val stage = b.readUInt32().toInt()
        val index = b.readUInt32().toInt()
        return IdSignature.LoweredDeclarationSignature(original, stage, index)
    }

    private fun deserializeScopeLocalDeclaration(): IdSignature.ScopeLocalDeclaration =
        withFlags { flags ->
            val id = b.readUInt32().toInt()
            val description = if (flags.consume()) null else deserializeString()
            IdSignature.ScopeLocalDeclaration(id, description)
        }

    private fun deserializeSpecialFakeOverrideSignature(): IdSignature.SpecialFakeOverrideSignature {
        val memberSignature = deserializeIdSignature()
        val overriddenSignatures = deserializeList(::deserializeIdSignature)
        return IdSignature.SpecialFakeOverrideSignature(memberSignature, overriddenSignatures)
    }

    private fun deserializeConstantDataElement(): ConstantDataElement =
        withTag { tag ->
            when (tag) {
                0 -> deserializeConstantDataCharArray()
                1 -> deserializeConstantDataCharField()
                2 -> deserializeConstantDataIntArray()
                3 -> deserializeConstantDataIntField()
                4 -> deserializeConstantDataIntegerArray()
                5 -> deserializeConstantDataStruct()
                else -> tagError()
            }
        }

    private fun deserializeConstantDataCharArray(): ConstantDataCharArray {
        val name = deserializeString()
        val value = deserializeList { deserializeSymbol { Char(deserializeInt()) } }
        return ConstantDataCharArray(name, value)
    }

    private fun deserializeConstantDataCharField(): ConstantDataCharField {
        val name = deserializeString()
        val value = deserializeSymbol { Char(deserializeInt()) }
        return ConstantDataCharField(name, value)
    }

    private fun deserializeConstantDataIntArray(): ConstantDataIntArray {
        val name = deserializeString()
        val value = deserializeList { deserializeSymbol(::deserializeInt) }
        return ConstantDataIntArray(name, value)
    }

    private fun deserializeConstantDataIntField(): ConstantDataIntField {
        val name = deserializeString()
        val value = deserializeSymbol(::deserializeInt)
        return ConstantDataIntField(name, value)
    }

    private fun deserializeConstantDataIntegerArray(): ConstantDataIntegerArray {
        val name = deserializeString()
        val value = deserializeList { b.readUInt64().toLong() }
        val integerSize = deserializeInt()
        return ConstantDataIntegerArray(name, value, integerSize)
    }

    private fun deserializeConstantDataStruct(): ConstantDataStruct {
        val name = deserializeString()
        val value = deserializeList(::deserializeConstantDataElement)
        return ConstantDataStruct(name, value)
    }

    private fun deserializeJsCodeSnippet(): WasmCompiledModuleFragment.JsCodeSnippet {
        val importName = deserializeSymbol(::deserializeString)
        val jsCode = deserializeString()
        return WasmCompiledModuleFragment.JsCodeSnippet(importName, jsCode)
    }

    private fun deserializeString(): String {
        return withFlags {
            if (it.consume()) {
                val length = b.readUInt32().toInt()
                val bytes = b.readBytes(length)
                String(bytes)
            } else {
                val lengthBytes = b.readUInt32().toInt()
                val bytes = b.readBytes(lengthBytes)
                val length = lengthBytes / Char.SIZE_BYTES
                val charArray = CharArray(length)
                for (i in 0..<length) {
                    val hi = bytes[i * Char.SIZE_BYTES].toInt() and 0xFF
                    val lo = bytes[i * Char.SIZE_BYTES + 1].toInt() and 0xFF
                    val code = hi or (lo shl Byte.SIZE_BITS)
                    charArray[i] = code.toChar()
                }
                String(charArray)
            }
        }
    }

    private fun skipString() {
        withFlags {
            it.consume()
            val length = b.readUInt32().toInt()
            b.skip(length)
        }
    }

    private fun deserializeInt() = b.readUInt32().toInt()

    private fun skipInt() {
        b.skip(4)
    }

    private fun deserializeLong() = b.readUInt64().toLong()

    private inline fun <Ir, Wasm : Any> deserializeReferencableElements(
        crossinline irDeserializeFunc: () -> Ir,
        crossinline wasmDeserializeFunc: () -> Wasm
    ) = WasmCompiledModuleFragment.ReferencableElements(
        unbound = deserializeMap(irDeserializeFunc) { deserializeSymbol(wasmDeserializeFunc) }
    )

    private inline fun <Ir, Wasm : Any> deserializeReferencableAndDefinable(
        crossinline irDeserializeFunc: () -> Ir,
        crossinline wasmDeserializeFunc: () -> Wasm
    ) = WasmCompiledModuleFragment.ReferencableAndDefinable(
        unbound = deserializeMap(irDeserializeFunc) { deserializeSymbol(wasmDeserializeFunc) },
        defined = deserializeMap(irDeserializeFunc, wasmDeserializeFunc),
        elements = deserializeList(wasmDeserializeFunc),
        wasmToIr = deserializeMap(wasmDeserializeFunc, irDeserializeFunc)
    )

    private inline fun <T : Any> deserializeSymbol(crossinline deserializeFunc: () -> T): WasmSymbol<T> =
        deserializeReference {
            withFlags { flags ->
                val owner = if (flags.consume()) null else deserializeFunc()
                WasmSymbol(owner)
            }
        }

    private fun deserializeCompiledFileFragment() = WasmCompiledFileFragment(
        functions = deserializeFunctions(),
        globalFields = deserializeGlobalFields(),
        globalVTables = deserializeGlobalVTables(),
        globalClassITables = deserializeGlobalClassITables(),
        functionTypes = deserializeFunctionTypes(),
        gcTypes = deserializeGcTypes(),
        vTableGcTypes = deserializeVTableGcTypes(),
        classITableGcType = deserializeClassITableGcType(),
        classITableInterfaceSlot = deserializeClassITableInterfaceSlot(),
        classITableInterfaceTableSize = deserializeClassITableInterfaceTableSize(),
        classITableInterfaceHasImplementors = deserializeClassITableInterfaceHasImplementors(),
        typeInfo = deserializeTypeInfo(),
        classIds = deserializeClassIds(),
        interfaceIds = deserializeInterfaceIds(),
        stringLiteralAddress = deserializeStringLiteralAddress(),
        stringLiteralPoolId = deserializeStringLiteralPoolId(),
        constantArrayDataSegmentId = deserializeConstantArrayDataSegmentId(),
        interfaceUnions = deserializeInterfaceUnions(),
        jsFuns = deserializeJsFuns(),
        jsModuleImports = deserializeJsModuleImports(),
        exports = deserializeExports(),
        scratchMemAddr = deserializeScratchMemAddr(),
        stringPoolSize = deserializeStringPoolSize(),
        throwableTagIndex = deserializeThrowableTagIndex(),
        jsExceptionTagIndex = deserializeJsExceptionTagIndex(),
        fieldInitializers = deserializeFieldInitializers(),
        mainFunctionWrappers = deserializeMainFunctionWrappers(),
        testFun = deserializeTestFun(),
        closureCallExports = deserializeClosureCallExports(),
        jsModuleAndQualifierReferences = deserializeJsModuleAndQualifierReferences(),
        classAssociatedObjectsInstanceGetters = deserializeClassAssociatedObjectInstanceGetters(),
        tryGetAssociatedObjectFun = deserializeTryGetAssociatedObject(),
        jsToKotlinAnyAdapterFun = deserializeJsToKotlinAnyAdapter(),
    )

    private fun deserializeFunctions() = deserializeReferencableAndDefinable(::deserializeIdSignature, ::deserializeFunction)
    private fun deserializeGlobalFields() = deserializeReferencableAndDefinable(::deserializeIdSignature, ::deserializeGlobal)
    private fun deserializeGlobalVTables() = deserializeReferencableAndDefinable(::deserializeIdSignature, ::deserializeGlobal)
    private fun deserializeGlobalClassITables() = deserializeReferencableAndDefinable(::deserializeIdSignature, ::deserializeGlobal)
    private fun deserializeFunctionTypes() = deserializeReferencableAndDefinable(::deserializeIdSignature, ::deserializeFunctionType)
    private fun deserializeGcTypes() = deserializeReferencableAndDefinable(::deserializeIdSignature, ::deserializeTypeDeclaration)
    private fun deserializeVTableGcTypes() = deserializeReferencableAndDefinable(::deserializeIdSignature, ::deserializeTypeDeclaration)
    private fun deserializeClassITableGcType() = deserializeReferencableElements(::deserializeIdSignature, ::deserializeTypeDeclaration)
    private fun deserializeClassITableInterfaceSlot() = deserializeReferencableElements(::deserializeIdSignature, ::deserializeInt)
    private fun deserializeClassITableInterfaceTableSize() = deserializeReferencableElements(::deserializeIdSignature, ::deserializeInt)
    private fun deserializeClassITableInterfaceHasImplementors() = deserializeReferencableElements(::deserializeIdSignature, ::deserializeInt)
    private fun deserializeTypeInfo() = deserializeMap(::deserializeIdSignature, ::deserializeConstantDataElement)
    private fun deserializeClassIds() = deserializeReferencableElements(::deserializeIdSignature, ::deserializeInt)
    private fun deserializeInterfaceIds() = deserializeReferencableElements(::deserializeIdSignature, ::deserializeInt)
    private fun deserializeStringLiteralAddress() = deserializeReferencableElements(::deserializeString, ::deserializeInt)
    private fun deserializeStringLiteralPoolId() = deserializeReferencableElements(::deserializeString, ::deserializeInt)
    private fun deserializeConstantArrayDataSegmentId(): ReferencableElements<Pair<List<Long>, WasmType>, Int> = deserializeReferencableElements({ deserializePair({ deserializeList(::deserializeLong) }, ::deserializeType) }, ::deserializeInt)
    private fun deserializeInterfaceUnions(): MutableList<List<IdSignature>> = deserializeList { deserializeList(::deserializeIdSignature) }
    private fun deserializeJsFuns() = deserializeList(::deserializeJsCodeSnippet)
    private fun deserializeJsModuleImports() = deserializeSet(::deserializeString)
    private fun deserializeExports() = deserializeList(::deserializeExport)
    private fun deserializeScratchMemAddr() = deserializeSymbol(::deserializeInt)
    private fun deserializeStringPoolSize() = deserializeSymbol(::deserializeInt)
    private fun deserializeThrowableTagIndex() = deserializeSymbol(::deserializeInt)
    private fun deserializeJsExceptionTagIndex() = deserializeSymbol(::deserializeInt)
    private fun deserializeFieldInitializers(): MutableList<FieldInitializer> = deserializeList(::deserializeFieldInitializer)
    private fun deserializeMainFunctionWrappers() = deserializeList(::deserializeIdSignature)
    private fun deserializeTestFun() = deserializeNullable(::deserializeIdSignature)
    private fun deserializeClosureCallExports() = deserializeList { deserializePair(::deserializeString, ::deserializeIdSignature) }
    private fun deserializeJsModuleAndQualifierReferences() = deserializeSet(::deserializeJsModuleAndQualifierReference)
    private fun deserializeClassAssociatedObjectInstanceGetters() = deserializeList(::deserializeClassAssociatedObjects)
    private fun deserializeTryGetAssociatedObject() = deserializeNullable(::deserializeIdSignature)
    private fun deserializeJsToKotlinAnyAdapter() = deserializeNullable(::deserializeIdSignature)

    private fun deserializeFieldInitializer(): FieldInitializer = withFlags {
        val field = deserializeIdSignature()
        val initializer = deserializeList(::deserializeInstr)
        val isObjectInstanceField = it.consume()
        FieldInitializer(field, initializer, isObjectInstanceField)
    }

    private fun deserializeAssociatedObject(): AssociatedObject = withFlags {
        val obj = deserializeIdSignature()
        val getterFunc = deserializeIdSignature()
        return AssociatedObject(obj, getterFunc, it.consume())
    }

    private fun deserializeClassAssociatedObjects(): ClassAssociatedObjects {
        val klass = deserializeIdSignature()
        val objects = deserializeList(::deserializeAssociatedObject)
        return ClassAssociatedObjects(klass, objects)
    }

    private fun deserializeJsModuleAndQualifierReference(): JsModuleAndQualifierReference = JsModuleAndQualifierReference(
        module = deserializeNullable(::deserializeString),
        qualifier = deserializeNullable(::deserializeString)
    )

    private inline fun <T : WasmNamedModuleField> deserializeNamedModuleField(crossinline deserializeFunc: (String) -> T) =
        deserializeNamedModuleField { name, _ -> deserializeFunc(name) }

    private inline fun <T : WasmNamedModuleField> deserializeNamedModuleField(crossinline deserializeFunc: (String, Flags) -> T) =
        deserializeReference {
            withFlags { flags ->
                // Deserializes the common part of WasmNamedModuleField.
                val id = if (flags.consume()) null else b.readUInt32().toInt()
                val name = if (flags.consume()) "" else deserializeString()
                deserializeFunc(name, flags).apply { this.id = id }
            }
        }

    private inline fun <T> withTag(deserializeFunc: (Int) -> T) =
        deserializeFunc(b.readUByte().toInt())

    private inline fun <T> withFlags(deserializeFunc: (Flags) -> T) =
        deserializeFunc(Flags(b.readUByte().toUInt()))

    private inline fun <T> deserializeReference(crossinline deserializeFunc: () -> T): T {
        val index = deserializeInt()
        return referenceTable[index].getOrCreate { bytes ->
            val oldB = b
            b = MyByteReader(ByteArrayInputStream(bytes))
            val result = deserializeFunc()
            b = oldB
            result
        }
    }

    private fun UByte.toBoolean(): Boolean = this == 1.toUByte()

    private fun tagError(): Nothing = error("Invalid tag")

    /**
     * Convenience wrapper class around a bitset byte
     */
    class Flags(private var flags: UInt) {
        operator fun get(i: Int): Boolean {
            if (i >= UByte.SIZE_BITS) {
                error("Flags structure can't have more than ${UByte.SIZE_BITS} flags")
            }
            return (flags and (1U shl i)) != 0U
        }

        fun consume(): Boolean {
            val result = this[0]
            flags = (flags shr 1)
            return result
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class Symbol(private val bytes: ByteArray, private var obj: Any? = null) {
        private var inConstruction = false
        inline fun <T> getOrCreate(deserialize: (ByteArray) -> T): T {
            if (obj == null) {
                if (inConstruction) {
                    error("Dependency cycle detected between reference table elements.")
                }
                inConstruction = true
                obj = deserialize(bytes)
                inConstruction = false
            }
            return obj as T
        }
    }
}
