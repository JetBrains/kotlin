/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.serialization

import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.convertors.ByteWriter
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.*

/**
 * Each polymorphic type is prepended by a type id byte, so that we know the type of the object to
 *  construct on deserialization. To prepend a serialization by an id, a [withId] block is used.
 *
 * All booleans and flags (whether a nullable field is null or not) are combined into a byte bitset.
 *  This bitset (if exists) is prepended to the struct. The interpretation of each bit of the bitset
 *  is up to each function implementation. One exception is when there is only a single boolean field,
 *  in which case, it can be anywhere. To prepend a serialization by a bitset, a [withFlags] block is used.
 *
 * Some objects need to be serialized as references, meaning that if the same instance is serialized
 *  in more than one place, all deserialized instances must be the same in all of these locations.
 *  [WasmSymbol] is the most obvious example for instances that must be serialized by reference, but
 *  is not the only object type. To change a serialization to be by reference instead of by value,
 *  all you need is to put the serialization code inside a [serializeAsReference] block.
 *
 * For clarity, we'll use [WasmSymbol] as an example when discussing the reference table.
 * When deserializing, a [WasmSymbol] referenced by multiple object (serialized multiple times)
 *  all deserialized instances of this symbol must be the same instance. For this to be possible,
 *  symbols are stored in a reference table at the front of the serialized output. When a symbol
 *  is to be serialized, it's substituted by an integer that indicates its position in the table.
 *  This integer is used as a reference to the symbol. If the same symbol is referenced at two
 *  locations, the serialization output will contain the same integer in place of the two symbol
 *  references, thus, pointing to the same location in the reference table.
 *
 * The structure of the serialized reference table is as follows:
 *  N n0 d0 n1 d1 n2 d2... where:
 *    N : number of elements in the table
 *    nx: size (in bytes) of the table slot for element x
 *    dx: actual data of element x
 *  We need to store the size of each element because element sizes are not the same.
 *
 * The reference table can contain any object type, and not limited to a set of types.
 *
 * When deserializing a reference, the type of the data in each slot is determined by the context.
 *  For example, if we're deserializing a class `X` that contains a `WasmSymbol<Y>`, and the
 *  reference for this symbol is 3, then we know that slot 3 in the index table contains
 *  a `WasmSymbol<Y>`, and this is how we're going to interpret the bytes in slot 3.
 *
 * See [WasmDeserializer]
 */

class WasmSerializer(outputStream: OutputStream) {

    /**
     * @property id Unique identifier for the reference that is used as an index in the reference table
     * @property serializeFunc Contains the serialization logic for the desired object
     */
    private data class DeferredSerialization(val id: Int, val serializeFunc: () -> Unit)

    /**
     * Used to make sure that if a reference is already serialized, it doesn't get serialized again.
     *  This happens by assigning each reference a unique id (an index into the reference table). This
     *  id is used as a reference to the object in the reference table.
     *
     * When serializing a reference, the object is serialized into the table, and is substituted by its id.
     *
     * This will be used once all objects are processed, and the number of references are known.
     *  After holding the content of the serialization body in the [bodyBuffer] temporarily, the
     *  functions in [referenceTable] is executed one by one, ordered by the id, effectively,
     *  serializing the content of the reference table first, then the content in [bodyBuffer]
     *  is output after the reference table content.
     *
     * This map must compare REFERENCES, and not values, two different symbol instances should serialize
     *  twice even if they're equal in value, but the same instance serialized twice should serialize only once.
     */
    private val referenceTable: IdentityHashMap<Any, DeferredSerialization> = IdentityHashMap()

    /**
     * When executing serialization functions in [referenceTable], these functions may introduce new
     *  references that need to be added to the reference table. To process all references, this list
     *  accumulates references added to the [referenceTable], copied (sorted), cleared, then functions
     *  in this list are executed. This process repeats while [newReferences] is not empty.
     *
     * Note that we can't clear the [referenceTable] instead, because this holds all references and is
     *  used as a lookup table as well to avoid serializing the same instance more than one.
     */
    private val newReferences = mutableListOf<DeferredSerialization>()

    /**
     * Body of the serialized file. It's kept in a buffer (not written directly), because we'll
     *  need to defer writing the body until the reference table is created and written.
     */
    private val bodyBuffer = ByteArrayOutputStream()
    private val body = ByteWriter.OutputStream(bodyBuffer)

    /**
     * Temporary buffer, mainly used to store content that is prepended by its size, but the
     *  size is not known in advance. The output is written in this temp buffer, then its
     *  size is written, then the content in the temp buffer is written.
     *
     * We need reference to the tempBuffer as a [ByteArrayOutputStream] to be able to
     *  reset its content manually.
     */
    private val tempBuffer = ByteArrayOutputStream()
    private val temp = ByteWriter.OutputStream(tempBuffer)

    /**
     * The current serialization target. This can be set to differently according to the current
     *  stage in the serialization process.
     *
     * This is set to `body` by default, because this is the first target the bytes will be written to.
     */
    private var b: ByteWriter = body

    private val out = ByteWriter.OutputStream(outputStream)

    fun serialize(compiledFileFragment: WasmCompiledFileFragment) {
        // Step 1: process non-deferred serializations (put into bodyBuffer temporarily)
        serializeCompiledFileFragment(compiledFileFragment)

        // Step 2: output the reference table first, in the form: size content
        // Step 2.1: output each element in the form: sizeInBytes data

        // Compute the bytes of each table slot
        b = temp
        val tableElementsInBytes = mutableListOf<ByteArray>()
        while (newReferences.isNotEmpty()) {
            val sorted = newReferences.sortedBy { it.id }
            newReferences.clear()
            sorted.forEach {
                tempBuffer.reset()
                it.serializeFunc()
                tableElementsInBytes.add(tempBuffer.toByteArray())
            }
        }

        // Output the size of the reference table first
        val size = tableElementsInBytes.size
        out.writeUInt32(size.toUInt())

        // Output each element in the form: sizeInBytes data
        tableElementsInBytes.forEach {
            val bytesCount = it.size
            out.writeUInt32(bytesCount.toUInt())
            out.writeBytes(it)
        }

        // Step 3: after the reference table is written, write the body
        out.writeBytes(bodyBuffer.toByteArray())
    }

    private fun serialize(func: WasmFunction) =
        serializeNamedModuleField(func) {
            serialize(func.type, ::serialize)
            when (func) {
                is WasmFunction.Defined -> withId(0U) {
                    serialize(func.locals, ::serialize)
                    serialize(func.instructions, ::serialize)
                }
                is WasmFunction.Imported -> withId(1U) {
                    serialize(func.importPair)
                }
            }
        }

    private fun serialize(global: WasmGlobal) =
        serializeNamedModuleField(global, listOf(global.isMutable, global.importPair == null)) {
            serialize(global.type)
            serialize(global.init, ::serialize)
            global.importPair?.let { serialize(it) }
        }

    private fun serialize(funcType: WasmFunctionType) =
        serializeNamedModuleField(funcType) {
            serialize(funcType.parameterTypes, ::serialize)
            serialize(funcType.resultTypes, ::serialize)
        }

    private fun serialize(typeDecl: WasmTypeDeclaration): Unit =
        when (typeDecl) {
            is WasmFunctionType -> withId(0U) { serialize(typeDecl) }
            is WasmStructDeclaration -> withId(1U) { serialize(typeDecl) }
            is WasmArrayDeclaration -> withId(2U) { serialize(typeDecl) }
        }

    private fun serialize(structDecl: WasmStructDeclaration) {
        serializeNamedModuleField(structDecl, listOf(structDecl.superType == null, structDecl.isFinal)) {
            serialize(structDecl.fields, ::serialize)
            structDecl.superType?.let { serialize(it, ::serialize) }
        }
    }

    private fun serialize(arrDecl: WasmArrayDeclaration): Unit =
        serializeNamedModuleField(arrDecl, listOf(arrDecl.field.isMutable)) {
            serialize(arrDecl.field.name)
            serialize(arrDecl.field.type)
        }

    private fun serialize(memory: WasmMemory) =
        serializeNamedModuleField(memory, listOf(memory.importPair == null)) {
            serialize(memory.limits)
            memory.importPair?.let { serialize(it) }
        }

    private fun serialize(tag: WasmTag): Unit =
        serializeNamedModuleField(tag, listOf(tag.importPair == null)) {
            serialize(tag.type)
            tag.importPair?.let { serialize(it) }
        }

    private fun serialize(structFieldDecl: WasmStructFieldDeclaration) {
        serialize(structFieldDecl.name)
        serialize(structFieldDecl.type)
        b.writeByte(structFieldDecl.isMutable.toByte())
    }

    private fun serialize(type: WasmType) =
        when (type) {
            is WasmRefType -> withId(0U) { serialize(type.heapType) }
            is WasmRefNullType -> withId(1U) { serialize(type.heapType) }
            else -> {
                WASM_TYPE_OBJECTS.forEachIndexed { i, obj ->
                    if (type == obj) {
                        withId((i + 2).toUByte()) { }
                        return
                    }
                }
                error("Not supported type: ${type::class}")
            }
        }

    private fun serialize(type: WasmHeapType) =
        when (type) {
            WasmHeapType.Simple.Any -> withId(0U) { }
            WasmHeapType.Simple.Eq -> withId(1U) { }
            WasmHeapType.Simple.Extern -> withId(2U) { }
            WasmHeapType.Simple.Func -> withId(3U) { }
            WasmHeapType.Simple.NoExtern -> withId(4U) { }
            WasmHeapType.Simple.None -> withId(5U) { }
            WasmHeapType.Simple.Struct -> withId(6U) { }
            is WasmHeapType.Type -> withId(7U) { serialize(type.type) { serialize(it) } }
        }

    private fun serialize(local: WasmLocal) {
        b.writeUInt32(local.id.toUInt())
        serialize(local.name)
        serialize(local.type)
        b.writeByte(local.isParameter.toByte())
    }

    private fun serialize(instr: WasmInstr) {
        var opcode = instr.operator.opcode
        if (opcode == WASM_OP_PSEUDO_OPCODE) {
            opcode = when (instr.operator) {
                WasmOp.PSEUDO_COMMENT_PREVIOUS_INSTR -> 0xFFFF - 0
                WasmOp.PSEUDO_COMMENT_GROUP_START -> 0xFFFF - 1
                WasmOp.PSEUDO_COMMENT_GROUP_END -> 0xFFFF - 2
                WasmOp.MACRO_IF -> 0xFFFF - 3
                WasmOp.MACRO_ELSE -> 0xFFFF - 4
                WasmOp.MACRO_END_IF -> 0xFFFF - 5
                WasmOp.MACRO_TABLE -> 0xFFFF - 6
                WasmOp.MACRO_TABLE_INDEX -> 0xFFFF - 7
                WasmOp.MACRO_TABLE_END -> 0xFFFF - 8
                else -> error("Unknown pseudo-opcode: $instr")
            }
        }
        b.writeUInt16(opcode.toUShort())
        when (instr) {
            is WasmInstrWithLocation -> withId(0U) { serialize(instr.immediates, ::serialize); serialize(instr.location) }
            is WasmInstrWithoutLocation -> withId(1U) { serialize(instr.immediates, ::serialize); }
        }
    }

    private fun serialize(i: WasmImmediate): Unit =
        when (i) {
            is WasmImmediate.BlockType.Function -> withId(0U) { serialize(i.type) }
            is WasmImmediate.BlockType.Value -> withIdNullable(1U, i.type) { serialize(i.type!!) }
            is WasmImmediate.Catch -> withId(2U) { serialize(i) }
            is WasmImmediate.ConstF32 -> withId(3U) { b.writeUInt32(i.rawBits) }
            is WasmImmediate.ConstF64 -> withId(4U) { b.writeUInt64(i.rawBits) }
            is WasmImmediate.ConstI32 -> withId(5U) { b.writeUInt32(i.value.toUInt()) }
            is WasmImmediate.ConstI64 -> withId(6U) { b.writeUInt64(i.value.toULong()) }
            is WasmImmediate.ConstString -> withId(7U) { serialize(i.value) }
            is WasmImmediate.ConstU8 -> withId(8U) { b.writeUByte(i.value) }
            is WasmImmediate.DataIdx -> withId(9U) { serialize(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.ElemIdx -> withId(10U) { serialize(i.value) }
            is WasmImmediate.FuncIdx -> withId(11U) { serialize(i.value) { serialize(it) } }
            is WasmImmediate.GcType -> withId(12U) { serialize(i.value) { serialize(it) } }
            is WasmImmediate.GlobalIdx -> withId(13U) { serialize(i.value) { serialize(it) } }
            is WasmImmediate.HeapType -> withId(14U) { serialize(i.value) }
            is WasmImmediate.LabelIdx -> withId(15U) { b.writeUInt32(i.value.toUInt()) }
            is WasmImmediate.LabelIdxVector -> withId(16U) { serialize(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.LocalIdx -> withId(17U) { serialize(i.value) { serialize(it) } }
            is WasmImmediate.MemArg -> withId(18U) { b.writeUInt32(i.align); b.writeUInt32(i.offset) }
            is WasmImmediate.MemoryIdx -> withId(19U) { b.writeUInt32(i.value.toUInt()) }
            is WasmImmediate.StructFieldIdx -> withId(20U) { serialize(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.SymbolI32 -> withId(21U) { serialize(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.TableIdx -> withId(22U) { serialize(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.TagIdx -> withId(23U) { b.writeUInt32(i.value.toUInt()) }
            is WasmImmediate.TypeIdx -> withId(24U) { serialize(i.value) { serialize(it) } }
            is WasmImmediate.ValTypeVector -> withId(25U) { serialize(i.value, ::serialize) }
        }

    private fun serialize(catch: WasmImmediate.Catch) {
        val type = when (catch.type) {
            WasmImmediate.Catch.CatchType.CATCH -> 0
            WasmImmediate.Catch.CatchType.CATCH_REF -> 1
            WasmImmediate.Catch.CatchType.CATCH_ALL -> 2
            WasmImmediate.Catch.CatchType.CATCH_ALL_REF -> 3
        }.toByte()
        b.writeByte(type)
        serialize(catch.immediates, ::serialize)
    }

    private fun serialize(table: WasmTable) {
        val max = table.limits.maxSize
        val ip = table.importPair
        serializeNamedModuleField(table, listOf(max == null, ip == null)) {
            b.writeUInt32(table.limits.minSize)
            max?.let { b.writeUInt32(it) }
            serialize(table.elementType)
            ip?.let { serialize(it) }
        }
    }

    private fun serialize(value: WasmTable.Value): Unit =
        when (value) {
            is WasmTable.Value.Expression -> withId(0U) { serialize(value.expr, ::serialize) }
            is WasmTable.Value.Function -> withId(1U) { serialize(value.function) { serialize(it) } }
        }

    private fun serialize(element: WasmElement): Unit =
        serializeNamedModuleField(element) {
            serialize(element.type)
            serialize(element.values, ::serialize)
            serialize(element.mode)
        }

    private fun serialize(mode: WasmElement.Mode) =
        when (mode) {
            is WasmElement.Mode.Active -> withId(0U) {
                serialize(mode.table)
                serialize(mode.offset, ::serialize)
            }
            WasmElement.Mode.Declarative -> withId(1U) { }
            WasmElement.Mode.Passive -> withId(2U) { }
        }

    private fun serialize(export: WasmExport<*>) {
        // The name is serialized before the id.
        serialize(export.name)
        when (export) {
            is WasmExport.Function -> withId(0U) { serialize(export.field) }
            is WasmExport.Table -> withId(1U) { serialize(export.field) }
            is WasmExport.Memory -> withId(2U) { serialize(export.field) }
            is WasmExport.Global -> withId(3U) { serialize(export.field) }
            is WasmExport.Tag -> withId(4U) { serialize(export.field) }
        }
    }

    private fun serialize(limit: WasmLimits) =
        withFlags(limit.maxSize == null) {
            b.writeUInt32(limit.minSize)
            limit.maxSize?.let { b.writeUInt32(it) }
        }

    private fun serialize(descriptor: WasmImportDescriptor) {
        serialize(descriptor.moduleName)
        serialize(descriptor.declarationName, ::serialize)
    }

    private fun <A, B> serialize(pair: Pair<A, B>, serializeAFunc: (A) -> Unit, serializeBFunc: (B) -> Unit) {
        serializeAFunc(pair.first)
        serializeBFunc(pair.second)
    }

    private fun <T> serialize(list: List<T>, serializeFunc: (T) -> Unit) {
        b.writeUInt32(list.size.toUInt())
        list.forEach { serializeFunc(it) }
    }

    private fun <T> serialize(set: Set<T>, serializeFunc: (T) -> Unit) {
        b.writeUInt32(set.size.toUInt())
        set.forEach { serializeFunc(it) }
    }

    private fun <K, V> serialize(map: Map<K, V>, serializeKeyFunc: (K) -> Unit, serializeValueFunc: (V) -> Unit) {
        b.writeUInt32(map.size.toUInt())
        map.forEach { (key, value) ->
            serializeKeyFunc(key)
            serializeValueFunc(value)
        }
    }

    private fun serialize(sl: SourceLocation) =
        when (sl) {
            SourceLocation.NoLocation -> withId(0U) { }
            is SourceLocation.Location -> withId(1U) {
                serialize(sl.file)
                b.writeUInt32(sl.line.toUInt())
                b.writeUInt32(sl.column.toUInt())
            }
            is SourceLocation.IgnoredLocation -> withId(1U) {
                serialize(sl.file)
                b.writeUInt32(sl.line.toUInt())
                b.writeUInt32(sl.column.toUInt())
            }
        }

    private fun serialize(idSignature: IdSignature) =
        when (idSignature) {
            is IdSignature.AccessorSignature -> withId(0U) { serialize(idSignature) }
            is IdSignature.CommonSignature -> withId(1U) { serialize(idSignature) }
            is IdSignature.CompositeSignature -> withId(2U) { serialize(idSignature) }
            is IdSignature.FileLocalSignature -> withId(3U) { serialize(idSignature) }
            is IdSignature.LocalSignature -> withId(4U) { serialize(idSignature) }
            is IdSignature.LoweredDeclarationSignature -> withId(5U) { serialize(idSignature) }
            is IdSignature.ScopeLocalDeclaration -> withId(6U) { serialize(idSignature) }
            is IdSignature.SpecialFakeOverrideSignature -> withId(7U) { serialize(idSignature) }
            is IdSignature.FileSignature -> withId(8U) { }
        }

    private fun serialize(accessor: IdSignature.AccessorSignature) {
        with(accessor) {
            serialize(propertySignature)
            serialize(accessorSignature)
        }
    }

    private fun serialize(common: IdSignature.CommonSignature) {
        with(common) {
            withFlags(id == null, description == null) {
                serialize(packageFqName)
                serialize(declarationFqName)
                id?.let { b.writeUInt64(it.toULong()) }
                b.writeUInt64(mask.toULong())
                description?.let { serialize(it) }
            }
        }
    }

    private fun serialize(composite: IdSignature.CompositeSignature) {
        with(composite) {
            serialize(container)
            serialize(inner)
        }
    }

    private fun serialize(fileLocal: IdSignature.FileLocalSignature) {
        with(fileLocal) {
            withFlags(description == null) {
                serialize(container)
                b.writeUInt64(id.toULong())
                description?.let { serialize(it) }
            }
        }
    }

    private fun serialize(local: IdSignature.LocalSignature) {
        with(local) {
            withFlags(hashSig == null, description == null) {
                serialize(localFqn)
                hashSig?.let { b.writeUInt64(it.toULong()) }
                description?.let { serialize(it) }
            }
        }
    }

    private fun serialize(loweredDeclaration: IdSignature.LoweredDeclarationSignature) {
        with(loweredDeclaration) {
            serialize(original)
            b.writeUInt32(stage.toUInt())
            b.writeUInt32(index.toUInt())
        }
    }

    private fun serialize(scopeLocal: IdSignature.ScopeLocalDeclaration) {
        with(scopeLocal) {
            withFlags(description == null) {
                b.writeUInt32(id.toUInt())
                description?.let { serialize(it) }
            }
        }
    }

    private fun serialize(specialFakeOverride: IdSignature.SpecialFakeOverrideSignature) {
        with(specialFakeOverride) {
            serialize(memberSignature)
            serialize(overriddenSignatures, ::serialize)
        }
    }

    private fun serialize(constantDataElement: ConstantDataElement) {
        when (constantDataElement) {
            is ConstantDataCharArray -> withId(0U) { serialize(constantDataElement) }
            is ConstantDataCharField -> withId(1U) { serialize(constantDataElement) }
            is ConstantDataIntArray -> withId(2U) { serialize(constantDataElement) }
            is ConstantDataIntField -> withId(3U) { serialize(constantDataElement) }
            is ConstantDataIntegerArray -> withId(4U) { serialize(constantDataElement) }
            is ConstantDataStruct -> withId(5U) { serialize(constantDataElement) }
        }
    }

    private fun serialize(constantDataCharArray: ConstantDataCharArray) {
        serialize(constantDataCharArray.name)
        serialize(constantDataCharArray.value) { serialize(it) { b.writeUInt32(it.code.toUInt()) } }
    }

    private fun serialize(constantDataCharField: ConstantDataCharField) {
        serialize(constantDataCharField.name)
        serialize(constantDataCharField.value) { b.writeUInt32(it.code.toUInt()) }
    }

    private fun serialize(constantDataIntArray: ConstantDataIntArray) {
        serialize(constantDataIntArray.name)
        serialize(constantDataIntArray.value) { serialize(it) { b.writeUInt32(it.toUInt()) } }
    }

    private fun serialize(constantDataIntField: ConstantDataIntField) {
        serialize(constantDataIntField.name)
        serialize(constantDataIntField.value) { b.writeUInt32(it.toUInt()) }
    }

    private fun serialize(constantDataIntegerArray: ConstantDataIntegerArray) {
        serialize(constantDataIntegerArray.name)
        serialize(constantDataIntegerArray.value) { b.writeUInt64(it.toULong()) }
        b.writeUInt32(constantDataIntegerArray.integerSize.toUInt())
    }

    private fun serialize(constantDataStruct: ConstantDataStruct) {
        serialize(constantDataStruct.name)
        serialize(constantDataStruct.elements, ::serialize)
    }

    private fun serialize(jsCodeSnippet: WasmCompiledModuleFragment.JsCodeSnippet) {
        serialize(jsCodeSnippet.importName, ::serialize)
        serialize(jsCodeSnippet.jsCode)
    }

    private fun serialize(funWithPriority: WasmCompiledModuleFragment.FunWithPriority) {
        serialize(funWithPriority.function)
        serialize(funWithPriority.priority)
    }

    private fun serialize(str: String) {
        val bytes = str.toByteArray()
        b.writeUInt32(bytes.size.toUInt())
        b.writeBytes(bytes)
    }

    private fun serialize(int: Int) {
        b.writeUInt32(int.toUInt())
    }

    private fun serialize(long: Long) {
        b.writeUInt64(long.toULong())
    }

    private fun <Ir, Wasm : Any> serialize(
        referencableElements: WasmCompiledModuleFragment.ReferencableElements<Ir, Wasm>,
        irSerializeFunc: (Ir) -> Unit,
        wasmSerializeFunc: (Wasm) -> Unit
    ) = serialize(referencableElements.unbound, irSerializeFunc) { serialize(it, wasmSerializeFunc) }

    private fun <Ir, Wasm : Any> serialize(
        referencableAndDefinable: WasmCompiledModuleFragment.ReferencableAndDefinable<Ir, Wasm>,
        irSerializeFunc: (Ir) -> Unit,
        wasmSerializeFunc: (Wasm) -> Unit
    ) = with(referencableAndDefinable) {
        serialize(unbound, irSerializeFunc) { serialize(it, wasmSerializeFunc) }
        serialize(defined, irSerializeFunc, wasmSerializeFunc)
        serialize(elements, wasmSerializeFunc)
        serialize(wasmToIr, wasmSerializeFunc, irSerializeFunc)
    }

    private fun <T : Any> serialize(symbol: WasmSymbolReadOnly<T>, serializeFunc: (T) -> Unit) =
        serializeAsReference(symbol) {
            withFlags(symbol.getOwner() == null) {
                symbol.getOwner()?.let { serializeFunc(it) }
            }
        }

    private fun serializeCompiledFileFragment(compiledFileFragment: WasmCompiledFileFragment) =
        with(compiledFileFragment) {
            serialize(functions, ::serialize, ::serialize)
            serialize(globalFields, ::serialize, ::serialize)
            serialize(globalVTables, ::serialize, ::serialize)
            serialize(globalClassITables, ::serialize, ::serialize)
            serialize(functionTypes, ::serialize, ::serialize)
            serialize(gcTypes, ::serialize, ::serialize)
            serialize(vTableGcTypes, ::serialize, ::serialize)
            serialize(classITableGcType, ::serialize, ::serialize)
            serialize(classITableInterfaceSlot, ::serialize, ::serialize)
            serialize(classITableInterfaceTableSize, ::serialize, ::serialize)
            serialize(classITableInterfaceHasImplementors, ::serialize, ::serialize)
            serialize(typeInfo, ::serialize, ::serialize)
            serialize(classIds, ::serialize, ::serialize)
            serialize(interfaceIds, ::serialize, ::serialize)
            serialize(stringLiteralAddress, ::serialize, ::serialize)
            serialize(stringLiteralPoolId, ::serialize, ::serialize)
            serialize(constantArrayDataSegmentId, { serialize(it, { serialize(it, ::serialize) }, ::serialize)}, ::serialize)
            serialize(interfaceUnions) { serialize(it, ::serialize) }
            serialize(declaredInterfaces, ::serialize)
            serialize(initFunctions, ::serialize)
            serialize(uniqueJsFunNames, ::serialize, ::serialize)
            serialize(jsFuns, ::serialize)
            serialize(jsModuleImports, ::serialize)
            serialize(exports, ::serialize)
            serialize(scratchMemAddr, ::serialize)
            serialize(stringPoolSize, ::serialize)
        }

    private fun serializeNamedModuleField(obj: WasmNamedModuleField, flags: List<Boolean> = listOf(), serializeFunc: () -> Unit) =
        serializeAsReference(obj) {
            // Serializes the common part of WasmNamedModuleField.
            withFlags(*listOf(obj.id == null, obj.name.isEmpty()).plus(flags).toBooleanArray()) {
                obj.id?.let { b.writeUInt32(it.toUInt()) }
                if (obj.name.isNotEmpty()) serialize(obj.name)
                serializeFunc()
            }
        }

    private fun withIdNullable(id: UByte, obj: Any?, serializeFunc: () -> Unit) {
        // Use the MSB of the id as a flag
        val isNull = if (obj == null) 1U else 0U
        val newId = (id.toUInt() or (isNull shl 7)).toUByte()
        b.writeUByte(newId)
        if (isNull != 1U) serializeFunc()
    }

    private fun withId(id: UByte, serializeFunc: () -> Unit) {
        b.writeUByte(id)
        serializeFunc()
    }

    private fun withFlags(vararg flags: Boolean, serializeFunc: () -> Unit) {
        if (flags.size > 8) {
            error("Can't pack more than 8 flags in a single byte")
        }
        b.writeUByte(flagsToUByte(flags))
        serializeFunc()
    }

    private fun serializeAsReference(obj: Any, serializeFunc: () -> Unit) {
        val id = referenceTable.getOrElse(obj) {
            DeferredSerialization(referenceTable.size, serializeFunc).also {
                referenceTable[obj] = it
                newReferences.add(it)
            }
        }.id
        serialize(id)
    }

    private fun <T : Any> WasmSymbolReadOnly<T>.getOwner() =
        when (this) {
            is WasmSymbol<T> -> if (isBound()) owner else null
            else -> error("Unsupported symbol type: ${this::class}")
        }

    private fun flagsToUByte(flags: BooleanArray): UByte {
        var result = 0U
        flags.forEachIndexed { i, flag ->
            if (flag) result = result or (1U shl i)
        }
        return result.toUByte()
    }

    private fun Boolean.toByte(): Byte = (if (this) 1 else 0).toByte()
}

val WASM_TYPE_OBJECTS = listOf(
    WasmAnyRef,
    WasmEqRef,
    WasmExnRefType,
    WasmExternRef,
    WasmF32,
    WasmF64,
    WasmFuncRef,
    WasmI16,
    WasmI31Ref,
    WasmI32,
    WasmI64,
    WasmI8,
    WasmNullExnRefType,
    WasmRefNullExternrefType,
    WasmRefNullrefType,
    WasmStructRef,
    WasmUnreachableType,
    WasmV128
)