/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.serialization

import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.utils.newLinkedHashMapWithExpectedSize
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.convertors.MyByteReader
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import java.io.InputStream

class WasmDeserializer(val inputStream: InputStream) {

    /** This class is the exact opposite of [WasmSerializer] */

    private val b: MyByteReader = MyByteReader(inputStream)

    companion object {
        val OPCODE_TO_WASM_OP by lazy { enumValues<WasmOp>().associateBy { it.opcode } }
    }

    fun deserializeFunction() =
        deserializeNamedModuleField { name, flags ->
            val type = deserializeSymbol(flags, ::deserializeFunctionType)
            withId { id ->
                when (id) {
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
                    else -> idError()
                }
            }
        }

    fun deserializeGlobal() =
        deserializeNamedModuleField { name, flags ->
            val type = deserializeType()
            val isMutable = flags.consume()
            val init = deserializeList(::deserializeInstr)
            val importPair = if (flags.consume()) null else deserializeImportDescriptor()
            WasmGlobal(name, type, isMutable, init, importPair)
        }

    fun deserializeFunctionType() =
        deserializeNamedModuleField { _ ->
            val parameterTypes = deserializeList(::deserializeType)
            val resultTypes = deserializeList(::deserializeType)
            WasmFunctionType(parameterTypes, resultTypes)
        }

    fun deserializeTypeDeclaration(): WasmTypeDeclaration =
        withId { id ->
            when (id) {
                0 -> deserializeFunctionType()
                1 -> deserializeStructDeclaration()
                2 -> deserializeArrayDeclaration()
                else -> idError()
            }
        }

    fun deserializeStructDeclaration(): WasmStructDeclaration =
        deserializeNamedModuleField { name, flags ->
            val fields = deserializeList(::deserializeStructFieldDeclaration)
            val superType = deserializeSymbol(flags, ::deserializeTypeDeclaration)
            val isFinal = flags.consume()
            WasmStructDeclaration(
                name,
                fields,
                superType,
                isFinal
            )
        }

    fun deserializeArrayDeclaration(): WasmArrayDeclaration =
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

    fun deserializeStructFieldDeclaration(): WasmStructFieldDeclaration {
        val name = deserializeString()
        val type = deserializeType()
        val isMutable = b.readUByte().toBoolean()
        return WasmStructFieldDeclaration(name, type, isMutable)
    }

    fun deserializeType() =
        withId { id ->
            when (id) {
                0 -> WasmRefType(deserializeHeapType())
                1 -> WasmRefNullType(deserializeHeapType())
                else -> WASM_TYPE_OBJECTS.getOrElse(id - 2) { idError() }
            }
        }

    fun deserializeHeapType() =
        withId { id ->
            when (id) {
                0 -> WasmHeapType.Simple.Any
                1 -> WasmHeapType.Simple.Eq
                2 -> WasmHeapType.Simple.Extern
                3 -> WasmHeapType.Simple.Func
                4 -> WasmHeapType.Simple.NoExtern
                5 -> WasmHeapType.Simple.None
                6 -> WasmHeapType.Simple.Struct
                7 -> WasmHeapType.Type(deserializeSymbol(::deserializeTypeDeclaration))
                else -> idError()
            }
        }

    fun deserializeLocal(): WasmLocal {
        val id = deserializeInt()
        val name = deserializeString()
        val type = deserializeType()
        val isParameter = b.readUByte().toBoolean()
        return WasmLocal(id, name, type, isParameter)
    }

    fun deserializeInstr(): WasmInstr {
        val opcode = b.readUInt16().toInt()
        return withId { id ->
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
            when (id) {
                0 -> WasmInstrWithLocation(op, immediates, deserializeSourceLocation())
                1 -> WasmInstrWithoutLocation(op, immediates)
                else -> idError()
            }
        }
    }

    fun deserializeImmediate(): WasmImmediate =
        withId { id ->
            when (id) {
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
                23 -> WasmImmediate.TagIdx(deserializeInt())
                24 -> WasmImmediate.TypeIdx(deserializeSymbol(::deserializeTypeDeclaration))
                25 -> WasmImmediate.ValTypeVector(deserializeList(::deserializeType))
                // This is the special case of BlockType.Value, which accepts a nullable WasmType. If is null, MSB is set to 1.
                129 -> WasmImmediate.BlockType.Value(null)
                else -> idError()
            }
        }

    fun deserializeImmediateCatch(): WasmImmediate.Catch {
        val type = when (b.readByte().toInt()) {
            0 -> WasmImmediate.Catch.CatchType.CATCH
            1 -> WasmImmediate.Catch.CatchType.CATCH_REF
            2 -> WasmImmediate.Catch.CatchType.CATCH_ALL
            3 -> WasmImmediate.Catch.CatchType.CATCH_ALL_REF
            else -> idError()
        }
        val immediates = deserializeList(::deserializeImmediate)
        return WasmImmediate.Catch(type, immediates)
    }

    fun deserializeTable() =
        deserializeNamedModuleField { _, flags ->
            val min = b.readUInt32()
            val max = if (flags.consume()) null else b.readUInt32()
            val elementType = deserializeType()
            val ip = deserializeImportDescriptor()
            WasmTable(WasmLimits(min, max), elementType, ip)
        }

    fun deserializeTableValue(): WasmTable.Value =
        withId { id ->
            when (id) {
                0 -> WasmTable.Value.Expression(deserializeList(::deserializeInstr))
                1 -> WasmTable.Value.Function(deserializeSymbol(::deserializeFunction))
                else -> idError()
            }
        }

    fun deserializeElement() =
        deserializeNamedModuleField { _ ->
            val type = deserializeType()
            val values = deserializeList(::deserializeTableValue)
            val mode = deserializeElementMode()
            WasmElement(type, values, mode)
        }

    fun deserializeElementMode(): WasmElement.Mode =
        withId { id ->
            when (id) {
                0 -> {
                    val table = deserializeTable()
                    val offset = deserializeList(::deserializeInstr)
                    WasmElement.Mode.Active(table, offset)
                }
                1 -> WasmElement.Mode.Declarative
                2 -> WasmElement.Mode.Passive
                else -> idError()
            }
        }

    fun deserializeImportDescriptor(): WasmImportDescriptor {
        val moduleName = deserializeString()
        val declarationName = deserializeSymbol(::deserializeString)
        return WasmImportDescriptor(moduleName, declarationName)
    }

    fun <T> deserializeList(itemDeserializeFunc: () -> T): MutableList<T> {
        val size = deserializeInt()
        val list = ArrayList<T>(size)
        for (i in 0 until size) {
            list.add(itemDeserializeFunc())
        }
        return list
    }

    fun <K, V> deserializeMap(deserializeKeyFunc: () -> K, deserializeValueFunc: () -> V): MutableMap<K, V> {
        val size = deserializeInt()
        val map = newLinkedHashMapWithExpectedSize<K, V>(size)
        repeat(size) {
            val key = deserializeKeyFunc()
            val value = deserializeValueFunc()
            map[key] = value
        }
        return map
    }

    fun <A, B> deserializePair(deserializeAFunc: () -> A, deserializeBFunc: () -> B): Pair<A, B> {
        val a = deserializeAFunc()
        val b = deserializeBFunc()
        return Pair(a, b)
    }

    fun deserializeSourceLocation() =
        withId { id ->
            when (id) {
                0 -> SourceLocation.NoLocation
                else -> {
                    val file = deserializeString()
                    val line = deserializeInt()
                    val column = deserializeInt()
                    when (id) {
                        1 -> SourceLocation.Location(file, line, column)
                        2 -> SourceLocation.IgnoredLocation(file, line, column)
                        else -> idError()
                    }
                }
            }
        }

    fun deserializeIdSignature(): IdSignature =
        withId { id ->
            when (id) {
                0 -> deserializeAccessorSignature()
                1 -> deserializeCommonSignature()
                2 -> deserializeCompositeSignature()
                3 -> deserializeFileLocalSignature()
                4 -> deserializeLocalSignature()
                5 -> deserializeLoweredDeclarationSignature()
                6 -> deserializeScopeLocalDeclaration()
                7 -> deserializeSpecialFakeOverrideSignature()
                else -> idError()
            }
        }

    fun deserializeAccessorSignature(): IdSignature.AccessorSignature {
        val propertySignature = deserializeIdSignature()
        val accessorSignature = deserializeCommonSignature()
        return IdSignature.AccessorSignature(propertySignature, accessorSignature)
    }

    fun deserializeCommonSignature(): IdSignature.CommonSignature =
        withFlags { flags ->
            val packageFqName = deserializeString()
            val declarationFqName = deserializeString()
            val id = if (flags.consume()) null else b.readUInt64().toLong()
            val mask = b.readUInt64().toLong()
            val description = if (flags.consume()) null else deserializeString()
            IdSignature.CommonSignature(packageFqName, declarationFqName, id, mask, description)
        }

    fun deserializeCompositeSignature(): IdSignature.CompositeSignature {
        val container = deserializeIdSignature()
        val inner = deserializeIdSignature()
        return IdSignature.CompositeSignature(container, inner)
    }

    fun deserializeFileLocalSignature(): IdSignature.FileLocalSignature =
        withFlags { flags ->
            val container = deserializeIdSignature()
            val id = b.readUInt64().toLong()
            val description = if (flags.consume()) null else deserializeString()
            IdSignature.FileLocalSignature(container, id, description)
        }

    fun deserializeLocalSignature(): IdSignature.LocalSignature =
        withFlags { flags ->
            val localFqn = deserializeString()
            val hashSig = if (flags.consume()) null else b.readUInt64().toLong()
            val description = if (flags.consume()) null else deserializeString()
            IdSignature.LocalSignature(localFqn, hashSig, description)
        }

    fun deserializeLoweredDeclarationSignature(): IdSignature.LoweredDeclarationSignature {
        val original = deserializeIdSignature()
        val stage = b.readUInt32().toInt()
        val index = b.readUInt32().toInt()
        return IdSignature.LoweredDeclarationSignature(original, stage, index)
    }

    fun deserializeScopeLocalDeclaration(): IdSignature.ScopeLocalDeclaration =
        withFlags { flags ->
            val id = b.readUInt32().toInt()
            val description = if (flags.consume()) null else deserializeString()
            IdSignature.ScopeLocalDeclaration(id, description)
        }

    fun deserializeSpecialFakeOverrideSignature(): IdSignature.SpecialFakeOverrideSignature {
        val memberSignature = deserializeIdSignature()
        val overriddenSignatures = deserializeList(::deserializeIdSignature)
        return IdSignature.SpecialFakeOverrideSignature(memberSignature, overriddenSignatures)
    }

    fun deserializeConstantDataElement(): ConstantDataElement =
        withId { id ->
            when (id) {
                0 -> deserializeConstantDataCharArray()
                1 -> deserializeConstantDataCharField()
                2 -> deserializeConstantDataIntArray()
                3 -> deserializeConstantDataIntField()
                4 -> deserializeConstantDataIntegerArray()
                5 -> deserializeConstantDataStruct()
                else -> idError()
            }
        }

    fun deserializeConstantDataCharArray(): ConstantDataCharArray {
        val name = deserializeString()
        val value = deserializeList { deserializeSymbol { Char(deserializeInt()) } }
        return ConstantDataCharArray(name, value)
    }

    fun deserializeConstantDataCharField(): ConstantDataCharField {
        val name = deserializeString()
        val value = deserializeSymbol { Char(deserializeInt()) }
        return ConstantDataCharField(name, value)
    }

    fun deserializeConstantDataIntArray(): ConstantDataIntArray {
        val name = deserializeString()
        val value = deserializeList { deserializeSymbol(::deserializeInt) }
        return ConstantDataIntArray(name, value)
    }

    fun deserializeConstantDataIntField(): ConstantDataIntField {
        val name = deserializeString()
        val value = deserializeSymbol(::deserializeInt)
        return ConstantDataIntField(name, value)
    }

    fun deserializeConstantDataIntegerArray(): ConstantDataIntegerArray {
        val name = deserializeString()
        val value = deserializeList { b.readUInt64().toLong() }
        val integerSize = deserializeInt()
        return ConstantDataIntegerArray(name, value, integerSize)
    }

    fun deserializeConstantDataStruct(): ConstantDataStruct {
        val name = deserializeString()
        val value = deserializeList(::deserializeConstantDataElement)
        return ConstantDataStruct(name, value)
    }

    fun deserializeJsCodeSnippet(): WasmCompiledModuleFragment.JsCodeSnippet {
        val importName = deserializeSymbol(::deserializeString)
        val jsCode = deserializeString()
        return WasmCompiledModuleFragment.JsCodeSnippet(importName, jsCode)
    }

    fun deserializeFunWithPriority(): WasmCompiledModuleFragment.FunWithPriority {
        val function = deserializeFunction()
        val priority = deserializeString()
        return WasmCompiledModuleFragment.FunWithPriority(function, priority)
    }

    fun deserializeString(): String {
        val length = b.readUInt32().toInt()
        val bytes = b.readBytes(length)
        return String(bytes)
    }

    private fun deserializeInt() = b.readUInt32().toInt()

    private fun <T : Any> deserializeSymbol(deserializeFunc: () -> T) =
        withFlags { flags -> deserializeSymbol(flags, deserializeFunc) }

    private fun <T : Any> deserializeSymbol(flags: Flags, deserializeFunc: () -> T): WasmSymbol<T> {
        val owner = if (flags.consume()) null else deserializeFunc()
        return WasmSymbol(owner)
    }

    private fun <T : WasmNamedModuleField> deserializeNamedModuleField(deserializeFunc: (String) -> T) =
        deserializeNamedModuleField { name, _ -> deserializeFunc(name) }

    private fun <T : WasmNamedModuleField> deserializeNamedModuleField(deserializeFunc: (String, Flags) -> T) =
        withFlags { flags ->
            // Deserializes the common part of WasmNamedModuleField.
            val id = if (flags.consume()) null else b.readUInt32().toInt()
            val name = if (flags.consume()) "" else deserializeString()
            deserializeFunc(name, flags).apply { this.id = id }
        }

    private fun <T> withId(deserializeFunc: (Int) -> T) =
        deserializeFunc(b.readUByte().toInt())

    private fun <T> withFlags(deserializeFunc: (Flags) -> T) =
        deserializeFunc(Flags(b.readUByte().toUInt()))

    private fun UByte.toBoolean(): Boolean = this == 1.toUByte()

    private fun idError(): Nothing = error("Invalid id")

    class Flags(private var flags: UInt = 0U) {
        operator fun get(i: Int): Boolean {
            if (i > 8) error("Flags structure can't have more than 8 flags")
            return (flags and (1U shl i)) != 0U
        }

        fun consume(): Boolean {
            val result = this[0]
            flags = (flags shr 1)
            return result
        }
    }
}