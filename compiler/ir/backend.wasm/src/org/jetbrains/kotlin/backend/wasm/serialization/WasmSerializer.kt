/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.serialization

import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.convertors.ByteWriter
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import java.io.OutputStream

class WasmSerializer(val outputStream: OutputStream) {

    /**
     * Each polymorphic type is prepended by a type id byte,
     *  so that we know the type of the object to construct
     *  on deserialization.
     *
     * All booleans and flags (whether a nullable field is
     *  null or not) are combined into a byte bitset. This
     *  bitset (if exists) is prepended to the struct. The
     *  interpretation of each bit of the bitset is up to
     *  each function implementation.
     * One exception is when there is only a single boolean
     *  field, in which case, it can be anywhere.
     *
     * See [WasmDeserializer]
     */

    private val b: ByteWriter = ByteWriter.OutputStream(outputStream)

    fun serialize(func: WasmFunction) {
        val type = func.type.getOwner()
        serializeNamedModuleField(func, listOf(type == null)) {
            type?.let { serialize(it) }
            when (func) {
                is WasmFunction.Defined -> withId(0U) {
                    serialize(func.locals)
                    serialize(func.instructions)
                }
                is WasmFunction.Imported -> withId(1U) {
                    serialize(func.importPair)
                }
            }
        }
    }

    fun serialize(global: WasmGlobal) =
        serializeNamedModuleField(global, listOf(global.isMutable, global.importPair == null)) {
            serialize(global.type)
            serialize(global.init)
            global.importPair?.let { serialize(it) }
        }

    fun serialize(funcType: WasmFunctionType) =
        serializeNamedModuleField(funcType) {
            serialize(funcType.parameterTypes)
            serialize(funcType.resultTypes)
        }

    fun serialize(typeDecl: WasmTypeDeclaration) =
        when (typeDecl) {
            is WasmFunctionType -> withId(0U) { serialize(typeDecl) }
            is WasmStructDeclaration -> withId(1U) { serialize(typeDecl) }
            is WasmArrayDeclaration -> withId(2U) { serialize(typeDecl) }
        }

    fun serialize(structDecl: WasmStructDeclaration) {
        // TODO extract all the boolean fields
        //  of structDecl.fields into a big bitset
        val superType = structDecl.superType?.getOwner()
        serializeNamedModuleField(structDecl, listOf(superType == null, structDecl.isFinal)) {
            serialize(structDecl.fields)
            superType?.let { serialize(it) }
        }
    }

    fun serialize(arrDecl: WasmArrayDeclaration): Unit =
        serializeNamedModuleField(arrDecl, listOf(arrDecl.field.isMutable)) {
            serialize(arrDecl.field.name)
            serialize(arrDecl.field.type)
        }

    fun serialize(structFieldDecl: WasmStructFieldDeclaration) {
        serialize(structFieldDecl.name)
        serialize(structFieldDecl.type)
        b.writeByte(structFieldDecl.isMutable.toByte())
    }

    fun serialize(type: WasmType) =
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

    fun serialize(type: WasmHeapType) =
        when (type) {
            WasmHeapType.Simple.Any -> withId(0U) { }
            WasmHeapType.Simple.Eq -> withId(1U) { }
            WasmHeapType.Simple.Extern -> withId(2U) { }
            WasmHeapType.Simple.Func -> withId(3U) { }
            WasmHeapType.Simple.NoExtern -> withId(4U) { }
            WasmHeapType.Simple.None -> withId(5U) { }
            WasmHeapType.Simple.Struct -> withId(6U) { }
            is WasmHeapType.Type -> withId(7U) { serializeSymbol(type.type) { serialize(it) } }
        }

    fun serialize(local: WasmLocal) {
        b.writeUInt32(local.id.toUInt())
        serialize(local.name)
        serialize(local.type)
        b.writeByte(local.isParameter.toByte())
    }

    fun serialize(instr: WasmInstr) {
        var opcode = instr.operator.opcode
        if (opcode == WASM_OP_PSEUDO_OPCODE) {
            opcode = when (instr.operator) {
                WasmOp.PSEUDO_COMMENT_PREVIOUS_INSTR -> 0xFFFF - 0
                WasmOp.PSEUDO_COMMENT_GROUP_START -> 0xFFFF - 1
                WasmOp.PSEUDO_COMMENT_GROUP_END -> 0xFFFF - 2
                else -> error("Unknown pseudo-opcode: $instr")
            }
        }
        b.writeUInt16(opcode.toUShort())
        when (instr) {
            is WasmInstrWithLocation -> withId(0U) { serialize(instr.immediates); serialize(instr.location) }
            is WasmInstrWithoutLocation -> withId(1U) { serialize(instr.immediates); }
        }
    }

    fun serialize(i: WasmImmediate) =
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
            is WasmImmediate.DataIdx -> withId(9U) { serializeSymbol(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.ElemIdx -> withId(10U) { serialize(i.value) }
            is WasmImmediate.FuncIdx -> withId(11U) { serializeSymbol(i.value) { serialize(it) } }
            is WasmImmediate.GcType -> withId(12U) { serializeSymbol(i.value) { serialize(it) } }
            is WasmImmediate.GlobalIdx -> withId(13U) { serializeSymbol(i.value) { serialize(it) } }
            is WasmImmediate.HeapType -> withId(14U) { serialize(i.value) }
            is WasmImmediate.LabelIdx -> withId(15U) { b.writeUInt32(i.value.toUInt()) }
            is WasmImmediate.LabelIdxVector -> withId(16U) { serialize(i.value) }
            is WasmImmediate.LocalIdx -> withId(17U) { serializeSymbol(i.value) { serialize(it) } }
            is WasmImmediate.MemArg -> withId(18U) { b.writeUInt32(i.align); b.writeUInt32(i.offset) }
            is WasmImmediate.MemoryIdx -> withId(19U) { b.writeUInt32(i.value.toUInt()) }
            is WasmImmediate.StructFieldIdx -> withId(20U) { serializeSymbol(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.SymbolI32 -> withId(21U) { serializeSymbol(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.TableIdx -> withId(22U) { serializeSymbol(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.TagIdx -> withId(23U) { b.writeUInt32(i.value.toUInt()) }
            is WasmImmediate.TypeIdx -> withId(24U) { serializeSymbol(i.value) { serialize(it) } }
            is WasmImmediate.ValTypeVector -> withId(25U) { serialize(i.value) }
        }

    fun serialize(catch: WasmImmediate.Catch) {
        val type = when (catch.type) {
            WasmImmediate.Catch.CatchType.CATCH -> 0
            WasmImmediate.Catch.CatchType.CATCH_REF -> 1
            WasmImmediate.Catch.CatchType.CATCH_ALL -> 2
            WasmImmediate.Catch.CatchType.CATCH_ALL_REF -> 3
        }.toByte()
        b.writeByte(type)
        serialize(catch.immediates)
    }

    fun serialize(table: WasmTable) {
        val max = table.limits.maxSize
        val ip = table.importPair
        serializeNamedModuleField(table, listOf(max == null, ip == null)) {
            b.writeUInt32(table.limits.minSize)
            max?.let { b.writeUInt32(it) }
            serialize(table.elementType)
            ip?.let { serialize(it) }
        }
    }

    fun serialize(value: WasmTable.Value) =
        when (value) {
            is WasmTable.Value.Expression -> withId(0U) { serialize(value.expr) }
            is WasmTable.Value.Function -> withId(1U) { serializeSymbol(value.function) { serialize(it) } }
        }

    fun serialize(element: WasmElement) =
        serializeNamedModuleField(element) {
            serialize(element.type)
            serialize(element.values)
            serialize(element.mode)
        }

    fun serialize(mode: WasmElement.Mode) =
        when (mode) {
            is WasmElement.Mode.Active -> withId(0U) {
                serialize(mode.table)
                serialize(mode.offset)
            }
            WasmElement.Mode.Declarative -> withId(1U) { }
            WasmElement.Mode.Passive -> withId(2U) { }
        }

    fun serialize(descriptor: WasmImportDescriptor) {
        serialize(descriptor.moduleName)
        serialize(descriptor.declarationName)
    }

    @Suppress("UNCHECKED_CAST")
    fun serialize(list: List<Any>) {
        b.writeUInt32(list.size.toUInt())
        if (list.isEmpty()) return
        when (list.first()) {
            is WasmLocal -> (list as List<WasmLocal>).forEach { serialize(it) }
            is WasmInstr -> (list as List<WasmInstr>).forEach { serialize(it) }
            is WasmType -> (list as List<WasmType>).forEach { serialize(it) }
            is WasmStructFieldDeclaration -> (list as List<WasmStructFieldDeclaration>).forEach { serialize(it) }
            is WasmImmediate -> (list as List<WasmImmediate>).forEach { serialize(it) }
            is WasmTable.Value -> (list as List<WasmTable.Value>).forEach { serialize(it) }
            is Int -> (list as List<Int>).forEach { b.writeUInt32(it.toUInt()) }
            else -> error("Unsupported element type: ${list.first()::class}")
        }
    }

    fun serialize(sl: SourceLocation) =
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

    fun serialize(str: String) {
        val bytes = str.toByteArray()
        b.writeUInt32(bytes.size.toUInt())
        b.writeBytes(bytes)
    }

    private fun <T : Any> serializeSymbol(value: WasmSymbolReadOnly<T>, serializeFunc: (T) -> Unit) =
        withFlags(listOf(value.getOwner() == null)) {
            value.getOwner()?.let { serializeFunc(it) }
        }

    private fun serializeNamedModuleField(obj: WasmNamedModuleField, flags: List<Boolean> = listOf(), serializeFunc: () -> Unit) =
        // Serializes the common part of WasmNamedModuleField.
        withFlags(listOf(obj.id == null, obj.name.isEmpty()).plus(flags)) {
            obj.id?.let { b.writeUInt32(it.toUInt()) }
            if (obj.name.isNotEmpty()) serialize(obj.name)
            serializeFunc()
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

    private fun withFlags(flags: List<Boolean>, serializeFunc: () -> Unit) {
        if (flags.size > 8) {
            error("Can't pack more than 8 flags in a single byte")
        }
        b.writeUByte(flagsToUByte(flags))
        serializeFunc()
    }

    private fun <T : Any> WasmSymbolReadOnly<T>.getOwner() =
        when (this) {
            is WasmSymbol<T> -> if (isBound()) owner else null
            else -> error("Unsupported symbol type: ${this::class}")
        }

    private fun flagsToUByte(flags: List<Boolean>): UByte {
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