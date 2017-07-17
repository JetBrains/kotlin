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

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.*

interface DeclarationMapper {
    fun getKotlinNameForPointed(structDecl: StructDecl): String
    fun isMappedToStrict(enumDef: EnumDef): Boolean
    fun getKotlinNameForValue(enumDef: EnumDef): String
}

val PrimitiveType.kotlinType: String
    get() = when (this) {
        is CharType -> "Byte"

        is BoolType -> "Boolean"

    // TODO: C primitive types should probably be generated as type aliases for Kotlin types.
        is IntegerType -> when (this.size) {
            1 -> "Byte"
            2 -> "Short"
            4 -> "Int"
            8 -> "Long"
            else -> TODO(this.toString())
        }

        is FloatingType -> when (this.size) {
            4 -> "Float"
            8 -> "Double"
            else -> TODO(this.toString())
        }

        else -> throw NotImplementedError()
    }

private val PrimitiveType.bridgedType: BridgedType
    get() {
        val kotlinType = this.kotlinType
        return BridgedType.values().single {
            it.kotlinType == kotlinType
        }
    }

private val ObjCPointer.isNullable: Boolean
    get() = this.nullability != ObjCPointer.Nullability.NonNull

/**
 * Describes the Kotlin types used to represent some C type.
 */
sealed class TypeMirror(val pointedTypeName: String, val info: TypeInfo) {
    /**
     * Type to be used in bindings for argument or return value.
     */
    abstract val argType: String

    /**
     * Mirror for C type to be represented in Kotlin as by-value type.
     */
    class ByValue(pointedTypeName: String, info: TypeInfo, val valueTypeName: String) :
            TypeMirror(pointedTypeName, info) {

        override val argType: String
            get() = valueTypeName +
                    if (info is TypeInfo.Pointer ||
                            (info is TypeInfo.ObjCPointerInfo && info.type.isNullable)) "?" else ""
    }

    /**
     * Mirror for C type to be represented in Kotlin as by-ref type.
     */
    class ByRef(pointedTypeName: String, info: TypeInfo) : TypeMirror(pointedTypeName, info) {
        override val argType: String
            get() = "CValue<$pointedTypeName>"
    }
}

/**
 * Describes various type conversions for [TypeMirror].
 */
sealed class TypeInfo {
    /**
     * The conversion from [TypeMirror.argType] to [bridgedType].
     */
    abstract fun argToBridged(name: String): String

    /**
     * The conversion from [bridgedType] to [TypeMirror.argType].
     */
    abstract fun argFromBridged(name: String): String

    abstract val bridgedType: BridgedType

    open fun cFromBridged(name: String): String = name

    open fun cToBridged(name: String): String = name

    /**
     * If this info is for [TypeMirror.ByValue], then this method describes how to
     * construct pointed-type from value type.
     */
    abstract fun constructPointedType(valueType: String): String

    class Primitive(override val bridgedType: BridgedType, val varTypeName: String) : TypeInfo() {

        override fun argToBridged(name: String) = name
        override fun argFromBridged(name: String) = name

        override fun constructPointedType(valueType: String) = "${varTypeName}Of<$valueType>"
    }

    class Boolean : TypeInfo() {
        override fun argToBridged(name: String) = "$name.toByte()"

        override fun argFromBridged(name: String) = "$name.toBoolean()"

        override val bridgedType: BridgedType get() = BridgedType.BYTE

        override fun cFromBridged(name: String) = "($name) ? 1 : 0"

        override fun cToBridged(name: String) = "($name) ? 1 : 0"

        override fun constructPointedType(valueType: String) = "BooleanVarOf<$valueType>"
    }

    class Enum(val className: String, override val bridgedType: BridgedType) : TypeInfo() {
        override fun argToBridged(name: String) = "$name.value"

        override fun argFromBridged(name: String) = "$className.byValue($name)"

        override fun constructPointedType(valueType: String) = "$className.Var" // TODO: improve

    }

    class Pointer(val pointee: String) : TypeInfo() {
        override fun argToBridged(name: String) = "$name.rawValue"

        override fun argFromBridged(name: String) = "interpretCPointer<$pointee>($name)"

        override val bridgedType: BridgedType
            get() = BridgedType.NATIVE_PTR

        override fun cFromBridged(name: String) = "(void*)$name" // Note: required for JVM

        override fun constructPointedType(valueType: String) = "CPointerVarOf<$valueType>"
    }

    class ObjCPointerInfo(val typeName: String, val type: ObjCPointer) : TypeInfo() {
        override fun argToBridged(name: String) = "$name.rawPtr"

        override fun argFromBridged(name: String) = "interpretObjCPointerOrNull<$typeName>($name)" +
                if (type.isNullable) "" else "!!"

        override val bridgedType: BridgedType
            get() = BridgedType.OBJC_POINTER

        override fun constructPointedType(valueType: String) = "ObjCObjectVar<$valueType>"
    }

    class NSString(val type: ObjCPointer) : TypeInfo() {
        override fun argToBridged(name: String) = "CreateNSStringFromKString($name)"

        override fun argFromBridged(name: String) = "CreateKStringFromNSString($name)" +
                if (type.isNullable) "" else "!!"

        override val bridgedType: BridgedType
            get() = BridgedType.OBJC_POINTER

        override fun constructPointedType(valueType: String): String {
            return "ObjCStringVarOf<$valueType>"
        }
    }

    class ByRef(val pointed: String) : TypeInfo() {
        override fun argToBridged(name: String) = error(pointed)
        override fun argFromBridged(name: String) = error(pointed)
        override val bridgedType: BridgedType get() = error(pointed)
        override fun cFromBridged(name: String) = error(pointed)
        override fun cToBridged(name: String) = error(pointed)

        // TODO: this method must not exist
        override fun constructPointedType(valueType: String): String = error(pointed)
    }
}

fun mirrorPrimitiveType(type: PrimitiveType): TypeMirror.ByValue {
    val varTypeName = when (type) {
        is CharType -> "ByteVar"
        is BoolType -> "BooleanVar"
        is IntegerType -> when (type.size) {
            1 -> "ByteVar"
            2 -> "ShortVar"
            4 -> "IntVar"
            8 -> "LongVar"
            else -> TODO(type.toString())
        }
        is FloatingType -> when (type.size) {
            4 -> "FloatVar"
            8 -> "DoubleVar"
            else -> TODO(type.toString())
        }
        else -> TODO(type.toString())
    }

    val info = if (type == BoolType) {
        TypeInfo.Boolean()
    } else {
        TypeInfo.Primitive(type.bridgedType, varTypeName)
    }
    return TypeMirror.ByValue(varTypeName, info, type.kotlinType)
}

private fun byRefTypeMirror(pointedTypeName: String) : TypeMirror.ByRef {
    val info = TypeInfo.ByRef(pointedTypeName)
    return TypeMirror.ByRef(pointedTypeName, info)
}

fun mirror(declarationMapper: DeclarationMapper, type: Type): TypeMirror = when (type) {
    is PrimitiveType -> mirrorPrimitiveType(type)

    is RecordType -> byRefTypeMirror(declarationMapper.getKotlinNameForPointed(type.decl).asSimpleName())

    is EnumType -> {
        val kotlinName = declarationMapper.getKotlinNameForValue(type.def)

        when {
            declarationMapper.isMappedToStrict(type.def) -> {
                val classSimpleName = kotlinName.asSimpleName()
                val bridgedType = (type.def.baseType.unwrapTypedefs() as PrimitiveType).bridgedType
                val info = TypeInfo.Enum(classSimpleName, bridgedType)
                TypeMirror.ByValue("$classSimpleName.Var", info, classSimpleName)
            }
            !type.def.isAnonymous -> {
                val baseTypeMirror = mirror(declarationMapper, type.def.baseType)
                TypeMirror.ByValue("${kotlinName}Var", baseTypeMirror.info, kotlinName.asSimpleName())
            }
            else -> mirror(declarationMapper, type.def.baseType)
        }
    }

    is PointerType -> {
        val pointeeType = type.pointeeType
        val unwrappedPointeeType = pointeeType.unwrapTypedefs()
        if (unwrappedPointeeType is VoidType) {
            val info = TypeInfo.Pointer("COpaque")
            TypeMirror.ByValue("COpaquePointerVar", info, "COpaquePointer")
        } else if (unwrappedPointeeType is ArrayType) {
            mirror(declarationMapper, pointeeType)
        } else {
            val pointeeMirror = mirror(declarationMapper, pointeeType)
            val info = TypeInfo.Pointer(pointeeMirror.pointedTypeName)
            TypeMirror.ByValue("CPointerVar<${pointeeMirror.pointedTypeName}>", info,
                    "CPointer<${pointeeMirror.pointedTypeName}>")
        }
    }

    is ArrayType -> {
        // TODO: array type doesn't exactly correspond neither to pointer nor to value.
        val elemTypeMirror = mirror(declarationMapper, type.elemType)
        if (type.elemType.unwrapTypedefs() is ArrayType) {
            elemTypeMirror
        } else {
            val info = TypeInfo.Pointer(elemTypeMirror.pointedTypeName)
            TypeMirror.ByValue("CArrayPointerVar<${elemTypeMirror.pointedTypeName}>", info,
                    "CArrayPointer<${elemTypeMirror.pointedTypeName}>")
        }
    }

    is FunctionType -> byRefTypeMirror("CFunction<${getKotlinFunctionType(declarationMapper, type)}>")

    is Typedef -> {
        val baseType = mirror(declarationMapper, type.def.aliased)
        val name = type.def.name
        when (baseType) {
            is TypeMirror.ByValue -> TypeMirror.ByValue("${name}Var", baseType.info, name.asSimpleName())
            is TypeMirror.ByRef -> TypeMirror.ByRef(name.asSimpleName(), baseType.info)
        }

    }

    is ObjCPointer -> objCPointerMirror(type)

    else -> TODO(type.toString())
}

private fun objCPointerMirror(type: ObjCPointer): TypeMirror.ByValue {
    if (type is ObjCObjectPointer && type.def.name == "NSString") {
        val info = TypeInfo.NSString(type)
        val valueType = if (type.isNullable) "String?" else "String"
        return TypeMirror.ByValue(info.constructPointedType(valueType), info, valueType)
    }

    val typeName = when (type) {
        is ObjCIdType -> type.protocols.firstOrNull()?.kotlinName ?: "ObjCObject"
        is ObjCClassPointer -> "ObjCClass"
        is ObjCObjectPointer -> type.def.name
        is ObjCInstanceType -> TODO(type.toString()) // Must have already been handled.
    }

    return objCPointerMirror(typeName.asSimpleName(), type)
}

private fun objCPointerMirror(typeName: String, type: ObjCPointer): TypeMirror.ByValue {
    val valueType = if (type.isNullable) "$typeName?" else typeName
    return TypeMirror.ByValue("ObjCObjectVar<$valueType>",
            TypeInfo.ObjCPointerInfo(typeName, type), typeName)
}

fun getKotlinFunctionType(declarationMapper: DeclarationMapper, type: FunctionType): String {
    val returnType = if (type.returnType.unwrapTypedefs() is VoidType) {
        "Unit"
    } else {
        mirror(declarationMapper, type.returnType).argType
    }
    return "(" +
            type.parameterTypes.map { mirror(declarationMapper, it).argType }.joinToString(", ") +
            ") -> " +
            returnType
}

