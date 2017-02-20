package org.jetbrains.kotlin.native.interop.indexer

import clang.*
import clang.CXIdxEntityKind.*
import clang.CXTypeKind.*
import kotlinx.cinterop.*
import java.io.File

private class StructDeclImpl(spelling: String) : StructDecl(spelling) {
    override var def: StructDefImpl? = null
}

private class StructDefImpl(size: Long, align: Int, decl: StructDecl, hasNaturalLayout: Boolean) :
        StructDef(size, align, decl, hasNaturalLayout) {

    override val fields = mutableListOf<Field>()
}

private class EnumDefImpl(spelling: String, type: PrimitiveType) : EnumDef(spelling, type) {
    override val values = mutableListOf<EnumValue>()
}

private class NativeIndexImpl : NativeIndex() {

    private data class DeclarationID(val usr: String)

    val structById = mutableMapOf<DeclarationID, StructDeclImpl>()

    override val structs: List<StructDecl>
        get() = structById.values.toList()

    val enumById = mutableMapOf<DeclarationID, EnumDefImpl>()

    override val enums: List<EnumDef>
        get() = enumById.values.toList()

    val typedefById = mutableMapOf<DeclarationID, TypedefDef>()

    override val typedefs: List<TypedefDef>
        get() = typedefById.values.toList()

    val functionByName = mutableMapOf<String, FunctionDecl>()

    override val functions: List<FunctionDecl>
        get() = functionByName.values.toList()

    fun getDeclarationId(cursor: CXCursor): DeclarationID = memScoped {
        val usr = clang_getCursorUSR(cursor, memScope).convertAndDispose()
        DeclarationID(usr)
    }

    fun getStructDeclAt(cursor: CXCursor): StructDeclImpl {
        val declId = getDeclarationId(cursor)

        return structById.getOrPut(declId) {
            memScoped {
                val cursorType = clang_getCursorType(cursor, memScope)
                val typeSpelling = clang_getTypeSpelling(cursorType, memScope).convertAndDispose()

                StructDeclImpl(typeSpelling)
            }
        }
    }

    fun getEnumDefAt(cursor: CXCursor): EnumDefImpl {
        if (clang_isCursorDefinition(cursor) == 0) {
            TODO("support enum forward declarations")
        }

        val declId = getDeclarationId(cursor)

        return enumById.getOrPut(declId) {
            memScoped {
                val cursorType = clang_getCursorType(cursor, memScope)
                val typeSpelling = clang_getTypeSpelling(cursorType, memScope).convertAndDispose()

                val baseType = convertType(clang_getEnumDeclIntegerType(cursor, memScope)) as PrimitiveType

                EnumDefImpl(typeSpelling, baseType)
            }
        }
    }

    private fun builtinVaListType(type: CXType, name: String, underlying: Type): Type {
        assert (type.kind.value == CXType_Typedef)
        val declarationId = DeclarationID("c:@T@$name")

        val structDeclaration = structById.getOrPut(declarationId) {
            StructDeclImpl(name).apply {
                val size = clang_Type_getSizeOf(type)
                val align = clang_Type_getAlignOf(type).toInt()
                val def = StructDefImpl(size, align, this, hasNaturalLayout = false)
                this.def = def
            }
        }

        assert (underlying is ConstArrayType)
        // So the result must feel like array:
        return ConstArrayType(RecordType(structDeclaration), 1)
    }

    fun getTypedef(type: CXType): Type = memScoped {
        val declCursor = clang_getTypeDeclaration(type, memScope)
        val name = clang_getCursorSpelling(declCursor, memScope).convertAndDispose()

        val underlying = convertType(clang_getTypedefDeclUnderlyingType(declCursor, memScope))

        if (name == "__builtin_va_list") {
            // On some platforms (e.g. macOS) libclang reports `__builtin_va_list` to be defined as array using
            //   typedef struct __va_list_tag __builtin_va_list[1]
            // while `struct __va_list_tag` is incomplete.
            // So `__builtin_va_list` gets declared as incorrect type, and requires some dirty hacks:
            return builtinVaListType(type, name, underlying)
        }

        if ((underlying is RecordType && underlying.decl.spelling.split(' ').last() == name) ||
                (underlying is EnumType && underlying.def.spelling.split(' ').last() == name)) {

            // special handling for:
            // typedef struct { ... } name;
            // typedef enum { ... } name;
            // FIXME: implement better solution
            return underlying
        }

        val declId = getDeclarationId(declCursor)
        val typedefDef = typedefById.getOrPut(declId) {

            TypedefDef(underlying, name)
        }

        return Typedef(typedefDef)
    }

    /**
     * Computes [StructDef.hasNaturalLayout] property.
     */
    fun structHasNaturalLayout(structDefCursor: CXCursor): Boolean {
        val defKind = structDefCursor.kind.value

        when (defKind) {

            CXCursorKind.CXCursor_UnionDecl -> return false

            CXCursorKind.CXCursor_StructDecl -> memScoped {
                val hasAttributes = memScope.alloc<CInt32Var>()
                hasAttributes.value = 0
                clang_visitChildren(structDefCursor, staticCFunction { cursor, parent, clientData ->
                    if (clang_isAttribute(cursor.kind.value) != 0) {
                        val hasAttributes = clientData!!.reinterpret<CInt32Var>().pointed
                        hasAttributes.value = 1
                    }
                    CXChildVisitResult.CXChildVisit_Continue
                }, hasAttributes.ptr)

                return hasAttributes.value == 0
            }

            else -> throw IllegalArgumentException(defKind.toString())
        }
    }

    fun convertType(type: CXType): Type = memScoped {
        val kind = type.kind.value
        return when (kind) {
            CXType_Elaborated -> convertType(clang_Type_getNamedType(type, memScope))

            CXType_Unexposed -> {
                val canonicalType = clang_getCanonicalType(type, memScope)
                if (canonicalType.kind.value != CXType_Unexposed) {
                    convertType(canonicalType)
                } else {
                    throw NotImplementedError()
                }
            }

            // TODO: is e.g. CXType_Int guaranteed to be int32_t?
            CXType_Void -> VoidType

            CXType_Char_U, CXType_UChar -> UInt8Type
            CXType_Char_S, CXType_SChar -> Int8Type

            CXType_UShort -> UInt16Type
            CXType_Short -> Int16Type

            CXType_UInt -> UInt32Type
            CXType_Int -> Int32Type

            CXType_ULong -> UIntPtrType
            CXType_Long -> IntPtrType

            CXType_ULongLong -> UInt64Type
            CXType_LongLong -> Int64Type

            CXType_Typedef -> getTypedef(type)

            CXType_Record -> RecordType(getStructDeclAt(clang_getTypeDeclaration(type, memScope)))
            CXType_Enum -> EnumType(getEnumDefAt(clang_getTypeDeclaration(type, memScope)))

            CXType_Pointer -> PointerType(convertType(clang_getPointeeType(type, memScope)))

            CXType_ConstantArray -> {
                val elemType = convertType(clang_getArrayElementType(type, memScope))
                val length = clang_getArraySize(type)
                ConstArrayType(elemType, length)
            }

            CXType_IncompleteArray -> {
                val elemType = convertType(clang_getArrayElementType(type, memScope))
                IncompleteArrayType(elemType)
            }

            CXType_FunctionProto -> {
                if (clang_isFunctionTypeVariadic(type) != 0) {
                    UnsupportedType
                } else {
                    val returnType = convertType(clang_getResultType(type, memScope))
                    val numArgs = clang_getNumArgTypes(type)
                    val paramTypes = (0..numArgs - 1).map {
                        convertType(clang_getArgType(type, it, memScope))
                    }
                    FunctionType(paramTypes, returnType)
                }
            }

            else -> UnsupportedType
        }
    }

    fun indexDeclaration(info: CXIdxDeclInfo): Unit = memScoped {
        val cursor = info.cursor
        val entityInfo = info.entityInfo.pointed!!
        val entityName = entityInfo.name.value?.asCString()?.toString()
        val kind = entityInfo.kind.value

        when (kind) {
            CXIdxEntity_Field -> {
                val name = entityName!!
                val type = convertType(clang_getCursorType(cursor, memScope))
                val offset = clang_Cursor_getOffsetOfField(cursor)

                val container = info.semanticContainer.pointed!!
                val structDef = getStructDeclAt(container.cursor).def!!
                structDef.fields.add(Field(name, type, offset))
            }

            CXIdxEntity_Struct, CXIdxEntity_Union -> {
                val structDecl = getStructDeclAt(cursor)
                if (clang_isCursorDefinition(cursor) != 0) {
                    val type = clang_getCursorType(cursor, memScope)
                    val size = clang_Type_getSizeOf(type)
                    val align = clang_Type_getAlignOf(clang_getCursorType(cursor, memScope)).toInt()
                    val hasNaturalLayout = structHasNaturalLayout(cursor)
                    structDecl.def = StructDefImpl(size, align, structDecl, hasNaturalLayout)
                }
            }

            CXIdxEntity_Function -> {
                val name = entityName!!
                val returnType = convertType(clang_getCursorResultType(cursor, memScope))
                val argNum = clang_Cursor_getNumArguments(cursor)
                val args = (0 .. argNum - 1).map {
                    val argCursor = clang_Cursor_getArgument(cursor, it, memScope)
                    val argName = clang_getCursorSpelling(argCursor, memScope).convertAndDispose()
                    val type = convertType(clang_getCursorType(argCursor, memScope))
                    Parameter(argName, type)
                }

                functionByName[name] = FunctionDecl(name, args, returnType)
            }

            CXIdxEntity_Enum -> {
                getEnumDefAt(cursor)
            }

            CXIdxEntity_EnumConstant -> {
                val container = info.semanticContainer.pointed!!
                val name = entityName!!
                val value = clang_getEnumConstantDeclValue(info.cursor)

                val values = getEnumDefAt(container.cursor).values
                val existingValue = values.find { it.name == name }
                if (existingValue == null) {
                    values.add(EnumValue(name, value))
                } else {
                    // in some cases Clang may index the same definition multiple times; ignore redeclaration
                    // TODO: implement the same fix for structs
                    assert (existingValue.value == value)
                }
            }
        }
    }

}

fun CXString.convertAndDispose(): String {
    try {
        return clang_getCString(this)!!.asCString().toString()
    } finally {
        clang_disposeString(this)
    }
}

fun buildNativeIndexImpl(headerFile: File, args: List<String>): NativeIndex {
    // TODO: dispose all allocated memory and resources
    val args1 = args.map { CString.fromString(it, nativeHeap)!!.asCharPtr() }.toTypedArray()

    val index = clang_createIndex(0, 0)
    val indexAction = clang_IndexAction_create(index)
    val callbacks = nativeHeap.alloc<IndexerCallbacks>()

    val res = NativeIndexImpl()
    val nativeIndexPtr = StableObjPtr.create(res)
    val clientData = nativeIndexPtr.value

    try {
        with(callbacks) {
            abortQuery.value = null
            diagnostic.value = null
            enteredMainFile.value = null
            ppIncludedFile.value = null
            importedASTFile.value = null
            startedTranslationUnit.value = null
            indexDeclaration.value = staticCFunction { clientData, info ->
                val nativeIndex = StableObjPtr.fromValue(clientData!!).get() as NativeIndexImpl
                nativeIndex.indexDeclaration(info!!.pointed)
            }
            indexEntityReference.value = null
        }

        val commandLineArgs = nativeHeap.allocArrayOfPointersTo(*args1)[0].ptr

        clang_indexSourceFile(indexAction, clientData, callbacks.ptr, IndexerCallbacks.size.toInt(),
                0, headerFile.path, commandLineArgs, args1.size, null, 0, null, 0)


        return res

    } finally {
        nativeIndexPtr.dispose()
    }
}