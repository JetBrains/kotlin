/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.native.interop.indexer

import clang.*
import clang.CXIdxEntityKind.*
import clang.CXTypeKind.*
import kotlinx.cinterop.*

private class StructDeclImpl(spelling: String) : StructDecl(spelling) {
    override var def: StructDefImpl? = null
}

private class StructDefImpl(size: Long, align: Int, decl: StructDecl, hasNaturalLayout: Boolean) :
        StructDef(size, align, decl, hasNaturalLayout) {

    override val fields = mutableListOf<Field>()
}

private class EnumDefImpl(spelling: String, type: PrimitiveType) : EnumDef(spelling, type) {
    override val constants = mutableListOf<EnumConstant>()
}

internal class NativeIndexImpl(val library: NativeLibrary) : NativeIndex() {

    private data class DeclarationID(val usr: String)

    private val structById = mutableMapOf<DeclarationID, StructDeclImpl>()

    override val structs: List<StructDecl>
        get() = structById.values.toList()

    private val enumById = mutableMapOf<DeclarationID, EnumDefImpl>()

    override val enums: List<EnumDef>
        get() = enumById.values.toList()

    private val typedefById = mutableMapOf<DeclarationID, TypedefDef>()

    override val typedefs: List<TypedefDef>
        get() = typedefById.values.toList()

    val functionByName = mutableMapOf<String, FunctionDecl>()

    override val functions: List<FunctionDecl>
        get() = functionByName.values.toList()

    override val macroConstants = mutableListOf<ConstantDef>()

    private fun getDeclarationId(cursor: CValue<CXCursor>): DeclarationID {
        val usr = clang_getCursorUSR(cursor).convertAndDispose()
        return DeclarationID(usr)
    }

    private fun getStructDeclAt(cursor: CValue<CXCursor>): StructDeclImpl {
        val declId = getDeclarationId(cursor)

        val structDecl = structById.getOrPut(declId) {
            val cursorType = clang_getCursorType(cursor)
            val typeSpelling = clang_getTypeSpelling(cursorType).convertAndDispose()

            StructDeclImpl(typeSpelling)
        }

        if (structDecl.def == null) {
            val definitionCursor = clang_getCursorDefinition(cursor)
            if (clang_Cursor_isNull(definitionCursor) == 0) {
                assert (clang_isCursorDefinition(definitionCursor) != 0)
                createStructDef(structDecl, cursor)
            }
        }

        return structDecl
    }

    private fun createStructDef(structDecl: StructDeclImpl, cursor: CValue<CXCursor>) {
        val type = clang_getCursorType(cursor)
        val size = clang_Type_getSizeOf(type)
        val align = clang_Type_getAlignOf(type).toInt()
        val hasNaturalLayout = structHasNaturalLayout(cursor)
        val structDef = StructDefImpl(size, align, structDecl, hasNaturalLayout)
        structDecl.def = structDef

        visitChildren(cursor) { childCursor: CValue<CXCursor>, _: CValue<CXCursor> ->
            if (clang_getCursorKind(childCursor) == CXCursorKind.CXCursor_FieldDecl) {
                val name = clang_getCursorSpelling(childCursor).convertAndDispose()
                val fieldType = convertCursorType(childCursor)
                val offset = clang_Cursor_getOffsetOfField(childCursor)
                structDef.fields.add(Field(name, fieldType, offset))
            }
            CXChildVisitResult.CXChildVisit_Continue
        }
    }

    private fun getEnumDefAt(cursor: CValue<CXCursor>): EnumDefImpl {
        if (clang_isCursorDefinition(cursor) == 0) {
            TODO("support enum forward declarations")
        }

        val declId = getDeclarationId(cursor)

        return enumById.getOrPut(declId) {
            val cursorType = clang_getCursorType(cursor)
            val typeSpelling = clang_getTypeSpelling(cursorType).convertAndDispose()

            val baseType = convertType(clang_getEnumDeclIntegerType(cursor)) as PrimitiveType

            val enumDef = EnumDefImpl(typeSpelling, baseType)

            visitChildren(cursor) { childCursor, _ ->
                if (clang_getCursorKind(childCursor) == CXCursorKind.CXCursor_EnumConstantDecl) {
                    val name = clang_getCursorSpelling(childCursor).convertAndDispose()
                    val value = clang_getEnumConstantDeclValue(childCursor)

                    val constant = EnumConstant(name, value, isExplicitlyDefined = !childCursor.isLeaf())
                    enumDef.constants.add(constant)
                }

                CXChildVisitResult.CXChildVisit_Continue
            }

            enumDef
        }
    }

    private fun builtinVaListType(type: CValue<CXType>, name: String, underlying: Type): Type {
        assert (type.kind == CXType_Typedef)
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

    fun getTypedef(type: CValue<CXType>): Type {
        val declCursor = clang_getTypeDeclaration(type)
        val name = getCursorSpelling(declCursor)

        val underlying = convertType(clang_getTypedefDeclUnderlyingType(declCursor))

        if (name == "__builtin_va_list" && underlying is ConstArrayType) {
            // On some platforms (e.g. macOS) libclang reports `__builtin_va_list` to be defined as array using
            //   typedef struct __va_list_tag __builtin_va_list[1]
            // while `struct __va_list_tag` is incomplete.
            // So `__builtin_va_list` gets declared as incorrect type, and requires some dirty hacks:
            return builtinVaListType(type, name, underlying)
        }


        if (name == "__gnuc_va_list"  || name == "va_list") {
            // TODO: fix GNUC varargs support.
            return UnsupportedType
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
    fun structHasNaturalLayout(structDefCursor: CValue<CXCursor>): Boolean {
        val defKind = structDefCursor.kind

        when (defKind) {

            CXCursorKind.CXCursor_UnionDecl -> return false

            CXCursorKind.CXCursor_StructDecl -> {
                var hasAttributes = false

                visitChildren(structDefCursor) { cursor, _ ->
                    if (clang_isAttribute(cursor.kind) != 0) {
                        hasAttributes = true
                    }
                    CXChildVisitResult.CXChildVisit_Continue
                }

                return !hasAttributes
            }

            else -> throw IllegalArgumentException(defKind.toString())
        }
    }

    private fun convertCursorType(cursor: CValue<CXCursor>) =
        convertType(clang_getCursorType(cursor))

    fun convertType(type: CValue<CXType>): Type {
        val primitiveType = convertUnqualifiedPrimitiveType(type)
        if (primitiveType != UnsupportedType) {
            return primitiveType
        }

        val kind = type.kind
        return when (kind) {
            CXType_Elaborated -> convertType(clang_Type_getNamedType(type))

            CXType_Unexposed -> {
                if (clang_getResultType(type).kind != CXTypeKind.CXType_Invalid) {
                    convertFunctionType(type)
                } else {
                    val canonicalType = clang_getCanonicalType(type)
                    if (canonicalType.kind != CXType_Unexposed) {
                        convertType(canonicalType)
                    } else {
                        throw NotImplementedError()
                    }
                }
            }

            CXType_Void -> VoidType

            CXType_Typedef -> getTypedef(type)

            CXType_Record -> RecordType(getStructDeclAt(clang_getTypeDeclaration(type)))
            CXType_Enum -> EnumType(getEnumDefAt(clang_getTypeDeclaration(type)))

            CXType_Pointer -> {
                val pointeeType = clang_getPointeeType(type)
                val pointeeIsConst =
                        (clang_isConstQualifiedType(clang_getCanonicalType(pointeeType)) != 0)

                PointerType(convertType(pointeeType), pointeeIsConst = pointeeIsConst)
            }

            CXType_ConstantArray -> {
                val elemType = convertType(clang_getArrayElementType(type))
                val length = clang_getArraySize(type)
                ConstArrayType(elemType, length)
            }

            CXType_IncompleteArray -> {
                val elemType = convertType(clang_getArrayElementType(type))
                IncompleteArrayType(elemType)
            }

            CXType_FunctionProto -> {
                convertFunctionType(type)
            }

            else -> UnsupportedType
        }
    }

    private fun convertFunctionType(type: CValue<CXType>): Type {
        val kind = type.kind
        assert (kind == CXType_Unexposed || kind == CXType_FunctionProto)

        return if (clang_isFunctionTypeVariadic(type) != 0) {
            UnsupportedType
        } else {
            val returnType = convertType(clang_getResultType(type))
            val numArgs = clang_getNumArgTypes(type)
            val paramTypes = (0..numArgs - 1).map {
                convertType(clang_getArgType(type, it))
            }
            FunctionType(paramTypes, returnType)
        }
    }

    fun indexDeclaration(info: CXIdxDeclInfo): Unit {
        val cursor = info.cursor.readValue()
        val entityInfo = info.entityInfo!!.pointed
        val entityName = entityInfo.name?.toKString()
        val kind = entityInfo.kind

        if (!this.library.includesDeclaration(cursor)) {
            return
        }

        when (kind) {
            CXIdxEntity_Struct, CXIdxEntity_Union -> {
                getStructDeclAt(cursor)
            }

            CXIdxEntity_Typedef -> {
                val type = clang_getCursorType(cursor)
                getTypedef(type)
            }

            CXIdxEntity_Function -> {
                val name = entityName!!
                val returnType = convertType(clang_getCursorResultType(cursor))
                val argNum = clang_Cursor_getNumArguments(cursor)
                val args = (0 .. argNum - 1).map {
                    val argCursor = clang_Cursor_getArgument(cursor, it)
                    val argName = getCursorSpelling(argCursor)
                    val type = convertCursorType(argCursor)
                    Parameter(argName, type)
                }

                val binaryName = when (library.language) {
                    Language.C -> clang_Cursor_getMangling(cursor).convertAndDispose()
                }

                val definitionCursor = clang_getCursorDefinition(cursor)
                val isDefined = (clang_Cursor_isNull(definitionCursor) == 0)

                val isVararg = clang_Cursor_isVariadic(cursor) != 0

                functionByName[name] = FunctionDecl(name, args, returnType, binaryName, isDefined, isVararg)
            }

            CXIdxEntity_Enum -> {
                getEnumDefAt(cursor)
            }

            else -> {
                // Ignore declaration.
            }
        }
    }

}

fun buildNativeIndexImpl(library: NativeLibrary): NativeIndex {
    val result = NativeIndexImpl(library)
    indexDeclarations(library, result)
    findMacroConstants(library, result)
    return result
}

private fun indexDeclarations(library: NativeLibrary, nativeIndex: NativeIndexImpl) {
    val index = clang_createIndex(0, 0)!!
    try {
        val translationUnit = library.parse(index, options = CXTranslationUnit_DetailedPreprocessingRecord)
        try {
            translationUnit.ensureNoCompileErrors()

            val headers = getFilteredHeaders(library, index, translationUnit)

            indexTranslationUnit(index, translationUnit, 0, object : Indexer {
                override fun indexDeclaration(info: CXIdxDeclInfo) {
                    val file = memScoped {
                        val fileVar = alloc<CXFileVar>()
                        clang_indexLoc_getFileLocation(info.loc.readValue(), null, fileVar.ptr, null, null, null)
                        fileVar.value
                    }

                    if (file in headers) {
                        nativeIndex.indexDeclaration(info)
                    }
                }
            })
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    } finally {
        clang_disposeIndex(index)
    }
}
