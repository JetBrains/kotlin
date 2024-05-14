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
                    serialize(func.locals, ::serialize)
                    serialize(func.instructions, ::serialize)
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
            serialize(global.init, ::serialize)
            global.importPair?.let { serialize(it) }
        }

    fun serialize(funcType: WasmFunctionType) =
        serializeNamedModuleField(funcType) {
            serialize(funcType.parameterTypes, ::serialize)
            serialize(funcType.resultTypes, ::serialize)
        }

    fun serialize(typeDecl: WasmTypeDeclaration): Unit =
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
            serialize(structDecl.fields, ::serialize)
            superType?.let { serialize(it) }
        }
    }

    fun serialize(arrDecl: WasmArrayDeclaration): Unit =
        serializeNamedModuleField(arrDecl, listOf(arrDecl.field.isMutable)) {
            serialize(arrDecl.field.name)
            serialize(arrDecl.field.type)
        }

    fun serialize(memory: WasmMemory) =
        serializeNamedModuleField(memory, listOf(memory.importPair == null)) {
            serialize(memory.limits)
            memory.importPair?.let { serialize(it) }
        }

    fun serialize(tag: WasmTag): Unit =
        serializeNamedModuleField(tag, listOf(tag.importPair == null)) {
            serialize(tag.type)
            tag.importPair?.let { serialize(it) }
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
            is WasmHeapType.Type -> withId(7U) { serialize(type.type) { serialize(it) } }
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

    fun serialize(i: WasmImmediate): Unit =
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
            is WasmImmediate.TagIdx -> withId(23U) { serialize(i.value) { b.writeUInt32(it.toUInt()) } }
            is WasmImmediate.TypeIdx -> withId(24U) { serialize(i.value) { serialize(it) } }
            is WasmImmediate.ValTypeVector -> withId(25U) { serialize(i.value, ::serialize) }
        }

    fun serialize(catch: WasmImmediate.Catch) {
        val type = when (catch.type) {
            WasmImmediate.Catch.CatchType.CATCH -> 0
            WasmImmediate.Catch.CatchType.CATCH_REF -> 1
            WasmImmediate.Catch.CatchType.CATCH_ALL -> 2
            WasmImmediate.Catch.CatchType.CATCH_ALL_REF -> 3
        }.toByte()
        b.writeByte(type)
        serialize(catch.immediates, ::serialize)
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
            is WasmTable.Value.Expression -> withId(0U) { serialize(value.expr, ::serialize) }
            is WasmTable.Value.Function -> withId(1U) { serialize(value.function) { serialize(it) } }
        }

    fun serialize(element: WasmElement): Unit =
        serializeNamedModuleField(element) {
            serialize(element.type)
            serialize(element.values, ::serialize)
            serialize(element.mode)
        }

    fun serialize(mode: WasmElement.Mode) =
        when (mode) {
            is WasmElement.Mode.Active -> withId(0U) {
                serialize(mode.table)
                serialize(mode.offset, ::serialize)
            }
            WasmElement.Mode.Declarative -> withId(1U) { }
            WasmElement.Mode.Passive -> withId(2U) { }
        }

    fun serialize(export: WasmExport<*>) {
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

    fun serialize(limit: WasmLimits) =
        withFlags(limit.maxSize == null) {
            b.writeUInt32(limit.minSize)
            limit.maxSize?.let { b.writeUInt32(it) }
        }

    fun serialize(descriptor: WasmImportDescriptor) {
        serialize(descriptor.moduleName)
        serialize(descriptor.declarationName, ::serialize)
    }

    fun <A, B> serialize(pair: Pair<A, B>, serializeAFunc: (A) -> Unit, serializeBFunc: (B) -> Unit) {
        serializeAFunc(pair.first)
        serializeBFunc(pair.second)
    }

    fun <T> serialize(list: List<T>, serializeFunc: (T) -> Unit) {
        b.writeUInt32(list.size.toUInt())
        list.forEach { serializeFunc(it) }
    }

    fun <T> serialize(set: Set<T>, serializeFunc: (T) -> Unit) {
        b.writeUInt32(set.size.toUInt())
        set.forEach { serializeFunc(it) }
    }

    fun <K, V> serialize(map: Map<K, V>, serializeKeyFunc: (K) -> Unit, serializeValueFunc: (V) -> Unit) {
        b.writeUInt32(map.size.toUInt())
        map.forEach { (key, value) ->
            serializeKeyFunc(key)
            serializeValueFunc(value)
        }
    }

    fun serialize(sl: SourceLocation) =
        when (sl) {
            SourceLocation.NoLocation -> withId(0U) { }
            is SourceLocation.Location -> withId(1U) {
                serialize(sl.module)
                serialize(sl.file)
                b.writeUInt32(sl.line.toUInt())
                b.writeUInt32(sl.column.toUInt())
            }
            is SourceLocation.IgnoredLocation -> withId(1U) {
                serialize(sl.module)
                serialize(sl.file)
                b.writeUInt32(sl.line.toUInt())
                b.writeUInt32(sl.column.toUInt())
            }
        }

    fun serialize(idSignature: IdSignature) {
        when (idSignature) {
            is IdSignature.AccessorSignature -> withId(0U) { serialize(idSignature) }
            is IdSignature.CommonSignature -> withId(1U) { serialize(idSignature) }
            is IdSignature.CompositeSignature -> withId(2U) { serialize(idSignature) }
            is IdSignature.FileLocalSignature -> withId(3U) { serialize(idSignature) }
            is IdSignature.LocalSignature -> withId(4U) { serialize(idSignature) }
            is IdSignature.LoweredDeclarationSignature -> withId(5U) { serialize(idSignature) }
            is IdSignature.ScopeLocalDeclaration -> withId(6U) { serialize(idSignature) }
            is IdSignature.SpecialFakeOverrideSignature -> withId(7U) { serialize(idSignature) }
            is IdSignature.FileSignature -> TODO()
        }
    }

    fun serialize(accessor: IdSignature.AccessorSignature) {
        with(accessor) {
            serialize(propertySignature)
            serialize(accessorSignature)
        }
    }

    fun serialize(common: IdSignature.CommonSignature) {
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

    fun serialize(composite: IdSignature.CompositeSignature) {
        with(composite) {
            serialize(container)
            serialize(inner)
        }
    }

    fun serialize(fileLocal: IdSignature.FileLocalSignature) {
        with(fileLocal) {
            withFlags(description == null) {
                serialize(container)
                b.writeUInt64(id.toULong())
                description?.let { serialize(it) }
            }
        }
    }

    fun serialize(local: IdSignature.LocalSignature) {
        with(local) {
            withFlags(hashSig == null, description == null) {
                serialize(localFqn)
                hashSig?.let { b.writeUInt64(it.toULong()) }
                description?.let { serialize(it) }
            }
        }
    }

    fun serialize(loweredDeclaration: IdSignature.LoweredDeclarationSignature) {
        with(loweredDeclaration) {
            serialize(original)
            b.writeUInt32(stage.toUInt())
            b.writeUInt32(index.toUInt())
        }
    }

    fun serialize(scopeLocal: IdSignature.ScopeLocalDeclaration) {
        with(scopeLocal) {
            withFlags(description == null) {
                b.writeUInt32(id.toUInt())
                description?.let { serialize(it) }
            }
        }
    }

    fun serialize(specialFakeOverride: IdSignature.SpecialFakeOverrideSignature) {
        with(specialFakeOverride) {
            serialize(memberSignature)
            serialize(overriddenSignatures, ::serialize)
        }
    }

    fun serialize(constantDataElement: ConstantDataElement) {
        when (constantDataElement) {
            is ConstantDataCharArray -> withId(0U) { serialize(constantDataElement) }
            is ConstantDataCharField -> withId(1U) { serialize(constantDataElement) }
            is ConstantDataIntArray -> withId(2U) { serialize(constantDataElement) }
            is ConstantDataIntField -> withId(3U) { serialize(constantDataElement) }
            is ConstantDataIntegerArray -> withId(4U) { serialize(constantDataElement) }
            is ConstantDataStruct -> withId(5U) { serialize(constantDataElement) }
        }
    }

    fun serialize(constantDataCharArray: ConstantDataCharArray) {
        serialize(constantDataCharArray.name)
        serialize(constantDataCharArray.value) { serialize(it) { b.writeUInt32(it.code.toUInt()) } }
    }

    fun serialize(constantDataCharField: ConstantDataCharField) {
        serialize(constantDataCharField.name)
        serialize(constantDataCharField.value) { b.writeUInt32(it.code.toUInt()) }
    }

    fun serialize(constantDataIntArray: ConstantDataIntArray) {
        serialize(constantDataIntArray.name)
        serialize(constantDataIntArray.value) { serialize(it) { b.writeUInt32(it.toUInt()) } }
    }

    fun serialize(constantDataIntField: ConstantDataIntField) {
        serialize(constantDataIntField.name)
        serialize(constantDataIntField.value) { b.writeUInt32(it.toUInt()) }
    }

    fun serialize(constantDataIntegerArray: ConstantDataIntegerArray) {
        serialize(constantDataIntegerArray.name)
        serialize(constantDataIntegerArray.value) { b.writeUInt64(it.toULong()) }
        b.writeUInt32(constantDataIntegerArray.integerSize.toUInt())
    }

    fun serialize(constantDataStruct: ConstantDataStruct) {
        serialize(constantDataStruct.name)
        serialize(constantDataStruct.elements, ::serialize)
    }

    fun serialize(jsCodeSnippet: WasmCompiledModuleFragment.JsCodeSnippet) {
        serialize(jsCodeSnippet.importName, ::serialize)
        serialize(jsCodeSnippet.jsCode)
    }

    fun serialize(funWithPriority: WasmCompiledModuleFragment.FunWithPriority) {
        serialize(funWithPriority.function)
        serialize(funWithPriority.priority)
    }

    fun serialize(str: String) {
        val bytes = str.toByteArray()
        b.writeUInt32(bytes.size.toUInt())
        b.writeBytes(bytes)
    }

    fun serialize(int: Int) {
        b.writeUInt32(int.toUInt())
    }

    fun serialize(long: Long) {
        b.writeUInt64(long.toULong())
    }

    fun <Ir, Wasm : Any> serialize(
        referencableElements: WasmCompiledModuleFragment.ReferencableElements<Ir, Wasm>,
        irSerializeFunc: (Ir) -> Unit,
        wasmSerializeFunc: (Wasm) -> Unit
    ) = serialize(referencableElements.unbound, irSerializeFunc) { serialize(it, wasmSerializeFunc) }

    fun <Ir, Wasm : Any> serialize(
        referencableAndDefinable: WasmCompiledModuleFragment.ReferencableAndDefinable<Ir, Wasm>,
        irSerializeFunc: (Ir) -> Unit,
        wasmSerializeFunc: (Wasm) -> Unit
    ) = with(referencableAndDefinable) {
        serialize(unbound, irSerializeFunc) { serialize(it, wasmSerializeFunc) }
        serialize(defined, irSerializeFunc, wasmSerializeFunc)
        serialize(elements, wasmSerializeFunc)
        serialize(wasmToIr, wasmSerializeFunc, irSerializeFunc)
    }

    fun serialize(wasmCompiledFileFragment: WasmCompiledFileFragment) =
        with(wasmCompiledFileFragment) {
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

    fun <T : Any> serialize(value: WasmSymbolReadOnly<T>, serializeFunc: (T) -> Unit) =
        withFlags(value.getOwner() == null) {
            value.getOwner()?.let { serializeFunc(it) }
        }

    private fun serializeNamedModuleField(obj: WasmNamedModuleField, flags: List<Boolean> = listOf(), serializeFunc: () -> Unit) =
        // Serializes the common part of WasmNamedModuleField.
        withFlags(*listOf(obj.id == null, obj.name.isEmpty()).plus(flags).toBooleanArray()) {
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

    private fun withFlags(vararg flags: Boolean, serializeFunc: () -> Unit) {
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