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

private class StructDefImpl(
        size: Long, align: Int, decl: StructDecl,
        hasNaturalLayout: Boolean
) : StructDef(
        size, align, decl,
        hasNaturalLayout = hasNaturalLayout
) {

    override val fields = mutableListOf<Field>()
}

private class EnumDefImpl(spelling: String, type: Type) : EnumDef(spelling, type) {
    override val constants = mutableListOf<EnumConstant>()
}

private interface ObjCClassOrProtocolImpl {
    val protocols: MutableList<ObjCProtocol>
    val methods: MutableList<ObjCMethod>
    val properties: MutableList<ObjCProperty>
}

private class ObjCProtocolImpl(name: String) : ObjCProtocol(name), ObjCClassOrProtocolImpl {
    override val protocols = mutableListOf<ObjCProtocol>()
    override val methods = mutableListOf<ObjCMethod>()
    override val properties = mutableListOf<ObjCProperty>()
}

private class ObjCClassImpl(name: String) : ObjCClass(name), ObjCClassOrProtocolImpl {
    override val protocols = mutableListOf<ObjCProtocol>()
    override val methods = mutableListOf<ObjCMethod>()
    override val properties = mutableListOf<ObjCProperty>()
    override var baseClass: ObjCClass? = null
}

internal class NativeIndexImpl(val library: NativeLibrary) : NativeIndex() {

    private sealed class DeclarationID {
        data class USR(val usr: String) : DeclarationID()
        object VaListTag : DeclarationID()
        object BuiltinVaList : DeclarationID()
    }

    private val structById = mutableMapOf<DeclarationID, StructDeclImpl>()

    override val structs: List<StructDecl>
        get() = structById.values.toList()

    private val enumById = mutableMapOf<DeclarationID, EnumDefImpl>()

    override val enums: List<EnumDef>
        get() = enumById.values.toList()

    private val objCClassesByName = mutableMapOf<String, ObjCClassImpl>()

    override val objCClasses: List<ObjCClass> get() = objCClassesByName.values.toList()

    private val objCProtocolsByName = mutableMapOf<String, ObjCProtocolImpl>()

    override val objCProtocols: List<ObjCProtocol> get() = objCProtocolsByName.values.toList()

    private val typedefById = mutableMapOf<DeclarationID, TypedefDef>()

    override val typedefs: List<TypedefDef>
        get() = typedefById.values.toList()

    private val functionById = mutableMapOf<DeclarationID, FunctionDecl>()

    override val functions: List<FunctionDecl>
        get() = functionById.values.toList()

    override val macroConstants = mutableListOf<ConstantDef>()

    private fun getDeclarationId(cursor: CValue<CXCursor>): DeclarationID {
        val usr = clang_getCursorUSR(cursor).convertAndDispose()
        if (usr == "") {
            val kind = cursor.kind
            val spelling = getCursorSpelling(cursor)
            return when (kind to spelling) {
                CXCursorKind.CXCursor_StructDecl to "__va_list_tag" -> DeclarationID.VaListTag
                CXCursorKind.CXCursor_TypedefDecl to "__builtin_va_list" -> DeclarationID.BuiltinVaList
                else -> error(spelling)
            }
        }

        return DeclarationID.USR(usr)
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

        val structDef = StructDefImpl(
                size, align, structDecl,
                hasNaturalLayout = structHasNaturalLayout(cursor)
        )

        structDecl.def = structDef

        addDeclaredFields(structDef, type, type)
    }

    private fun addDeclaredFields(structDef: StructDefImpl, structType: CValue<CXType>, containerType: CValue<CXType>) {
        getFields(containerType).forEach { fieldCursor ->
            val name = getCursorSpelling(fieldCursor)
            if (name.isNotEmpty()) {
                val fieldType = convertCursorType(fieldCursor)
                val offset = clang_Type_getOffsetOf(structType, name)
                if (clang_Cursor_isBitField(fieldCursor) == 0) {
                    val typeAlign = clang_Type_getAlignOf(clang_getCursorType(fieldCursor))
                    structDef.fields.add(Field(name, fieldType, offset, typeAlign))
                } else {
                    // Ignore bit fields for now.
                }
            } else {
                // Unnamed field.
                val fieldType = clang_getCursorType(fieldCursor)
                when (fieldType.kind) {
                    CXTypeKind.CXType_Record -> {
                        // Unnamed struct fields also contribute their fields:
                        addDeclaredFields(structDef, structType, fieldType)
                    }
                    else -> {
                        // Nothing.
                    }
                }
            }
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

            val baseType = convertType(clang_getEnumDeclIntegerType(cursor))

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

    private fun getObjCCategoryClassCursor(cursor: CValue<CXCursor>): CValue<CXCursor> {
        assert(cursor.kind == CXCursorKind.CXCursor_ObjCCategoryDecl)
        var classRef: CValue<CXCursor>? = null
        visitChildren(cursor) { child, _ ->
            if (child.kind == CXCursorKind.CXCursor_ObjCClassRef) {
                classRef = child
                CXChildVisitResult.CXChildVisit_Break
            } else {
                CXChildVisitResult.CXChildVisit_Continue
            }
        }

        return clang_getCursorReferenced(classRef!!).apply {
            assert(this.kind == CXCursorKind.CXCursor_ObjCInterfaceDecl)
        }
    }

    private fun getObjCClassAt(cursor: CValue<CXCursor>): ObjCClassImpl {
        assert(cursor.kind == CXCursorKind.CXCursor_ObjCInterfaceDecl) { cursor.kind }

        val name = clang_getCursorDisplayName(cursor).convertAndDispose()

        objCClassesByName[name]?.let { return it }

        val result = ObjCClassImpl(name)
        objCClassesByName[name] = result

        addChildrenToClassOrProtocol(cursor, result)
        return result

    }

    private fun getObjCProtocolAt(cursor: CValue<CXCursor>): ObjCProtocolImpl? {
        assert(cursor.kind == CXCursorKind.CXCursor_ObjCProtocolDecl) { cursor.kind }
        if (clang_isCursorDefinition(cursor) == 0) {
            val definition = clang_getCursorDefinition(cursor)
            if (clang_isCursorDefinition(cursor) == 0) return null
            return getObjCProtocolAt(definition)
        }

        val name = clang_getCursorDisplayName(cursor).convertAndDispose()

        objCProtocolsByName[name]?.let { return it }

        val result = ObjCProtocolImpl(name)
        objCProtocolsByName[name] = result

        addChildrenToClassOrProtocol(cursor, result)
        return result
    }

    private fun addChildrenToClassOrProtocol(cursor: CValue<CXCursor>, result: ObjCClassOrProtocolImpl) {
        visitChildren(cursor) { child, _ ->
            when (child.kind) {
                CXCursorKind.CXCursor_ObjCSuperClassRef -> {
                    assert(cursor.kind == CXCursorKind.CXCursor_ObjCInterfaceDecl)
                    result as ObjCClassImpl

                    assert(result.baseClass == null)
                    result.baseClass = getObjCClassAt(clang_getCursorReferenced(child))
                }
                CXCursorKind.CXCursor_ObjCProtocolRef -> {
                    getObjCProtocolAt(clang_getCursorReferenced(child))?.let {
                        if (it !in result.protocols) {
                            result.protocols.add(it)
                        }
                    }
                }
                CXCursorKind.CXCursor_ObjCClassMethodDecl, CXCursorKind.CXCursor_ObjCInstanceMethodDecl -> {
                    getObjCMethod(child)?.let { method ->
                        result.methods.removeAll { method.replaces(it) }
                        result.methods.add(method)
                    }
                }
                else -> {}
            }
            CXChildVisitResult.CXChildVisit_Continue
        }
    }

    fun getTypedef(type: CValue<CXType>): Type {
        val declCursor = clang_getTypeDeclaration(type)
        val name = getCursorSpelling(declCursor)

        val underlying = convertType(clang_getTypedefDeclUnderlyingType(declCursor))
        if (clang_getCursorLexicalParent(declCursor).kind != CXCursorKind.CXCursor_TranslationUnit) {
            // Objective-C type parameters are represented as non-top-level typedefs.
            // Erase for now:
            return underlying
        }

        if (library.language == Language.OBJECTIVE_C) {
            if (name == "BOOL" || name == "Boolean") {
                assert(clang_Type_getSizeOf(type) == 1L)
                return BoolType
            }

            if (underlying is ObjCPointer && (name == "Class" || name == "id") ||
                    underlying is PointerType && name == "SEL") {

                // Ignore implicit Objective-C typedefs:
                return underlying
            }
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
        convertType(clang_getCursorType(cursor), clang_getDeclTypeAttributes(cursor))

    private inline fun objCType(supplier: () -> ObjCPointer) = when (library.language) {
        Language.C -> UnsupportedType
        Language.OBJECTIVE_C -> supplier()
    }

    fun convertType(type: CValue<CXType>, typeAttributes: CValue<CXTypeAttributes>? = null): Type {
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

            CXType_Typedef -> {
                val declCursor = clang_getTypeDeclaration(type)
                val declSpelling = getCursorSpelling(declCursor)
                val underlying = convertType(clang_getTypedefDeclUnderlyingType(declCursor))
                when {
                    declSpelling == "instancetype" && underlying is ObjCPointer ->
                        ObjCInstanceType(getNullability(type, typeAttributes))

                    else -> getTypedef(type)
                }
            }

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

            CXType_ObjCObjectPointer -> objCType {
                val declaration = clang_getTypeDeclaration(clang_getPointeeType(type))
                val declarationKind = declaration.kind
                val nullability = getNullability(type, typeAttributes)
                when (declarationKind) {
                    CXCursorKind.CXCursor_NoDeclFound -> ObjCIdType(nullability, getProtocols(type))

                    CXCursorKind.CXCursor_ObjCInterfaceDecl ->
                        ObjCObjectPointer(getObjCClassAt(declaration), nullability, getProtocols(type))

                    else -> TODO(declarationKind.toString())
                }
            }

            CXType_ObjCId -> objCType { ObjCIdType(getNullability(type, typeAttributes), getProtocols(type)) }

            CXType_ObjCClass -> objCType { ObjCClassPointer(getNullability(type, typeAttributes), getProtocols(type)) }

            CXType_ObjCSel -> PointerType(VoidType)

            CXType_BlockPointer -> objCType { ObjCIdType(getNullability(type, typeAttributes), getProtocols(type)) }

            else -> UnsupportedType
        }
    }

    private fun getNullability(
            type: CValue<CXType>, typeAttributes: CValue<CXTypeAttributes>?
    ): ObjCPointer.Nullability {

        if (typeAttributes == null) return ObjCPointer.Nullability.Unspecified

        return when (clang_Type_getNullabilityKind(type, typeAttributes)) {
            CXNullabilityKind.CXNullabilityKind_Nullable -> ObjCPointer.Nullability.Nullable
            CXNullabilityKind.CXNullabilityKind_NonNull -> ObjCPointer.Nullability.NonNull
            CXNullabilityKind.CXNullabilityKind_Unspecified -> ObjCPointer.Nullability.Unspecified
        }
    }

    private fun getProtocols(type: CValue<CXType>): List<ObjCProtocol> {
        val num = clang_Type_getNumProtocols(type)
        return (0 until num).mapNotNull { index ->
            getObjCProtocolAt(clang_Type_getProtocol(type, index))
        }
    }

    private fun convertFunctionType(type: CValue<CXType>): Type {
        val kind = type.kind
        assert (kind == CXType_Unexposed || kind == CXType_FunctionProto)

        return if (clang_isFunctionTypeVariadic(type) != 0) {
            VoidType // make this function pointer opaque.
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
                if (entityName == null) {
                    // Skip anonymous struct.
                    // (It gets included anyway if used as a named field type).
                } else {
                    getStructDeclAt(cursor)
                }
            }

            CXIdxEntity_Typedef -> {
                val type = clang_getCursorType(cursor)
                getTypedef(type)
            }

            CXIdxEntity_Function -> {
                if (isAvailable(cursor)) {
                    functionById[getDeclarationId(cursor)] = getFunction(cursor)
                }
            }

            CXIdxEntity_Enum -> {
                getEnumDefAt(cursor)
            }

            CXIdxEntity_ObjCClass -> {
                if (isAvailable(cursor)) {
                    getObjCClassAt(clang_getCursorReferenced(cursor))
                }
            }

            CXIdxEntity_ObjCCategory -> {
                val classCursor = getObjCCategoryClassCursor(cursor)
                if (isAvailable(classCursor)) {
                    val objCClass = getObjCClassAt(classCursor)
                    addChildrenToClassOrProtocol(cursor, objCClass)
                }
            }

            CXIdxEntity_ObjCProtocol -> {
                if (isAvailable(cursor)) {
                    getObjCProtocolAt(cursor)
                }
            }

            CXIdxEntity_ObjCProperty -> {
                val container = clang_getCursorSemanticParent(cursor)
                if (isAvailable(cursor) && isAvailable(container)) {
                    val propertyInfo = clang_index_getObjCPropertyDeclInfo(info.ptr)!!.pointed
                    val getter = getObjCMethod(propertyInfo.getter!!.pointed.cursor.readValue())
                    val setter = propertyInfo.setter?.let {
                        getObjCMethod(it.pointed.cursor.readValue())
                    }

                    if (getter != null) {
                        val property = ObjCProperty(entityName!!, getter, setter)
                        val classOrProtocol: ObjCClassOrProtocolImpl? = when (container.kind) {
                            CXCursorKind.CXCursor_ObjCCategoryDecl -> {
                                val classCursor = getObjCCategoryClassCursor(container)
                                if (isAvailable(classCursor)) {
                                    getObjCClassAt(classCursor)
                                } else {
                                    null
                                }
                            }
                            CXCursorKind.CXCursor_ObjCInterfaceDecl -> getObjCClassAt(container)
                            CXCursorKind.CXCursor_ObjCProtocolDecl -> getObjCProtocolAt(container)!!
                            else -> error(container.kind)
                        }

                        if (classOrProtocol != null) {
                            classOrProtocol.properties.removeAll { property.replaces(it) }
                            classOrProtocol.properties.add(property)
                        }
                    }
                }
            }

            else -> {
                // Ignore declaration.
            }
        }
    }

    private fun getFunction(cursor: CValue<CXCursor>): FunctionDecl {
        val name = clang_getCursorSpelling(cursor).convertAndDispose()
        val returnType = convertType(clang_getCursorResultType(cursor), clang_getCursorResultTypeAttributes(cursor))

        val parameters = getFunctionParameters(cursor)

        val binaryName = when (library.language) {
            Language.C, Language.OBJECTIVE_C -> clang_Cursor_getMangling(cursor).convertAndDispose()
        }

        val definitionCursor = clang_getCursorDefinition(cursor)
        val isDefined = (clang_Cursor_isNull(definitionCursor) == 0)

        val isVararg = clang_Cursor_isVariadic(cursor) != 0

        return FunctionDecl(name, parameters, returnType, binaryName, isDefined, isVararg)
    }

    private fun getObjCMethod(cursor: CValue<CXCursor>): ObjCMethod? {
        if (!isAvailable(cursor)) {
            return null
        }

        val selector = clang_getCursorDisplayName(cursor).convertAndDispose()

        // Ignore some very special methods:
        when (selector) {
            "dealloc", "retain", "release", "autorelease", "retainCount", "self" -> return null
        }

        val encoding = clang_getDeclObjCTypeEncoding(cursor).convertAndDispose()
        val returnType = convertType(clang_getCursorResultType(cursor), clang_getCursorResultTypeAttributes(cursor))
        val parameters = getFunctionParameters(cursor)

        val isClass = when (cursor.kind) {
            CXCursorKind.CXCursor_ObjCClassMethodDecl -> true
            CXCursorKind.CXCursor_ObjCInstanceMethodDecl -> false
            else -> error(cursor.kind)
        }

        return ObjCMethod(selector, encoding, parameters, returnType,
                isClass = isClass,
                nsConsumesSelf = hasAttribute(cursor, NS_CONSUMES_SELF),
                nsReturnsRetained = hasAttribute(cursor, NS_RETURNS_RETAINED),
                isOptional = (clang_Cursor_isObjCOptional(cursor) != 0),
                isInit = (clang_Cursor_isObjCInitMethod(cursor) != 0))
    }

    // TODO: unavailable declarations should be imported as deprecated.
    private fun isAvailable(cursor: CValue<CXCursor>): Boolean = when (clang_getCursorAvailability(cursor)) {
        CXAvailabilityKind.CXAvailability_Available,
        CXAvailabilityKind.CXAvailability_Deprecated -> true

        CXAvailabilityKind.CXAvailability_NotAvailable,
        CXAvailabilityKind.CXAvailability_NotAccessible -> false
    }

    private fun getFunctionParameters(cursor: CValue<CXCursor>): List<Parameter> {
        val argNum = clang_Cursor_getNumArguments(cursor)
        val args = (0..argNum - 1).map {
            val argCursor = clang_Cursor_getArgument(cursor, it)
            val argName = getCursorSpelling(argCursor)
            val type = convertCursorType(argCursor)
            Parameter(argName, type,
                    nsConsumed = hasAttribute(argCursor, NS_CONSUMED))
        }
        return args
    }

    private val NS_CONSUMED = "ns_consumed"
    private val NS_CONSUMES_SELF = "ns_consumes_self"
    private val NS_RETURNS_RETAINED = "ns_returns_retained"

    private fun hasAttribute(cursor: CValue<CXCursor>, name: String): Boolean {
        var result = false
        visitChildren(cursor) { child, _ ->
            if (clang_isAttribute(child.kind) != 0 && clang_Cursor_getAttributeSpelling(child)?.toKString() == name) {
                result = true
                CXChildVisitResult.CXChildVisit_Break
            } else {
                CXChildVisitResult.CXChildVisit_Continue
            }
        }
        return result
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
