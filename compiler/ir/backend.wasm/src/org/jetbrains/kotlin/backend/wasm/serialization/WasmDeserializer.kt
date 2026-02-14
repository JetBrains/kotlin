/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.serialization

import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.backend.wasm.serialization.InstructionTags.LOCATED0
import org.jetbrains.kotlin.backend.wasm.serialization.InstructionTags.LOCATED1
import org.jetbrains.kotlin.backend.wasm.serialization.InstructionTags.LOCATED2
import org.jetbrains.kotlin.backend.wasm.serialization.InstructionTags.LOCATED3
import org.jetbrains.kotlin.backend.wasm.serialization.InstructionTags.LOCATED4
import org.jetbrains.kotlin.backend.wasm.serialization.InstructionTags.NOT_LOCATED0
import org.jetbrains.kotlin.backend.wasm.serialization.InstructionTags.NOT_LOCATED1
import org.jetbrains.kotlin.backend.wasm.serialization.InstructionTags.NOT_LOCATED2
import org.jetbrains.kotlin.backend.wasm.serialization.InstructionTags.NOT_LOCATED3
import org.jetbrains.kotlin.backend.wasm.serialization.InstructionTags.NOT_LOCATED4
import org.jetbrains.kotlin.backend.wasm.serialization.ReferenceTags.IN_PLACE
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.newLinkedHashMapWithExpectedSize
import org.jetbrains.kotlin.utils.newLinkedHashSetWithExpectedSize
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.convertors.MyByteReader
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import java.io.InputStream
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet
import kotlin.collections.MutableMap

/**
 * This class is the exact opposite of [WasmSerializer]. See [WasmSerializer] for details.
 *
 * When deserializing an object serialized with [WasmSerializer.withFlags], use [withFlags]
 * When deserializing an object serialized with [WasmSerializer.withTag], use [withTag]
 * When deserializing an object serialized with [WasmSerializer.serializeAsReference],
 *  use [deserializeReference]
 */

class WasmDeserializer(inputStream: InputStream, private val skipLocalNames: Boolean = false) {
    private val input: MyByteReader = MyByteReader(inputStream)
    private val deserializedReferences: MutableList<Any?> = mutableListOf()

    companion object {
        private val OPCODE_TO_WASM_OP by lazy { enumValues<WasmOp>().associateBy { it.opcode } }
    }

    fun deserializeModuleReferencedTypes(): ModuleReferencedTypes = ModuleReferencedTypes(
        gcTypes = deserializeSignatureSet(),
        functionTypes = deserializeSignatureSet(),
    )

    fun deserializeModuleReferencedDeclarations(): ModuleReferencedDeclarations = ModuleReferencedDeclarations(
        functions = deserializeSignatureSet(),
        globalVTable = deserializeSignatureSet(),
        globalClassITable = deserializeSignatureSet(),
        rttiGlobal = deserializeSignatureSet(),
    )

    private fun deserializeSignatureSet() =
        deserializeSet(::deserializeIdSignature)

    private fun deserializeFunction() =
        deserializeNamedModuleField { name ->
            val type = FunctionHeapTypeSymbol(deserializeIdSignature())
            withTag { tag ->
                when (tag) {
                    FunctionTags.DEFINED -> {
                        val locals = deserializeList(::deserializeLocal)
                        val instructions = deserializeList(::deserializeInstr)
                        val startLocation = deserializeSourceLocation()
                        val endLocation = deserializeSourceLocation()
                        WasmFunction.Defined(
                            name,
                            type,
                            locals,
                            instructions,
                            startLocation,
                            endLocation
                        )
                    }
                    FunctionTags.IMPORTED -> WasmFunction.Imported(
                        name,
                        type,
                        deserializeImportDescriptor(),
                    )
                    else -> tagError(tag)
                }
            }
        }

    private fun deserializeGlobal() =
        deserializeNamedModuleField { name, flags ->
            val type = deserializeType()
            val isMutable = flags.consume()
            val init = deserializeList(::deserializeInstr)
            val importPair = runIf(!flags.consume(), ::deserializeImportDescriptor)
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
                TypeDeclarationTags.FUNCTION -> deserializeFunctionType()
                TypeDeclarationTags.STRUCT -> deserializeStructDeclaration()
                TypeDeclarationTags.ARRAY -> deserializeArrayDeclaration()
                else -> tagError(tag)
            }
        }

    private fun deserializeStructDeclaration(): WasmStructDeclaration =

        deserializeNamedModuleField { name, flags ->
            val fields = deserializeList(::deserializeStructFieldDeclaration)
            val superType = if (flags.consume()) null else (deserializeHeapType() as WasmHeapType.Type)
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
            val type = FunctionHeapTypeSymbol(deserializeIdSignature())
            val importPair = if (flags.consume()) null else deserializeImportDescriptor()
            WasmTag(type, importPair)
        }

    private fun deserializeStructFieldDeclaration(): WasmStructFieldDeclaration {
        val name = deserializeString()
        val type = deserializeType()
        val isMutable = deserializeBoolean()
        return WasmStructFieldDeclaration(name, type, isMutable)
    }

    private fun deserializeType() =
        withTag { tag ->
            when (tag) {
                TypeTags.REF -> WasmRefType(deserializeHeapType())
                TypeTags.REF_NULL -> WasmRefNullType(deserializeHeapType())
                TypeTags.ANYREF -> WasmAnyRef
                TypeTags.EQREF -> WasmEqRef
                TypeTags.EXTERN_REF_TYPE -> WasmExnRefType
                TypeTags.EXTERN_REF -> WasmExternRef
                TypeTags.F32 -> WasmF32
                TypeTags.F64 -> WasmF64
                TypeTags.FUNC_REF -> WasmFuncRef
                TypeTags.I16 -> WasmI16
                TypeTags.I31_REF -> WasmI31Ref
                TypeTags.I32 -> WasmI32
                TypeTags.I64 -> WasmI64
                TypeTags.I8 -> WasmI8
                TypeTags.NULL_EXTERN_REF_TYPE -> WasmNullExnRefType
                TypeTags.REF_NULL_EXTERN_REF_TYPE -> WasmRefNullExternrefType
                TypeTags.REF_NULL_REF_TYPE -> WasmRefNullrefType
                TypeTags.STRUCT_REF -> WasmStructRef
                TypeTags.UNREACHABLE_TYPE -> WasmUnreachableType
                TypeTags.V12 -> WasmV128
                TypeTags.ARRAY_REF -> WasmArrayRef
                else -> tagError(tag)
            }
        }

    private fun deserializeHeapType() =
        withTag { tag ->
            when (tag) {
                HeapTypeTags.ANY -> WasmHeapType.Simple.Any
                HeapTypeTags.EQ -> WasmHeapType.Simple.Eq
                HeapTypeTags.EXTERN -> WasmHeapType.Simple.Extern
                HeapTypeTags.FUNC -> WasmHeapType.Simple.Func
                HeapTypeTags.NO_EXTERN -> WasmHeapType.Simple.NoExtern
                HeapTypeTags.NONE -> WasmHeapType.Simple.None
                HeapTypeTags.NO_FUNC -> WasmHeapType.Simple.NoFunc
                HeapTypeTags.STRUCT -> WasmHeapType.Simple.Struct
                HeapTypeTags.HEAP_GC_TYPE -> GcHeapTypeSymbol(deserializeIdSignature())
                HeapTypeTags.HEAP_VT_TYPE -> VTableHeapTypeSymbol(deserializeIdSignature())
                HeapTypeTags.HEAP_FUNC_TYPE -> FunctionHeapTypeSymbol(deserializeIdSignature())
                else -> tagError(tag)
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
        val isParameter = deserializeBoolean()
        return WasmLocal(id, name, type, isParameter)
    }

    private fun deserializeInstr(): WasmInstr {
        val opcode = input.readUInt16().toInt()
        return withTag { tag ->
            val op = when (opcode) {
                0xFFFF - 0 -> WasmOp.PSEUDO_COMMENT_PREVIOUS_INSTR
                0xFFFF - 1 -> WasmOp.PSEUDO_COMMENT_GROUP_START
                0xFFFF - 2 -> WasmOp.PSEUDO_COMMENT_GROUP_END
                else -> OPCODE_TO_WASM_OP.getOrElse(opcode) { error("Unknown opcode $opcode") }
            }

            when (tag) {
                LOCATED0 ->
                    wasmInstrWithLocation(op, deserializeSourceLocation())
                LOCATED1 ->
                    wasmInstrWithLocation(op, deserializeSourceLocation(), deserializeImmediate())
                LOCATED2 ->
                    wasmInstrWithLocation(op, deserializeSourceLocation(), deserializeImmediate(), deserializeImmediate())
                LOCATED3 ->
                    wasmInstrWithLocation(op, deserializeSourceLocation(), deserializeImmediate(), deserializeImmediate(), deserializeImmediate())
                LOCATED4 ->
                    wasmInstrWithLocation(op, deserializeSourceLocation(), deserializeImmediate(), deserializeImmediate(), deserializeImmediate(), deserializeImmediate())
                NOT_LOCATED0 ->
                    wasmInstrWithoutLocation(op)
                NOT_LOCATED1 ->
                    wasmInstrWithoutLocation(op, deserializeImmediate())
                NOT_LOCATED2 ->
                    wasmInstrWithoutLocation(op, deserializeImmediate(), deserializeImmediate())
                NOT_LOCATED3 ->
                    wasmInstrWithoutLocation(op, deserializeImmediate(), deserializeImmediate(), deserializeImmediate())
                NOT_LOCATED4 ->
                    wasmInstrWithoutLocation(op, deserializeImmediate(), deserializeImmediate(), deserializeImmediate(), deserializeImmediate())
                else -> tagError(tag)
            }
        }
    }

    private fun deserializeImmediate(): WasmImmediate =
        withTag { tag ->
            when (tag) {
                ImmediateTags.BLOCK_TYPE_FUNCTION -> WasmImmediate.BlockType.Function(deserializeSymbol(::deserializeFunctionType))
                ImmediateTags.BLOCK_TYPE_VALUE -> WasmImmediate.BlockType.Value(deserializeType())
                ImmediateTags.CATCH -> deserializeImmediateCatch()
                ImmediateTags.CONST_F32 -> WasmImmediate.ConstF32(input.readUInt32())
                ImmediateTags.CONST_F64 -> WasmImmediate.ConstF64(input.readUInt64())
                ImmediateTags.CONST_I32 -> WasmImmediate.ConstI32(deserializeInt())
                ImmediateTags.CONST_I64 -> WasmImmediate.ConstI64(deserializeLong())
                ImmediateTags.CONST_STRING -> WasmImmediate.ConstString(deserializeString())
                ImmediateTags.CONST_U8 -> WasmImmediate.ConstU8(input.readUByte())
                ImmediateTags.DATA_INDEX -> WasmImmediate.DataIdx(deserializeSymbol(::deserializeInt))
                ImmediateTags.ELEMENT_INDEX -> WasmImmediate.ElemIdx(deserializeElement())
                ImmediateTags.FUNC_INDEX -> FuncSymbol(deserializeIdSignature())

                ImmediateTags.GC_TYPE -> GcTypeSymbol(deserializeIdSignature())
                ImmediateTags.VT_TYPE -> VTableTypeSymbol(deserializeIdSignature())
                ImmediateTags.FUNC_TYPE -> FunctionTypeSymbol(deserializeIdSignature())

                ImmediateTags.GLOBAL_FIELD -> FieldGlobalSymbol(deserializeIdSignature())
                ImmediateTags.GLOBAL_VTABLE -> VTableGlobalSymbol(deserializeIdSignature())
                ImmediateTags.GLOBAL_CLASSITABLE -> ClassITableGlobalSymbol(deserializeIdSignature())
                ImmediateTags.GLOBAL_RTTI -> RttiGlobalSymbol(deserializeIdSignature())
                ImmediateTags.GLOBAL_STRING -> LiteralGlobalSymbol(deserializeString())

                ImmediateTags.HEAP_TYPE -> WasmImmediate.HeapType(deserializeHeapType())
                ImmediateTags.LABEL_INDEX -> WasmImmediate.LabelIdx.get(deserializeInt())
                ImmediateTags.LABEL_INDEX_VECTOR -> WasmImmediate.LabelIdxVector(deserializeList(::deserializeInt))
                ImmediateTags.LOCAL_INDEX -> WasmImmediate.LocalIdx.get(deserializeInt())
                ImmediateTags.MEM_ARG -> { val align = input.readUInt32(); val offset = input.readUInt32(); WasmImmediate.MemArg(align, offset) }
                ImmediateTags.MEMORY_INDEX -> WasmImmediate.MemoryIdx(deserializeInt())
                ImmediateTags.STRUCT_FIELD_INDEX -> WasmImmediate.StructFieldIdx.get(deserializeInt())
                ImmediateTags.SYMBOL_I32 -> WasmImmediate.SymbolI32(deserializeSymbol(::deserializeInt))
                ImmediateTags.TABLE_INDEX -> WasmImmediate.TableIdx(deserializeSymbol(::deserializeInt))
                ImmediateTags.TAG_INDEX -> WasmImmediate.TagIdx(deserializeSymbol(::deserializeInt))
                ImmediateTags.VALUE_TYPE_VECTOR -> WasmImmediate.ValTypeVector(deserializeList(::deserializeType))
                // This is the special case of BlockType.Value, which accepts a nullable WasmType. If is null, MSB is set to 1.
                ImmediateTags.BLOCK_TYPE_NULL_VALUE -> WasmImmediate.BlockType.Value(null)
                else -> tagError(tag)
            }
        }

    private fun deserializeImmediateCatch(): WasmImmediate.Catch {
        val type = withTag { tag ->
            when (tag) {
                ImmediateCatchTags.CATCH -> WasmImmediate.Catch.CatchType.CATCH
                ImmediateCatchTags.CATCH_REF -> WasmImmediate.Catch.CatchType.CATCH_REF
                ImmediateCatchTags.CATCH_ALL -> WasmImmediate.Catch.CatchType.CATCH_ALL
                ImmediateCatchTags.CATCH_ALL_REF -> WasmImmediate.Catch.CatchType.CATCH_ALL_REF
                else -> tagError(tag)
            }
        }
        val immediates = deserializeList(::deserializeImmediate)
        return WasmImmediate.Catch(type, immediates)
    }

    private fun deserializeTable() =
        deserializeNamedModuleField { _, flags ->
            val min = input.readUInt32()
            val max = if (flags.consume()) null else input.readUInt32()
            val elementType = deserializeType()
            val ip = deserializeImportDescriptor()
            WasmTable(WasmLimits(min, max), elementType, ip)
        }

    private fun deserializeTableValue(): WasmTable.Value =
        withTag { tag ->
            when (tag) {
                TableValueTags.EXPRESSION -> WasmTable.Value.Expression(deserializeList(::deserializeInstr))
                TableValueTags.FUNCTION -> WasmTable.Value.Function(deserializeSymbol(::deserializeFunction))
                else -> tagError(tag)
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
                ElementModeTags.ACTIVE -> {
                    val table = deserializeTable()
                    val offset = deserializeList(::deserializeInstr)
                    WasmElement.Mode.Active(table, offset)
                }
                ElementModeTags.DECLARATIVE -> WasmElement.Mode.Declarative
                ElementModeTags.PASSIVE -> WasmElement.Mode.Passive
                else -> tagError(tag)
            }
        }

    private fun deserializeExport(): WasmExport<*> {
        // The name is deserialized before the tag.
        val name = deserializeString()
        return withTag { tag ->
            when (tag) {
                ExportTags.FUNCTION -> WasmExport.Function(name, deserializeFunction())
                ExportTags.TABLE -> WasmExport.Table(name, deserializeTable())
                ExportTags.MEMORY -> WasmExport.Memory(name, deserializeMemory())
                ExportTags.GLOBAL -> WasmExport.Global(name, deserializeGlobal())
                ExportTags.TAG -> WasmExport.Tag(name, deserializeTag())
                else -> tagError(tag)
            }
        }
    }

    private fun deserializeLimits(): WasmLimits =
        withFlags { flags ->
            val minSize = input.readUInt32()
            val maxSize = if (flags.consume()) null else input.readUInt32()
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
                LocationTags.NO_LOCATION -> SourceLocation.NoLocation
                LocationTags.IGNORED_LOCATION -> SourceLocation.IgnoredLocation
                LocationTags.NEXT_LOCATION -> SourceLocation.NextLocation
                LocationTags.DEFINED_LOCATION -> SourceLocation.DefinedLocation(
                    file = deserializeString(),
                    line = deserializeInt(),
                    column = deserializeInt()
                )
                else -> tagError(tag)
            }
        }

    private inline fun <T> deserializeNullable(crossinline deserializeFunc: () -> T): T? = withTag {
        when (it) {
            NullableTags.NULL -> null
            NullableTags.NOT_NULL -> deserializeFunc()
            else -> tagError(it)
        }
    }

    private fun deserializeIdSignature(): IdSignature = deserializeReference {
        withTag { tag ->
            when (tag) {
                IdSignatureTags.ACCESSOR -> deserializeAccessorSignature()
                IdSignatureTags.COMMON -> deserializeCommonSignature()
                IdSignatureTags.COMPOSITE -> deserializeCompositeSignature()
                IdSignatureTags.FILE_LOCAL -> deserializeFileLocalSignature()
                IdSignatureTags.LOCAL -> deserializeLocalSignature()
                IdSignatureTags.LOWERED_DECLARATION -> deserializeLoweredDeclarationSignature()
                IdSignatureTags.SCOPE_LOCAL_DECLARATION -> deserializeScopeLocalDeclaration()
                IdSignatureTags.FILE -> deserializeString().let { IdSignature.FileSignature(it, FqName.ROOT, it) }
                else -> tagError(tag)
            }
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
            val id = if (flags.consume()) null else deserializeLong()
            val mask = deserializeLong()
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
            val id = deserializeLong()
            IdSignature.FileLocalSignature(container, id)
        }

    private fun deserializeLocalSignature(): IdSignature.LocalSignature =
        withFlags { flags ->
            val localFqn = deserializeString()
            val hashSig = if (flags.consume()) null else deserializeLong()
            IdSignature.LocalSignature(localFqn, hashSig)
        }

    private fun deserializeLoweredDeclarationSignature(): IdSignature.LoweredDeclarationSignature {
        val original = deserializeIdSignature()
        val stage = deserializeInt()
        val index = deserializeInt()
        return IdSignature.LoweredDeclarationSignature(original, stage, index)
    }

    private fun deserializeScopeLocalDeclaration(): IdSignature.ScopeLocalDeclaration =
        withFlags { flags ->
            val id = deserializeInt()
            IdSignature.ScopeLocalDeclaration(id)
        }

    private fun deserializeConstantDataElement(): ConstantDataElement =
        withTag { tag ->
            when (tag) {
                ConstantDataElementTags.CHAR_ARRAY -> deserializeConstantDataCharArray()
                ConstantDataElementTags.CHAR_FIELD -> deserializeConstantDataCharField()
                ConstantDataElementTags.INT_ARRAY -> deserializeConstantDataIntArray()
                ConstantDataElementTags.INT_FIELD -> deserializeConstantDataIntField()
                ConstantDataElementTags.INTEGER_ARRAY -> deserializeConstantDataIntegerArray()
                ConstantDataElementTags.STRUCT -> deserializeConstantDataStruct()
                else -> tagError(tag)
            }
        }

    private fun deserializeConstantDataCharArray(): ConstantDataCharArray {
        val value = deserializeList { deserializeSymbol { Char(deserializeInt()) } }
        val fitsLatin1 = deserializeBoolean()
        return ConstantDataCharArray(value, fitsLatin1)
    }

    private fun deserializeConstantDataCharField(): ConstantDataCharField {
        val value = deserializeSymbol { Char(deserializeInt()) }
        return ConstantDataCharField(value)
    }

    private fun deserializeConstantDataIntArray(): ConstantDataIntArray {
        val value = deserializeList { deserializeSymbol(::deserializeInt) }
        return ConstantDataIntArray(value)
    }

    private fun deserializeConstantDataIntField(): ConstantDataIntField {
        val value = deserializeSymbol(::deserializeInt)
        return ConstantDataIntField(value)
    }

    private fun deserializeConstantDataIntegerArray(): ConstantDataIntegerArray {
        val value = deserializeList { deserializeLong() }
        val integerSize = deserializeInt()
        return ConstantDataIntegerArray(value, integerSize)
    }

    private fun deserializeConstantDataStruct(): ConstantDataStruct {
        val value = deserializeList(::deserializeConstantDataElement)
        return ConstantDataStruct(value)
    }

    private fun deserializeJsCodeSnippet(): WasmCompiledModuleFragment.JsCodeSnippet {
        val importName = deserializeSymbol(::deserializeString)
        val jsCode = deserializeString()
        return WasmCompiledModuleFragment.JsCodeSnippet(importName, jsCode)
    }

    private fun deserializeString(): String {
        return deserializeReference {
            withFlags {
                if (it.consume()) {
                    val length = deserializeInt()
                    val bytes = input.readBytes(length)
                    String(bytes)
                } else {
                    val lengthBytes = deserializeInt()
                    val bytes = input.readBytes(lengthBytes)
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
    }

    private fun skipString() {
        skipInt()
    }

    private fun deserializeInt() = input.readUInt32().toInt()

    private fun deserializeBoolean() = input.readUByte().toBoolean()

    private fun skipInt() {
        input.skip(4)
    }

    private fun deserializeLong() = input.readUInt64().toLong()

    private inline fun <T : Any> deserializeSymbol(crossinline deserializeFunc: () -> T): WasmSymbol<T> =
        deserializeReference {
            withFlags { flags ->
                val owner = if (flags.consume()) null else deserializeFunc()
                WasmSymbol(owner)
            }
        }

    fun deserializeCompiledTypesFragment() = WasmCompiledTypesFileFragment(
        definedGcTypes = deserializeGcTypes(),
        definedVTableGcTypes = deserializeVTableGcTypes(),
        definedFunctionTypes = deserializeFunctionTypes(),
    )

    fun deserializeCompiledDeclarationsFragment() = WasmCompiledDeclarationsFileFragment(
        definedFunctions = deserializeDefinedFunctions(),
        definedGlobalFields = deserializeGlobalFields(),
        definedGlobalVTables = deserializeGlobalVTables(),
        definedGlobalClassITables = deserializeGlobalClassITables(),
        definedRttiGlobal = deserializeGlobalRtti(),
        definedRttiSuperType = deserializeRttiSupertype(),
    )

    fun deserializeCompiledServiceFragment() = WasmCompiledServiceFileFragment(
        globalLiterals = deserializeGlobalLiterals(),
        globalLiteralsId = deserializeStringLiteralId(),
        stringLiteralId = deserializeStringLiteralId(),
        constantArrayDataSegmentId = deserializeConstantArrayDataSegmentId(),
        jsFuns = deserializeJsFuns(),
        jsModuleImports = deserializeJsModuleImports(),
        jsBuiltinsPolyfills = deserializeJsBuiltinsPolyfills(),
        exports = deserializeExports(),
        mainFunctionWrappers = deserializeMainFunctionWrappers(),
        testFunctionDeclarators = deserializeTestFunctionDeclarators(),
        equivalentFunctions = deserializeClosureCallExports(),
        jsModuleAndQualifierReferences = deserializeJsModuleAndQualifierReferences(),
        classAssociatedObjectsInstanceGetters = deserializeClassAssociatedObjectInstanceGetters(),
        objectInstanceFieldInitializers = deserializeList(::deserializeIdSignature),
        nonConstantFieldInitializers = deserializeList(::deserializeIdSignature),
    )

    private fun deserializeDefinedFunctions() = deserializeMap(::deserializeIdSignature, ::deserializeFunction)

    private fun deserializeGlobalFields() = deserializeMap(::deserializeIdSignature, ::deserializeGlobal)
    private fun deserializeGlobalVTables() = deserializeMap(::deserializeIdSignature, ::deserializeGlobal)
    private fun deserializeGlobalClassITables() = deserializeMap(::deserializeIdSignature, ::deserializeGlobal)
    private fun deserializeGlobalRtti() = deserializeMap(::deserializeIdSignature, ::deserializeGlobal)

    private fun deserializeRttiSupertype() = deserializeMap(::deserializeIdSignature) {
        deserializeNullable(::deserializeIdSignature)
    }

    private fun deserializeGcTypes() = deserializeMap(::deserializeIdSignature, ::deserializeTypeDeclaration)
    private fun deserializeVTableGcTypes() = deserializeMap(::deserializeIdSignature, ::deserializeStructDeclaration)
    private fun deserializeFunctionTypes() = deserializeMap(::deserializeIdSignature, ::deserializeFunctionType)

    private fun deserializeGlobalLiterals(): MutableSet<LiteralGlobalSymbol> =
        deserializeSet { LiteralGlobalSymbol(deserializeString()) }

    private fun deserializeIntSymbol() = deserializeSymbol { deserializeInt() }
    private fun deserializeStringLiteralId() = deserializeMap(::deserializeString, ::deserializeIntSymbol)
    private fun deserializeConstantArrayDataSegmentId(): MutableMap<Pair<List<Long>, WasmType>, WasmSymbol<Int>> =
        deserializeMap(
            deserializeKeyFunc = {
                deserializePair(
                    deserializeAFunc = { deserializeList(::deserializeLong) },
                    deserializeBFunc = ::deserializeType
                )
            },
            deserializeValueFunc = ::deserializeIntSymbol
        )


    private fun deserializeJsFuns() = deserializeMap(::deserializeIdSignature, ::deserializeJsCodeSnippet)
    private fun deserializeJsModuleImports() = deserializeMap(::deserializeIdSignature, ::deserializeString)
    private fun deserializeJsBuiltinsPolyfills() = deserializeMap(::deserializeString, ::deserializeString)
    private fun deserializeExports() = deserializeList(::deserializeExport)
    private fun deserializeMainFunctionWrappers() = deserializeList(::deserializeIdSignature)
    private fun deserializeTestFunctionDeclarators() = deserializeList(::deserializeIdSignature)
    private fun deserializeClosureCallExports() = deserializeList { deserializePair(::deserializeString, ::deserializeIdSignature) }
    private fun deserializeJsModuleAndQualifierReferences() = deserializeSet(::deserializeJsModuleAndQualifierReference)
    private fun deserializeClassAssociatedObjectInstanceGetters() = deserializeList(::deserializeClassAssociatedObjects)

    private fun deserializeAssociatedObject(): AssociatedObject = withFlags {
        val obj = deserializeLong()
        val getterFunc = deserializeIdSignature()
        return AssociatedObject(obj, getterFunc, it.consume())
    }

    private fun deserializeClassAssociatedObjects(): ClassAssociatedObjects {
        val klass = deserializeLong()
        val objects = deserializeList(::deserializeAssociatedObject)
        return ClassAssociatedObjects(klass, objects)
    }

    private fun deserializeJsModuleAndQualifierReference(): JsModuleAndQualifierReference = JsModuleAndQualifierReference(
        module = deserializeNullable(::deserializeString),
        qualifier = deserializeNullable(::deserializeString)
    )

    private inline fun <reified T : WasmNamedModuleField> deserializeNamedModuleField(crossinline deserializeFunc: (String) -> T) =
        deserializeNamedModuleField { name, _ -> deserializeFunc(name) }

    private inline fun <reified T : WasmNamedModuleField> deserializeNamedModuleField(crossinline deserializeFunc: (String, Flags) -> T) =
        deserializeReference<T> {
            withFlags { flags ->
                // Deserializes the common part of WasmNamedModuleField.
                val id = if (flags.consume()) null else deserializeInt()
                val name = if (flags.consume()) "" else deserializeString()
                deserializeFunc(name, flags).apply { this.id = id }
            }
        }

    private inline fun <T> withTag(deserializeFunc: (UInt) -> T) =
        deserializeFunc(input.readUByte().toUInt())

    private inline fun <T> withFlags(deserializeFunc: (Flags) -> T) =
        deserializeFunc(Flags(input.readUByte().toUInt()))

    private inline fun <reified T> deserializeReference(crossinline deserializeFunc: () -> T): T {
        val index = deserializeInt()
        return if (index == IN_PLACE) {
            val currentIndex = deserializedReferences.size
            deserializedReferences.add(null)
            deserializeFunc().also {
                deserializedReferences[currentIndex] = it
            }
        } else {
            val obj = deserializedReferences[index]
            check(obj != null) { "Recursive deserialization" }
            deserializedReferences[index] as T
        }
    }

    private fun UByte.toBoolean(): Boolean = this == 1.toUByte()

    private fun tagError(tag: UInt): Nothing = error("Invalid tag: $tag")

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
}