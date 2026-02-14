/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.serialization

import org.jetbrains.kotlin.backend.wasm.ir2wasm.*
import org.jetbrains.kotlin.backend.wasm.serialization.ReferenceTags.IN_PLACE
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.wasm.ir.*
import org.jetbrains.kotlin.wasm.ir.ByteWriter
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
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
 * When serializing an object, which referenced multiple times, all deserialized references must reference on a same object instance.
 * To make this possible, an object will be stored only once as it firstly reached ("in-place").
 * The following serializations of same object will store only an index in reference-table.
 *
 * The references which are saved with [serializeAsReference] can have any object type, and not limited to a set of types.
 *
 * When deserializing a reference, it gets the "in-place" flag, or it's index. If it is "in-place" than
 * deserialize it by a provided deserializer and put an object into reference-table. The following deserialization of this very same instance
 * will read index and take the reference from the reference-table.
 *
 * See [WasmDeserializer]
 */

class WasmSerializer(outputStream: OutputStream) {
    private val body = ByteWriter(outputStream)
    private val serializedReferenceIndexes = IdentityHashMap<Any, Int>()

    fun serialize(referencedTypes: ModuleReferencedTypes) {
        with(referencedTypes) {
            serializeSet(gcTypes, ::serializeIdSignature)
            serializeSet(functionTypes, ::serializeIdSignature)
        }
    }

    fun serializeCompiledTypes(definedTypes: WasmCompiledTypesFileFragment) = with(definedTypes) {
        serializeDefinedTypeDeclarations(definedGcTypes)
        serializeDefinedStructDeclarations(definedVTableGcTypes)
        serializeDefinedFunctionTypesDeclarations(definedFunctionTypes)
    }

    fun serializeCompiledDeclarations(definedDeclarations: WasmCompiledDeclarationsFileFragment) = with(definedDeclarations) {
        serializeDefinedFunctions(definedFunctions)
        serializeDefinedGlobals(definedGlobalFields)
        serializeDefinedGlobals(definedGlobalVTables)
        serializeDefinedGlobals(definedGlobalClassITables)
        serializeDefinedGlobals(definedRttiGlobal)
        serializeMap(definedRttiSuperType, ::serializeIdSignature, ::serializeClassSuperType)
    }

    fun serializeCompiledService(serviceData: WasmCompiledServiceFileFragment) = with(serviceData) {
        serializeGlobalLiterals(globalLiterals)
        serializeMap(globalLiteralsId, ::serializeString, ::serializeIntSymbol)
        serializeMap(stringLiteralId, ::serializeString, ::serializeIntSymbol)
        serializeConstantArrayDataSegmentId(constantArrayDataSegmentId)
        serializeMap(jsFuns, ::serializeIdSignature, ::serializeJsCodeSnippet)
        serializeMap(jsModuleImports, ::serializeIdSignature, ::serializeString)
        serializeMap(jsBuiltinsPolyfills, ::serializeString, ::serializeString)
        serializeList(exports, ::serializeWasmExport)
        serializeList(mainFunctionWrappers, ::serializeIdSignature)
        serializeList(testFunctionDeclarators, ::serializeIdSignature)
        serializeList(equivalentFunctions) { serializePair(it, ::serializeString, ::serializeIdSignature) }
        serializeSet(jsModuleAndQualifierReferences, ::serializeJsModuleAndQualifierReference)
        serializeList(classAssociatedObjectsInstanceGetters, ::serializeClassAssociatedObjects)
        serializeList(objectInstanceFieldInitializers, ::serializeIdSignature)
        serializeList(nonConstantFieldInitializers, ::serializeIdSignature)
    }


    fun serialize(declarations: ModuleReferencedDeclarations) {
        serializeSet(declarations.functions, ::serializeIdSignature)
        serializeSet(declarations.globalVTable, ::serializeIdSignature)
        serializeSet(declarations.globalClassITable, ::serializeIdSignature)
        serializeSet(declarations.rttiGlobal, ::serializeIdSignature)
    }

    private fun serializeWasmFunction(func: WasmFunction) =
        serializeNamedModuleField(func) {
            serializeIdSignature((func.type as FunctionHeapTypeSymbol).type)
            when (func) {
                is WasmFunction.Defined -> withTag(FunctionTags.DEFINED) {
                    serializeList(func.locals, ::serializeWasmLocal)
                    serializeList(func.instructions, ::serializeWasmInstr)
                    serializeSourceLocation(func.startLocation)
                    serializeSourceLocation(func.endLocation)
                }
                is WasmFunction.Imported -> withTag(FunctionTags.IMPORTED) {
                    serializeWasmImportDescriptor(func.importPair)
                }
            }
        }

    private fun serializeClassSuperType(supertype: IdSignature?) {
        serializeNullable(supertype, ::serializeIdSignature)
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
            structDecl.superType?.let { serializeWasmHeapType(it) }
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
            serializeIdSignature((tag.type as FunctionHeapTypeSymbol).type)
            tag.importPair?.let { serializeWasmImportDescriptor(it) }
        }

    private fun serializeWasmStructFieldDeclaration(structFieldDecl: WasmStructFieldDeclaration) {
        serializeString(structFieldDecl.name)
        serializeWasmType(structFieldDecl.type)
        serializeBoolean(structFieldDecl.isMutable)
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
            WasmArrayRef -> setTag(TypeTags.ARRAY_REF)
        }

    private fun serializeWasmHeapType(type: WasmHeapType) =
        when (type) {
            WasmHeapType.Simple.Any -> setTag(HeapTypeTags.ANY)
            WasmHeapType.Simple.Eq -> setTag(HeapTypeTags.EQ)
            WasmHeapType.Simple.Extern -> setTag(HeapTypeTags.EXTERN)
            WasmHeapType.Simple.Func -> setTag(HeapTypeTags.FUNC)
            WasmHeapType.Simple.NoExtern -> setTag(HeapTypeTags.NO_EXTERN)
            WasmHeapType.Simple.None -> setTag(HeapTypeTags.NONE)
            WasmHeapType.Simple.NoFunc -> setTag(HeapTypeTags.NO_FUNC)
            WasmHeapType.Simple.Struct -> setTag(HeapTypeTags.STRUCT)
            is GcHeapTypeSymbol -> withTag(HeapTypeTags.HEAP_GC_TYPE) { serializeIdSignature(type.type) }
            is VTableHeapTypeSymbol -> withTag(HeapTypeTags.HEAP_VT_TYPE) { serializeIdSignature(type.type) }
            is FunctionHeapTypeSymbol -> withTag(HeapTypeTags.HEAP_FUNC_TYPE) { serializeIdSignature(type.type) }
            else -> error("Unknown heap type:${type::class.simpleName}")
        }

    private fun serializeWasmLocal(local: WasmLocal) {
        body.writeUInt32(local.id.toUInt())
        serializeString(local.name)
        serializeWasmType(local.type)
        serializeBoolean(local.isParameter)
    }

    private fun serializeWasmInstr(instr: WasmInstr) {
        var opcode = instr.operator.opcode
        if (opcode == WASM_OP_PSEUDO_OPCODE) {
            opcode = when (instr.operator) {
                WasmOp.PSEUDO_COMMENT_PREVIOUS_INSTR -> 0xFFFF - 0
                WasmOp.PSEUDO_COMMENT_GROUP_START -> 0xFFFF - 1
                WasmOp.PSEUDO_COMMENT_GROUP_END -> 0xFFFF - 2
                else -> error("Unknown pseudo-opcode: $instr")
            }
        }

        body.writeUInt16(opcode.toUShort())

        val location = instr.location

        val tag = if (location != null) {
            when (instr.immediatesCount) {
                0 -> InstructionTags.LOCATED0
                1 -> InstructionTags.LOCATED1
                2 -> InstructionTags.LOCATED2
                3 -> InstructionTags.LOCATED3
                4 -> InstructionTags.LOCATED4
                else -> error("Invalid instruction with immediates count ${instr.immediatesCount}")
            }
        } else {
            when (instr.immediatesCount) {
                0 -> InstructionTags.NOT_LOCATED0
                1 -> InstructionTags.NOT_LOCATED1
                2 -> InstructionTags.NOT_LOCATED2
                3 -> InstructionTags.NOT_LOCATED3
                4 -> InstructionTags.NOT_LOCATED4
                else -> error("Invalid instruction with immediates count ${instr.immediatesCount}")
            }
        }
        withTag(tag) {
            if (location != null) {
                serializeSourceLocation(location)
            }
            instr.forEachImmediates {
                serializeWasmImmediate(it)
            }
        }
    }

    private fun serializeWasmImmediate(i: WasmImmediate): Unit =
        when (i) {
            is WasmImmediate.BlockType.Function -> withTag(ImmediateTags.BLOCK_TYPE_FUNCTION) { serializeWasmSymbolReadOnly(i.type, ::serializeWasmFunctionType) }
            is WasmImmediate.BlockType.Value -> withTagNullable(ImmediateTags.BLOCK_TYPE_VALUE, i.type) { serializeWasmType(i.type!!) }
            is WasmImmediate.Catch -> withTag(ImmediateTags.CATCH) { serializeCatchImmediate(i) }
            is WasmImmediate.ConstF32 -> withTag(ImmediateTags.CONST_F32) { body.writeUInt32(i.rawBits) }
            is WasmImmediate.ConstF64 -> withTag(ImmediateTags.CONST_F64) { body.writeUInt64(i.rawBits) }
            is WasmImmediate.ConstI32 -> withTag(ImmediateTags.CONST_I32) { serializeInt(i.value) }
            is WasmImmediate.ConstI64 -> withTag(ImmediateTags.CONST_I64) { serializeLong(i.value) }
            is WasmImmediate.ConstString -> withTag(ImmediateTags.CONST_STRING) { serializeString(i.value) }
            is WasmImmediate.ConstU8 -> withTag(ImmediateTags.CONST_U8) { body.writeUByte(i.value) }
            is WasmImmediate.DataIdx -> withTag(ImmediateTags.DATA_INDEX) { serializeWasmSymbolReadOnly(i.value) { serializeInt(it) } }
            is WasmImmediate.ElemIdx -> withTag(ImmediateTags.ELEMENT_INDEX) { serializeWasmElement(i.value) }
            is FuncSymbol -> withTag(ImmediateTags.FUNC_INDEX) { serializeIdSignature(i.value) }

            is GcTypeSymbol -> withTag(ImmediateTags.GC_TYPE) { serializeIdSignature(i.value) }
            is VTableTypeSymbol -> withTag(ImmediateTags.VT_TYPE) { serializeIdSignature(i.value) }
            is FunctionTypeSymbol -> withTag(ImmediateTags.FUNC_TYPE) { serializeIdSignature(i.value) }

            is FieldGlobalSymbol -> withTag(ImmediateTags.GLOBAL_FIELD) { serializeIdSignature(i.value) }
            is VTableGlobalSymbol -> withTag(ImmediateTags.GLOBAL_VTABLE) { serializeIdSignature(i.value) }
            is ClassITableGlobalSymbol -> withTag(ImmediateTags.GLOBAL_CLASSITABLE) { serializeIdSignature(i.value) }
            is RttiGlobalSymbol -> withTag(ImmediateTags.GLOBAL_RTTI) { serializeIdSignature(i.value) }
            is LiteralGlobalSymbol -> withTag(ImmediateTags.GLOBAL_STRING) { serializeString(i.value) }
            is WasmImmediate.HeapType -> withTag(ImmediateTags.HEAP_TYPE) { serializeWasmHeapType(i.value) }
            is WasmImmediate.LabelIdx -> withTag(ImmediateTags.LABEL_INDEX) { serializeInt(i.value) }
            is WasmImmediate.LabelIdxVector -> withTag(ImmediateTags.LABEL_INDEX_VECTOR) { serializeList(i.value) { serializeInt(it) } }
            is WasmImmediate.LocalIdx -> withTag(ImmediateTags.LOCAL_INDEX) { serializeInt(i.value) }
            is WasmImmediate.MemArg -> withTag(ImmediateTags.MEM_ARG) { body.writeUInt32(i.align); body.writeUInt32(i.offset) }
            is WasmImmediate.MemoryIdx -> withTag(ImmediateTags.MEMORY_INDEX) { serializeInt(i.value) }
            is WasmImmediate.StructFieldIdx -> withTag(ImmediateTags.STRUCT_FIELD_INDEX) { serializeInt(i.value) }
            is WasmImmediate.SymbolI32 -> withTag(ImmediateTags.SYMBOL_I32) { serializeWasmSymbolReadOnly(i.value) { serializeInt(it) } }
            is WasmImmediate.TableIdx -> withTag(ImmediateTags.TABLE_INDEX) { serializeWasmSymbolReadOnly(i.value) { serializeInt(it) } }
            is WasmImmediate.TagIdx -> withTag(ImmediateTags.TAG_INDEX) { serializeWasmSymbolReadOnly(i.value) { serializeInt(it) } }
            is WasmImmediate.ValTypeVector -> withTag(ImmediateTags.VALUE_TYPE_VECTOR) { serializeList(i.value, ::serializeWasmType) }
            else -> error("Unknown WasmImmediate type: ${i::class.simpleName}")
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
            body.writeUInt32(table.limits.minSize)
            max?.let { body.writeUInt32(it) }
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
            body.writeUInt32(limit.minSize)
            limit.maxSize?.let { body.writeUInt32(it) }
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
        serializeInt(list.size)
        list.forEach { serializeFunc(it) }
    }

    private fun <T> serializeSet(set: Set<T>, serializeFunc: (T) -> Unit) {
        serializeInt(set.size)
        set.forEach { serializeFunc(it) }
    }

    private fun <K, V> serializeMap(map: Map<K, V>, serializeKeyFunc: (K) -> Unit, serializeValueFunc: (V) -> Unit) {
        serializeInt(map.size)
        map.forEach { (key, value) ->
            serializeKeyFunc(key)
            serializeValueFunc(value)
        }
    }

    private fun serializeSourceLocation(sl: SourceLocation) =
        when (sl) {
            SourceLocation.NoLocation -> setTag(LocationTags.NO_LOCATION)
            SourceLocation.IgnoredLocation -> setTag(LocationTags.IGNORED_LOCATION)
            SourceLocation.NextLocation -> setTag(LocationTags.NEXT_LOCATION)
            is SourceLocation.DefinedLocation -> withTag(LocationTags.DEFINED_LOCATION) {
                serializeString(sl.file)
                body.writeUInt32(sl.line.toUInt())
                body.writeUInt32(sl.column.toUInt())
            }
        }

    private fun <T> serializeNullable(value: T?, serializeFunc: (T) -> Unit) {
        if (value != null) {
            withTag(NullableTags.NOT_NULL) { serializeFunc(value) }
        } else {
            setTag(NullableTags.NULL)
        }
    }

    private fun serializeIdSignature(idSignature: IdSignature) = serializeAsReference(idSignature) {
        when (idSignature) {
            is IdSignature.AccessorSignature -> withTag(IdSignatureTags.ACCESSOR) { serializeAccessorSignature(idSignature) }
            is IdSignature.CommonSignature -> withTag(IdSignatureTags.COMMON) { serializeCommonSignature(idSignature) }
            is IdSignature.CompositeSignature -> withTag(IdSignatureTags.COMPOSITE) { serializeCompositeSignature(idSignature) }
            is IdSignature.FileLocalSignature -> withTag(IdSignatureTags.FILE_LOCAL) { serializeFileLocalSignature(idSignature) }
            is IdSignature.LocalSignature -> withTag(IdSignatureTags.LOCAL) { serializeLocalSignature(idSignature) }
            is IdSignature.LoweredDeclarationSignature -> withTag(IdSignatureTags.LOWERED_DECLARATION) {
                serializeLoweredDeclarationSignature(idSignature)
            }
            is IdSignature.ScopeLocalDeclaration -> withTag(IdSignatureTags.SCOPE_LOCAL_DECLARATION) {
                serializeScopeLocalDeclaration(idSignature)
            }
            is IdSignature.SpecialFakeOverrideSignature -> error("SpecialFakeOverrideSignature is not supposed to be serialized")
            is IdSignature.FileSignature -> withTag(IdSignatureTags.FILE) { serializeString(idSignature.fileName) }
        }
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
                id?.let { serializeLong(it) }
                serializeLong(mask)
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
            withFlags {
                serializeIdSignature(container)
                serializeLong(id)
            }
        }
    }

    private fun serializeLocalSignature(local: IdSignature.LocalSignature) {
        with(local) {
            withFlags(hashSig == null) {
                serializeString(localFqn)
                hashSig?.let { serializeLong(it) }
            }
        }
    }

    private fun serializeLoweredDeclarationSignature(loweredDeclaration: IdSignature.LoweredDeclarationSignature) {
        with(loweredDeclaration) {
            serializeIdSignature(original)
            serializeInt(stage)
            serializeInt(index)
        }
    }

    private fun serializeScopeLocalDeclaration(scopeLocal: IdSignature.ScopeLocalDeclaration) {
        with(scopeLocal) {
            withFlags {
                serializeInt(id)
            }
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
        serializeList(constantDataCharArray.value) { serializeWasmSymbolReadOnly(it) { serializeInt(it.code) } }
        serializeBoolean(constantDataCharArray.fitsLatin1)
    }

    private fun serializeConstantDataCharField(constantDataCharField: ConstantDataCharField) {
        serializeWasmSymbolReadOnly(constantDataCharField.value) { serializeInt(it.code) }
    }

    private fun serializeConstantDataIntArray(constantDataIntArray: ConstantDataIntArray) {
        serializeList(constantDataIntArray.value) { serializeWasmSymbolReadOnly(it) { serializeInt(it) } }
    }

    private fun serializeConstantDataIntField(constantDataIntField: ConstantDataIntField) {
        serializeWasmSymbolReadOnly(constantDataIntField.value) { body.writeUInt32(it.toUInt()) }
    }

    private fun serializeConstantDataIntegerArray(constantDataIntegerArray: ConstantDataIntegerArray) {
        serializeList(constantDataIntegerArray.value) { serializeLong(it) }
        body.writeUInt32(constantDataIntegerArray.integerSize.toUInt())
    }

    private fun serializeConstantDataStruct(constantDataStruct: ConstantDataStruct) {
//        serializeString(constantDataStruct.name)
        serializeList(constantDataStruct.elements, ::serializeConstantDataElement)
    }

    private fun serializeJsCodeSnippet(jsCodeSnippet: WasmCompiledModuleFragment.JsCodeSnippet) {
        serializeWasmSymbolReadOnly(jsCodeSnippet.importName, ::serializeString)
        serializeString(jsCodeSnippet.jsCode)
    }


    private fun serializeByteArray(bytes: ByteArray) {
        serializeInt(bytes.size)
        body.writeBytes(bytes)
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
        body.writeUInt32(int.toUInt())
    }

    private fun serializeBoolean(bool: Boolean) {
        body.writeUByte(bool.toByte().toUByte())
    }

    private fun serializeLong(long: Long) {
        body.writeUInt64(long.toULong())
    }

    private fun serializeDefinedFunctions(functions: Map<IdSignature, WasmFunction>) {
        serializeMap(functions, ::serializeIdSignature, ::serializeWasmFunction)
    }

    private fun serializeDefinedGlobals(functions: Map<IdSignature, WasmGlobal>) {
        serializeMap(functions, ::serializeIdSignature, ::serializeWasmGlobal)
    }

    private fun serializeDefinedTypeDeclarations(functions: Map<IdSignature, WasmTypeDeclaration>) {
        serializeMap(functions, ::serializeIdSignature, ::serializeWasmTypeDeclaration)
    }

    private fun serializeDefinedStructDeclarations(functions: Map<IdSignature, WasmStructDeclaration>) {
        serializeMap(functions, ::serializeIdSignature, ::serializeWasmStructDeclaration)
    }

    private fun serializeDefinedFunctionTypesDeclarations(functions: Map<IdSignature, WasmFunctionType>) {
        serializeMap(functions, ::serializeIdSignature, ::serializeWasmFunctionType)
    }

    private fun <T : Any> serializeWasmSymbolReadOnly(symbol: WasmSymbolReadOnly<T>, serializeFunc: (T) -> Unit) =
        serializeAsReference(symbol) {
            withFlags(symbol.getOwner() == null) {
                symbol.getOwner()?.let { serializeFunc(it) }
            }
        }

    private fun serializeGlobalLiterals(globalLiterals: Set<LiteralGlobalSymbol>) {
        serializeSet(globalLiterals) {
            serializeString(it.value)
        }
    }

    private fun serializeIntSymbol(symbol: WasmSymbol<Int>) = serializeWasmSymbolReadOnly(symbol) { serializeInt(it) }

    private fun serializeConstantArrayDataSegmentId(segments: Map<Pair<List<Long>, WasmType>, WasmSymbol<Int>>) {
        serializeMap(
            map = segments,
            serializeKeyFunc = { key ->
                serializePair(key, { list ->
                    serializeList(list, ::serializeLong)
                }, ::serializeWasmType)
            },
            serializeValueFunc = ::serializeIntSymbol
        )
    }

    private fun serializeClassAssociatedObjects(classAssociatedObjects: ClassAssociatedObjects) {
        serializeLong(classAssociatedObjects.klass)
        serializeList(classAssociatedObjects.objects, ::serializeAssociatedObject)
    }

    private fun serializeAssociatedObject(associatedObject: AssociatedObject) = withFlags(associatedObject.isExternal) {
        serializeLong(associatedObject.obj)
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
                obj.id?.let { body.writeUInt32(it.toUInt()) }
                if (obj.name.isNotEmpty()) serializeString(obj.name)
                serializeFunc()
            }
        }

    private fun withTagNullable(tag: UInt, obj: Any?, serializeFunc: () -> Unit) {
        // Use the MSB of the tag as a flag
        val isNull = if (obj == null) 1U else 0U
        val newId = (tag or (isNull shl 7)).toUByte()
        body.writeUByte(newId)
        if (isNull != 1U) serializeFunc()
    }

    private fun setTag(tag: UInt) {
        body.writeUByte(tag.toUByte())
    }

    private fun withTag(tag: UInt, serializeFunc: () -> Unit) {
        body.writeUByte(tag.toUByte())
        serializeFunc()
    }

    private fun withFlags(vararg flags: Boolean, serializeFunc: () -> Unit) {
        if (flags.size > 8) {
            error("Can't pack more than 8 flags in a single byte")
        }
        body.writeUByte(flagsToUByte(flags))
        serializeFunc()
    }

    private inline fun serializeAsReference(obj: Any, crossinline serializeFunc: () -> Unit) {
        val index = serializedReferenceIndexes[obj]
        if (index == null) {
            serializeInt(IN_PLACE)
            serializedReferenceIndexes[obj] = serializedReferenceIndexes.size
            serializeFunc()
        } else {
            serializeInt(index)
        }
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