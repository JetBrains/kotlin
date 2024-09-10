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
 * Each polymorphic type is prepended by a type tag byte, so that we know the type of the object to
 *  construct on deserialization. To prepend a serialization by a tag, a [withTag] block is used.
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

    private fun serializeWasmFunction(func: WasmFunction) =
        serializeNamedModuleField(func) {
            serializeWasmSymbolReadOnly(func.type, ::serializeWasmFunctionType)
            when (func) {
                is WasmFunction.Defined -> withTag(FunctionTags.DEFINED) {
                    serializeList(func.locals, ::serializeWasmLocal)
                    serializeList(func.instructions, ::serializeWasmInstr)
                }
                is WasmFunction.Imported -> withTag(FunctionTags.IMPORTED) {
                    serializeWasmImportDescriptor(func.importPair)
                }
            }
        }

    private fun serializeWasmGlobal(global: WasmGlobal) =
        serializeNamedModuleField(global, listOf(global.isMutable, global.importPair == null)) {
            serializeWasmType(global.type)
            serializeList(global.init, ::serializeWasmInstr)
            global.importPair?.let { serializeWasmImportDescriptor(it) }
        }

    private fun serializeWasmFunctionType(funcType: WasmFunctionType) =
        serializeNamedModuleField(funcType) {
            serializeList(funcType.parameterTypes, ::serializeWasmType)
            serializeList(funcType.resultTypes, ::serializeWasmType)
        }

    private fun serializeWasmTypeDeclaration(typeDecl: WasmTypeDeclaration): Unit =
        when (typeDecl) {
            is WasmFunctionType -> withTag(TypeDeclarationTags.FUNCTION) { serializeWasmFunctionType(typeDecl) }
            is WasmStructDeclaration -> withTag(TypeDeclarationTags.STRUCT) { serializeWasmStructDeclaration(typeDecl) }
            is WasmArrayDeclaration -> withTag(TypeDeclarationTags.ARRAY) { serializeWasmArrayDeclaration(typeDecl) }
        }

    private fun serializeWasmStructDeclaration(structDecl: WasmStructDeclaration) {
        serializeNamedModuleField(structDecl, listOf(structDecl.superType == null, structDecl.isFinal)) {
            serializeList(structDecl.fields, ::serializeWasmStructFieldDeclaration)
            structDecl.superType?.let { serializeWasmSymbolReadOnly(it, ::serializeWasmTypeDeclaration) }
        }
    }

    private fun serializeWasmArrayDeclaration(arrDecl: WasmArrayDeclaration): Unit =
        serializeNamedModuleField(arrDecl, listOf(arrDecl.field.isMutable)) {
            serializeString(arrDecl.field.name)
            serializeWasmType(arrDecl.field.type)
        }

    private fun serializeWasmMemory(memory: WasmMemory) =
        serializeNamedModuleField(memory, listOf(memory.importPair == null)) {
            serializeWasmLimits(memory.limits)
            memory.importPair?.let { serializeWasmImportDescriptor(it) }
        }

    private fun serializeWasmTag(tag: WasmTag): Unit =
        serializeNamedModuleField(tag, listOf(tag.importPair == null)) {
            serializeWasmFunctionType(tag.type)
            tag.importPair?.let { serializeWasmImportDescriptor(it) }
        }

    private fun serializeWasmStructFieldDeclaration(structFieldDecl: WasmStructFieldDeclaration) {
        serializeString(structFieldDecl.name)
        serializeWasmType(structFieldDecl.type)
        b.writeByte(structFieldDecl.isMutable.toByte())
    }

    private fun serializeWasmType(type: WasmType) =
        when (type) {
            is WasmRefType -> withTag(TypeTags.REF) { serializeWasmHeapType(type.heapType) }
            is WasmRefNullType -> withTag(TypeTags.REF_NULL) { serializeWasmHeapType(type.heapType) }
            WasmAnyRef -> setTag(TypeTags.ANYREF)
            WasmEqRef -> setTag(TypeTags.EQREF)
            WasmExnRefType -> setTag(TypeTags.EXTERN_REF_TYPE)
            WasmExternRef -> setTag(TypeTags.EXTERN_REF)
            WasmF32 -> setTag(TypeTags.F32)
            WasmF64 -> setTag(TypeTags.F64)
            WasmFuncRef -> setTag(TypeTags.FUNC_REF)
            WasmI16 -> setTag(TypeTags.I16)
            WasmI31Ref -> setTag(TypeTags.I31_REF)
            WasmI32 -> setTag(TypeTags.I32)
            WasmI64 -> setTag(TypeTags.I64)
            WasmI8 -> setTag(TypeTags.I8)
            WasmNullExnRefType -> setTag(TypeTags.NULL_EXTERN_REF_TYPE)
            WasmRefNullExternrefType -> setTag(TypeTags.REF_NULL_EXTERN_REF_TYPE)
            WasmRefNullrefType -> setTag(TypeTags.REF_NULL_REF_TYPE)
            WasmStructRef -> setTag(TypeTags.STRUCT_REF)
            WasmUnreachableType -> setTag(TypeTags.UNREACHABLE_TYPE)
            WasmV128 -> setTag(TypeTags.V12)
        }

    private fun serializeWasmHeapType(type: WasmHeapType) =
        when (type) {
            WasmHeapType.Simple.Any -> setTag(HeapTypeTags.ANY)
            WasmHeapType.Simple.Eq -> setTag(HeapTypeTags.EQ)
            WasmHeapType.Simple.Extern -> setTag(HeapTypeTags.EXTERN)
            WasmHeapType.Simple.Func -> setTag(HeapTypeTags.FUNC)
            WasmHeapType.Simple.NoExtern -> setTag(HeapTypeTags.NO_EXTERN)
            WasmHeapType.Simple.None -> setTag(HeapTypeTags.NONE)
            WasmHeapType.Simple.Struct -> setTag(HeapTypeTags.STRUCT)
            is WasmHeapType.Type -> withTag(HeapTypeTags.HEAP_TYPE) { serializeWasmSymbolReadOnly(type.type) { serializeWasmTypeDeclaration(it) } }
        }

    private fun serializeWasmLocal(local: WasmLocal) {
        b.writeUInt32(local.id.toUInt())
        serializeString(local.name)
        serializeWasmType(local.type)
        b.writeByte(local.isParameter.toByte())
    }

    private fun serializeWasmInstr(instr: WasmInstr) {
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
            is WasmInstrWithLocation -> withTag(InstructionTags.WITH_LOCATION) { serializeList(instr.immediates, ::serializeWasmImmediate); serializeSourceLocation(instr.location) }
            is WasmInstrWithoutLocation -> withTag(InstructionTags.WITHOUT_LOCATION) { serializeList(instr.immediates, ::serializeWasmImmediate); }
        }
    }

    private fun serializeWasmImmediate(i: WasmImmediate): Unit =
        when (i) {
            is WasmImmediate.BlockType.Function -> withTag(ImmediateTags.BLOCK_TYPE_FUNCTION) { serializeWasmFunctionType(i.type) }
            is WasmImmediate.BlockType.Value -> withTagNullable(ImmediateTags.BLOCK_TYPE_VALUE, i.type) { serializeWasmType(i.type!!) }
            is WasmImmediate.Catch -> withTag(ImmediateTags.CATCH) { serializeCatchImmediate(i) }
            is WasmImmediate.ConstF32 -> withTag(ImmediateTags.CONST_F32) { b.writeUInt32(i.rawBits) }
            is WasmImmediate.ConstF64 -> withTag(ImmediateTags.CONST_F64) { b.writeUInt64(i.rawBits) }
            is WasmImmediate.ConstI32 -> withTag(ImmediateTags.CONST_I32) { b.writeUInt32(i.value.toUInt()) }
            is WasmImmediate.ConstI64 -> withTag(ImmediateTags.CONST_I64) { b.writeUInt64(i.value.toULong()) }
            is WasmImmediate.ConstString -> withTag(ImmediateTags.CONST_STRING) { serializeString(i.value) }
            is WasmImmediate.ConstU8 -> withTag(ImmediateTags.CONST_U8) { b.writeUByte(i.value) }
            is WasmImmediate.DataIdx -> withTag(ImmediateTags.DATA_INDEX) { serializeWasmSymbolReadOnly(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.ElemIdx -> withTag(ImmediateTags.ELEMENT_INDEX) { serializeWasmElement(i.value) }
            is WasmImmediate.FuncIdx -> withTag(ImmediateTags.FUNC_INDEX) { serializeWasmSymbolReadOnly(i.value) { serializeWasmFunction(it) } }
            is WasmImmediate.GcType -> withTag(ImmediateTags.GC_TYPE) { serializeWasmSymbolReadOnly(i.value) { serializeWasmTypeDeclaration(it) } }
            is WasmImmediate.GlobalIdx -> withTag(ImmediateTags.GLOBAL_INDEX) { serializeWasmSymbolReadOnly(i.value) { serializeWasmGlobal(it) } }
            is WasmImmediate.HeapType -> withTag(ImmediateTags.HEAP_TYPE) { serializeWasmHeapType(i.value) }
            is WasmImmediate.LabelIdx -> withTag(ImmediateTags.LABEL_INDEX) { b.writeUInt32(i.value.toUInt()) }
            is WasmImmediate.LabelIdxVector -> withTag(ImmediateTags.LABEL_INDEX_VECTOR) { serializeList(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.LocalIdx -> withTag(ImmediateTags.LOCAL_INDEX) { serializeWasmSymbolReadOnly(i.value) { serializeWasmLocal(it) } }
            is WasmImmediate.MemArg -> withTag(ImmediateTags.MEM_ARG) { b.writeUInt32(i.align); b.writeUInt32(i.offset) }
            is WasmImmediate.MemoryIdx -> withTag(ImmediateTags.MEMORY_INDEX) { b.writeUInt32(i.value.toUInt()) }
            is WasmImmediate.StructFieldIdx -> withTag(ImmediateTags.STRUCT_FIELD_INDEX) { serializeWasmSymbolReadOnly(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.SymbolI32 -> withTag(ImmediateTags.SYMBOL_I32) { serializeWasmSymbolReadOnly(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.TableIdx -> withTag(ImmediateTags.TABLE_INDEX) { serializeWasmSymbolReadOnly(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.TagIdx -> withTag(ImmediateTags.TAG_INDEX) { serializeWasmSymbolReadOnly(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.TypeIdx -> withTag(ImmediateTags.TYPE_INDEX) { serializeWasmSymbolReadOnly(i.value) { serializeWasmTypeDeclaration(it) } }
            is WasmImmediate.ValTypeVector -> withTag(ImmediateTags.VALUE_TYPE_VECTOR) { serializeList(i.value, ::serializeWasmType) }
        }

    private fun serializeCatchImmediate(catch: WasmImmediate.Catch) {
        val type = when (catch.type) {
            WasmImmediate.Catch.CatchType.CATCH -> ImmediateCatchTags.CATCH
            WasmImmediate.Catch.CatchType.CATCH_REF -> ImmediateCatchTags.CATCH_REF
            WasmImmediate.Catch.CatchType.CATCH_ALL -> ImmediateCatchTags.CATCH_ALL
            WasmImmediate.Catch.CatchType.CATCH_ALL_REF -> ImmediateCatchTags.CATCH_ALL_REF
        }
        withTag(type) {
            serializeList(catch.immediates, ::serializeWasmImmediate)
        }
    }

    private fun serializeWasmTable(table: WasmTable) {
        val max = table.limits.maxSize
        val ip = table.importPair
        serializeNamedModuleField(table, listOf(max == null, ip == null)) {
            b.writeUInt32(table.limits.minSize)
            max?.let { b.writeUInt32(it) }
            serializeWasmType(table.elementType)
            ip?.let { serializeWasmImportDescriptor(it) }
        }
    }

    private fun serializeWasmTableValue(value: WasmTable.Value): Unit =
        when (value) {
            is WasmTable.Value.Expression -> withTag(TableValueTags.EXPRESSION) { serializeList(value.expr, ::serializeWasmInstr) }
            is WasmTable.Value.Function -> withTag(TableValueTags.FUNCTION) { serializeWasmSymbolReadOnly(value.function) { serializeWasmFunction(it) } }
        }

    private fun serializeWasmElement(element: WasmElement): Unit =
        serializeNamedModuleField(element) {
            serializeWasmType(element.type)
            serializeList(element.values, ::serializeWasmTableValue)
            serializeWasmElementMode(element.mode)
        }

    private fun serializeWasmElementMode(mode: WasmElement.Mode) =
        when (mode) {
            is WasmElement.Mode.Active -> withTag(ElementModeTags.ACTIVE) {
                serializeWasmTable(mode.table)
                serializeList(mode.offset, ::serializeWasmInstr)
            }
            WasmElement.Mode.Declarative -> setTag(ElementModeTags.DECLARATIVE)
            WasmElement.Mode.Passive -> setTag(ElementModeTags.PASSIVE)
        }

    private fun serializeWasmExport(export: WasmExport<*>) {
        // The name is serialized before the tag.
        serializeString(export.name)
        when (export) {
            is WasmExport.Function -> withTag(ExportTags.FUNCTION) { serializeWasmFunction(export.field) }
            is WasmExport.Table -> withTag(ExportTags.TABLE) { serializeWasmTable(export.field) }
            is WasmExport.Memory -> withTag(ExportTags.MEMORY) { serializeWasmMemory(export.field) }
            is WasmExport.Global -> withTag(ExportTags.GLOBAL) { serializeWasmGlobal(export.field) }
            is WasmExport.Tag -> withTag(ExportTags.TAG) { serializeWasmTag(export.field) }
        }
    }

    private fun serializeWasmLimits(limit: WasmLimits) =
        withFlags(limit.maxSize == null) {
            b.writeUInt32(limit.minSize)
            limit.maxSize?.let { b.writeUInt32(it) }
        }

    private fun serializeWasmImportDescriptor(descriptor: WasmImportDescriptor) {
        serializeString(descriptor.moduleName)
        serializeWasmSymbolReadOnly(descriptor.declarationName, ::serializeString)
    }

    private fun <A, B> serializePair(pair: Pair<A, B>, serializeAFunc: (A) -> Unit, serializeBFunc: (B) -> Unit) {
        serializeAFunc(pair.first)
        serializeBFunc(pair.second)
    }

    private fun <T> serializeList(list: List<T>, serializeFunc: (T) -> Unit) {
        b.writeUInt32(list.size.toUInt())
        list.forEach { serializeFunc(it) }
    }

    private fun <T> serializeSet(set: Set<T>, serializeFunc: (T) -> Unit) {
        b.writeUInt32(set.size.toUInt())
        set.forEach { serializeFunc(it) }
    }

    private fun <K, V> serializeMap(map: Map<K, V>, serializeKeyFunc: (K) -> Unit, serializeValueFunc: (V) -> Unit) {
        b.writeUInt32(map.size.toUInt())
        map.forEach { (key, value) ->
            serializeKeyFunc(key)
            serializeValueFunc(value)
        }
    }

    private fun serializeSourceLocation(sl: SourceLocation) =
        when (sl) {
            SourceLocation.NoLocation -> setTag(LocationTags.NO_LOCATION)
            is SourceLocation.Location -> withTag(LocationTags.LOCATION) {
                serializeString(sl.module)
                serializeString(sl.file)
                b.writeUInt32(sl.line.toUInt())
                b.writeUInt32(sl.column.toUInt())
            }
            is SourceLocation.IgnoredLocation -> withTag(LocationTags.IGNORED_LOCATION) {
                serializeString(sl.module)
                serializeString(sl.file)
                b.writeUInt32(sl.line.toUInt())
                b.writeUInt32(sl.column.toUInt())
            }
        }

    private fun <T> serializeNullable(value: T?, serializeFunc: (T) -> Unit) {
        if (value != null) {
            withTag(NullableTags.NOT_NULL) { serializeFunc(value) }
        } else {
            setTag(NullableTags.NULL)
        }
    }

    private fun serializeIdSignature(idSignature: IdSignature) =
        when (idSignature) {
            is IdSignature.AccessorSignature -> withTag(IdSignatureTags.ACCESSOR) { serializeAccessorSignature(idSignature) }
            is IdSignature.CommonSignature -> withTag(IdSignatureTags.COMMON) { serializeCommonSignature(idSignature) }
            is IdSignature.CompositeSignature -> withTag(IdSignatureTags.COMPOSITE) { serializeCompositeSignature(idSignature) }
            is IdSignature.FileLocalSignature -> withTag(IdSignatureTags.FILE_LOCAL) { serializeFileLocalSignature(idSignature) }
            is IdSignature.LocalSignature -> withTag(IdSignatureTags.LOCAL) { serializeLocalSignature(idSignature) }
            is IdSignature.LoweredDeclarationSignature -> withTag(IdSignatureTags.LOWERED_DECLARATION) { serializeLoweredDeclarationSignature(idSignature) }
            is IdSignature.ScopeLocalDeclaration -> withTag(IdSignatureTags.SCOPE_LOCAL_DECLARATION) { serializeScopeLocalDeclaration(idSignature) }
            is IdSignature.SpecialFakeOverrideSignature -> withTag(IdSignatureTags.SPECIAL_FAKE_OVERRIDE) { serializeSpecialFakeOverrideSignature(idSignature) }
            is IdSignature.FileSignature -> withTag(IdSignatureTags.FILE) { serializeString(idSignature.fileName) }
        }

    private fun serializeAccessorSignature(accessor: IdSignature.AccessorSignature) {
        with(accessor) {
            serializeIdSignature(propertySignature)
            serializeCommonSignature(accessorSignature)
        }
    }

    private fun serializeCommonSignature(common: IdSignature.CommonSignature) {
        with(common) {
            withFlags(id == null, description == null) {
                serializeString(packageFqName)
                serializeString(declarationFqName)
                id?.let { b.writeUInt64(it.toULong()) }
                b.writeUInt64(mask.toULong())
                description?.let { serializeString(it) }
            }
        }
    }

    private fun serializeCompositeSignature(composite: IdSignature.CompositeSignature) {
        with(composite) {
            serializeIdSignature(container)
            serializeIdSignature(inner)
        }
    }

    private fun serializeFileLocalSignature(fileLocal: IdSignature.FileLocalSignature) {
        with(fileLocal) {
            withFlags(description == null) {
                serializeIdSignature(container)
                b.writeUInt64(id.toULong())
                description?.let { serializeString(it) }
            }
        }
    }

    private fun serializeLocalSignature(local: IdSignature.LocalSignature) {
        with(local) {
            withFlags(hashSig == null, description == null) {
                serializeString(localFqn)
                hashSig?.let { b.writeUInt64(it.toULong()) }
                description?.let { serializeString(it) }
            }
        }
    }

    private fun serializeLoweredDeclarationSignature(loweredDeclaration: IdSignature.LoweredDeclarationSignature) {
        with(loweredDeclaration) {
            serializeIdSignature(original)
            b.writeUInt32(stage.toUInt())
            b.writeUInt32(index.toUInt())
        }
    }

    private fun serializeScopeLocalDeclaration(scopeLocal: IdSignature.ScopeLocalDeclaration) {
        with(scopeLocal) {
            withFlags(description == null) {
                b.writeUInt32(id.toUInt())
                description?.let { serializeString(it) }
            }
        }
    }

    private fun serializeSpecialFakeOverrideSignature(specialFakeOverride: IdSignature.SpecialFakeOverrideSignature) {
        with(specialFakeOverride) {
            serializeIdSignature(memberSignature)
            serializeList(overriddenSignatures, ::serializeIdSignature)
        }
    }

    private fun serializeConstantDataElement(constantDataElement: ConstantDataElement) {
        when (constantDataElement) {
            is ConstantDataCharArray -> withTag(ConstantDataElementTags.CHAR_ARRAY) { serializeConstantDataCharArray(constantDataElement) }
            is ConstantDataCharField -> withTag(ConstantDataElementTags.CHAR_FIELD) { serializeConstantDataCharField(constantDataElement) }
            is ConstantDataIntArray -> withTag(ConstantDataElementTags.INT_ARRAY) { serializeConstantDataIntArray(constantDataElement) }
            is ConstantDataIntField -> withTag(ConstantDataElementTags.INT_FIELD) { serializeConstantDataIntField(constantDataElement) }
            is ConstantDataIntegerArray -> withTag(ConstantDataElementTags.INTEGER_ARRAY) { serializeConstantDataIntegerArray(constantDataElement) }
            is ConstantDataStruct -> withTag(ConstantDataElementTags.STRUCT) { serializeConstantDataStruct(constantDataElement) }
        }
    }

    private fun serializeConstantDataCharArray(constantDataCharArray: ConstantDataCharArray) {
        serializeString(constantDataCharArray.name)
        serializeList(constantDataCharArray.value) { serializeWasmSymbolReadOnly(it) { b.writeUInt32(it.code.toUInt()) } }
    }

    private fun serializeConstantDataCharField(constantDataCharField: ConstantDataCharField) {
        serializeString(constantDataCharField.name)
        serializeWasmSymbolReadOnly(constantDataCharField.value) { b.writeUInt32(it.code.toUInt()) }
    }

    private fun serializeConstantDataIntArray(constantDataIntArray: ConstantDataIntArray) {
        serializeString(constantDataIntArray.name)
        serializeList(constantDataIntArray.value) { serializeWasmSymbolReadOnly(it) { b.writeUInt32(it.toUInt()) } }
    }

    private fun serializeConstantDataIntField(constantDataIntField: ConstantDataIntField) {
        serializeString(constantDataIntField.name)
        serializeWasmSymbolReadOnly(constantDataIntField.value) { b.writeUInt32(it.toUInt()) }
    }

    private fun serializeConstantDataIntegerArray(constantDataIntegerArray: ConstantDataIntegerArray) {
        serializeString(constantDataIntegerArray.name)
        serializeList(constantDataIntegerArray.value) { b.writeUInt64(it.toULong()) }
        b.writeUInt32(constantDataIntegerArray.integerSize.toUInt())
    }

    private fun serializeConstantDataStruct(constantDataStruct: ConstantDataStruct) {
        serializeString(constantDataStruct.name)
        serializeList(constantDataStruct.elements, ::serializeConstantDataElement)
    }

    private fun serializeJsCodeSnippet(jsCodeSnippet: WasmCompiledModuleFragment.JsCodeSnippet) {
        serializeWasmSymbolReadOnly(jsCodeSnippet.importName, ::serializeString)
        serializeString(jsCodeSnippet.jsCode)
    }


    private fun serializeByteArray(bytes: ByteArray) {
        b.writeUInt32(bytes.size.toUInt())
        b.writeBytes(bytes)
    }

    private fun serializeString(str: String) {
        serializeAsReference(str) {
            val chars = str.toCharArray()
            if (chars.none { it.isSurrogate() }) {
                withFlags(true) {
                    serializeByteArray(str.toByteArray())
                }
            } else {
                val charsByteArray = ByteArray(chars.size * Char.SIZE_BYTES)
                var index = 0
                for (char in chars) {
                    val code = char.code
                    charsByteArray[index * Char.SIZE_BYTES] = (code and 0xFF).toByte()
                    charsByteArray[index * Char.SIZE_BYTES + 1] = (code ushr Byte.SIZE_BITS).toByte()
                    index++
                }
                withFlags(false) {
                    serializeByteArray(charsByteArray)
                }
            }
        }
    }

    private fun serializeInt(int: Int) {
        b.writeUInt32(int.toUInt())
    }

    private fun serializeLong(long: Long) {
        b.writeUInt64(long.toULong())
    }

    private fun <Ir, Wasm : Any> serializeReferencableElements(
        referencableElements: WasmCompiledModuleFragment.ReferencableElements<Ir, Wasm>,
        irSerializeFunc: (Ir) -> Unit,
        wasmSerializeFunc: (Wasm) -> Unit
    ) = serializeMap(referencableElements.unbound, irSerializeFunc) { serializeWasmSymbolReadOnly(it, wasmSerializeFunc) }

    private fun <Ir, Wasm : Any> serializeReferencableAndDefinable(
        referencableAndDefinable: WasmCompiledModuleFragment.ReferencableAndDefinable<Ir, Wasm>,
        irSerializeFunc: (Ir) -> Unit,
        wasmSerializeFunc: (Wasm) -> Unit
    ) = with(referencableAndDefinable) {
        serializeMap(unbound, irSerializeFunc) { serializeWasmSymbolReadOnly(it, wasmSerializeFunc) }
        serializeMap(defined, irSerializeFunc, wasmSerializeFunc)
        serializeList(elements, wasmSerializeFunc)
        serializeMap(wasmToIr, wasmSerializeFunc, irSerializeFunc)
    }

    private fun <T : Any> serializeWasmSymbolReadOnly(symbol: WasmSymbolReadOnly<T>, serializeFunc: (T) -> Unit) =
        serializeAsReference(symbol) {
            withFlags(symbol.getOwner() == null) {
                symbol.getOwner()?.let { serializeFunc(it) }
            }
        }

    private fun serializeCompiledFileFragment(compiledFileFragment: WasmCompiledFileFragment) =
        with(compiledFileFragment) {
            serializeNullable(fragmentTag, ::serializeString)
            serializeReferencableAndDefinable(functions, ::serializeIdSignature, ::serializeWasmFunction)
            serializeReferencableAndDefinable(globalFields, ::serializeIdSignature, ::serializeWasmGlobal)
            serializeReferencableAndDefinable(globalVTables, ::serializeIdSignature, ::serializeWasmGlobal)
            serializeReferencableAndDefinable(globalClassITables, ::serializeIdSignature, ::serializeWasmGlobal)
            serializeReferencableAndDefinable(functionTypes, ::serializeIdSignature, ::serializeWasmFunctionType)
            serializeReferencableAndDefinable(gcTypes, ::serializeIdSignature, ::serializeWasmTypeDeclaration)
            serializeReferencableAndDefinable(vTableGcTypes, ::serializeIdSignature, ::serializeWasmTypeDeclaration)
            serializeReferencableElements(classITableGcType, ::serializeIdSignature, ::serializeWasmTypeDeclaration)
            serializeReferencableElements(classITableInterfaceSlot, ::serializeIdSignature, ::serializeInt)
            serializeReferencableElements(classITableInterfaceTableSize, ::serializeIdSignature, ::serializeInt)
            serializeReferencableElements(classITableInterfaceHasImplementors, ::serializeIdSignature, ::serializeInt)
            serializeMap(typeInfo, ::serializeIdSignature, ::serializeConstantDataElement)
            serializeReferencableElements(classIds, ::serializeIdSignature, ::serializeInt)
            serializeReferencableElements(interfaceIds, ::serializeIdSignature, ::serializeInt)
            serializeReferencableElements(stringLiteralAddress, ::serializeString, ::serializeInt)
            serializeReferencableElements(stringLiteralPoolId, ::serializeString, ::serializeInt)
            serializeReferencableElements(constantArrayDataSegmentId, { serializePair(it, { serializeList(it, ::serializeLong) }, ::serializeWasmType)}, ::serializeInt)
            serializeList(interfaceUnions) { serializeList(it, ::serializeIdSignature) }
            serializeList(jsFuns, ::serializeJsCodeSnippet)
            serializeSet(jsModuleImports, ::serializeString)
            serializeList(exports, ::serializeWasmExport)
            serializeNullable(scratchMemAddr) { serializeWasmSymbolReadOnly(it, ::serializeInt) }
            serializeNullable(stringPoolSize) { serializeWasmSymbolReadOnly(it, ::serializeInt) }
            serializeNullable(throwableTagIndex) { serializeWasmSymbolReadOnly(it, ::serializeInt) }
            serializeNullable(jsExceptionTagIndex) { serializeWasmSymbolReadOnly(it, ::serializeInt) }
            serializeList(fieldInitializers, ::serializeFieldInitializer)
            serializeList(mainFunctionWrappers, ::serializeIdSignature)
            serializeNullable(testFun, ::serializeIdSignature)
            serializeList(closureCallExports) { serializePair(it, ::serializeString, ::serializeIdSignature) }
            serializeSet(jsModuleAndQualifierReferences, ::serializeJsModuleAndQualifierReference)
            serializeList(classAssociatedObjectsInstanceGetters, ::serializeClassAssociatedObjects)
            serializeNullable(tryGetAssociatedObjectFun, ::serializeIdSignature)
            serializeNullable(jsToKotlinAnyAdapterFun, ::serializeIdSignature)
        }

    private fun serializeFieldInitializer(fieldInitializer: FieldInitializer) {
        withFlags(fieldInitializer.isObjectInstanceField) {
            serializeIdSignature(fieldInitializer.field)
            serializeList(fieldInitializer.instructions, ::serializeWasmInstr)
        }
    }

    private fun serializeClassAssociatedObjects(classAssociatedObjects: ClassAssociatedObjects) {
        serializeIdSignature(classAssociatedObjects.klass)
        serializeList(classAssociatedObjects.objects, ::serializeAssociatedObject)
    }

    private fun serializeAssociatedObject(associatedObject: AssociatedObject) = withFlags(associatedObject.isExternal) {
        serializeIdSignature(associatedObject.obj)
        serializeIdSignature(associatedObject.getterFunc)
    }

    private fun serializeJsModuleAndQualifierReference(obj: JsModuleAndQualifierReference) {
        serializeNullable(obj.module, ::serializeString)
        serializeNullable(obj.qualifier, ::serializeString)
    }

    private fun serializeNamedModuleField(obj: WasmNamedModuleField, flags: List<Boolean> = listOf(), serializeFunc: () -> Unit) =
        serializeAsReference(obj) {
            // Serializes the common part of WasmNamedModuleField.
            withFlags(*listOf(obj.id == null, obj.name.isEmpty()).plus(flags).toBooleanArray()) {
                obj.id?.let { b.writeUInt32(it.toUInt()) }
                if (obj.name.isNotEmpty()) serializeString(obj.name)
                serializeFunc()
            }
        }

    private fun withTagNullable(tag: UInt, obj: Any?, serializeFunc: () -> Unit) {
        // Use the MSB of the tag as a flag
        val isNull = if (obj == null) 1U else 0U
        val newId = (tag or (isNull shl 7)).toUByte()
        b.writeUByte(newId)
        if (isNull != 1U) serializeFunc()
    }

    private fun setTag(tag: UInt) {
        b.writeUByte(tag.toUByte())
    }

    private fun withTag(tag: UInt, serializeFunc: () -> Unit) {
        b.writeUByte(tag.toUByte())
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
        val id = referenceTable.getOrPut(obj) {
            DeferredSerialization(referenceTable.size, serializeFunc).also {
                newReferences.add(it)
            }
        }.id
        serializeInt(id)
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