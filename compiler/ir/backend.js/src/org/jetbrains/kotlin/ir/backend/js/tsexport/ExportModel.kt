/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.js.ModuleKind

sealed class ExportedDeclaration {
    val attributes = mutableListOf<ExportedAttribute>()
}

sealed class ExportedAttribute {
    class DeprecatedAttribute(val message: String): ExportedAttribute()
}

data class ExportedModule(
    val name: String,
    val moduleKind: ModuleKind,
    val declarations: List<ExportedDeclaration>
)

class ExportedNamespace(
    val name: String,
    val declarations: List<ExportedDeclaration>,
    val isPrivate: Boolean = false
) : ExportedDeclaration()

data class ExportedFunction(
    val name: String,
    val returnType: ExportedType,
    val parameters: List<ExportedParameter>,
    val typeParameters: List<ExportedType.TypeParameter> = emptyList(),
    val isMember: Boolean = false,
    val isStatic: Boolean = false,
    val isAbstract: Boolean = false,
    val isProtected: Boolean,
) : ExportedDeclaration()

data class ExportedConstructor(
    val parameters: List<ExportedParameter>,
    val visibility: ExportedVisibility
) : ExportedDeclaration() {
    val isProtected: Boolean
        get() = visibility == ExportedVisibility.PROTECTED
}

data class ExportedConstructSignature(
    val parameters: List<ExportedParameter>,
    val returnType: ExportedType,
) : ExportedDeclaration()

data class ExportedProperty(
    val name: String,
    val type: ExportedType,
    val mutable: Boolean = true,
    val isMember: Boolean = false,
    val isStatic: Boolean = false,
    val isAbstract: Boolean = false,
    val isProtected: Boolean = false,
    val isField: Boolean = false,
    val isObjectGetter: Boolean = false,
    val isOptional: Boolean = false,
    val isQualified: Boolean = false,
) : ExportedDeclaration()

// TODO: Cover all cases with frontend and disable error declarations
class ErrorDeclaration(val message: String) : ExportedDeclaration()


sealed class ExportedClass : ExportedDeclaration() {
    abstract val name: String
    abstract val members: List<ExportedDeclaration>
    abstract val superClasses: List<ExportedType>
    abstract val superInterfaces: List<ExportedType>
    abstract val nestedClasses: List<ExportedClass>
    abstract val originalClassId: ClassId?
    abstract val isCompanion: Boolean
    abstract val isExternal: Boolean
}

data class ExportedRegularClass(
    override val name: String,
    val isInterface: Boolean = false,
    val isAbstract: Boolean = false,
    val requireMetadata: Boolean = !isInterface,
    override val superClasses: List<ExportedType> = emptyList(),
    override val superInterfaces: List<ExportedType> = emptyList(),
    val typeParameters: List<ExportedType.TypeParameter>,
    override val members: List<ExportedDeclaration>,
    override val nestedClasses: List<ExportedClass>,
    override val originalClassId: ClassId?,
    val innerClassReference: String? = null,
    override val isExternal: Boolean,
) : ExportedClass() {
    override val isCompanion: Boolean
        get() = false
}

data class ExportedObject(
    override val name: String,
    override val superClasses: List<ExportedType> = emptyList(),
    override val superInterfaces: List<ExportedType> = emptyList(),
    override val members: List<ExportedDeclaration>,
    override val nestedClasses: List<ExportedClass>,
    val typeParameters: List<ExportedType.TypeParameter> = emptyList(),
    override val originalClassId: ClassId?,
    override val isExternal: Boolean,
    override val isCompanion: Boolean,
    val isInsideInterface: Boolean,
) : ExportedClass()

class ExportedParameter(
    val name: String,
    val type: ExportedType,
    val hasDefaultValue: Boolean = false
)

sealed class ExportedType {
    open fun replaceTypes(substitution: Map<ExportedType, ExportedType>): ExportedType =
        substitution[this] ?: this

    sealed class Primitive(val typescript: kotlin.String) : ExportedType() {
        object Boolean : Primitive("boolean")
        object Number : Primitive("number")
        object BigInt : Primitive("bigint")
        object ByteArray : Primitive("Int8Array")
        object ShortArray : Primitive("Int16Array")
        object IntArray : Primitive("Int32Array")
        object FloatArray : Primitive("Float32Array")
        object DoubleArray : Primitive("Float64Array")
        object LongArray : Primitive("BigInt64Array")
        object String : Primitive("string")
        object Throwable : Primitive("Error")
        object Any : Primitive("any")
        object Undefined : Primitive("undefined")
        object Unit : Primitive("void")
        object Nothing : Primitive("never")
        object UniqueSymbol : Primitive("unique symbol")
        object Unknown : Primitive("unknown") {
            override fun withNullability(nullable: kotlin.Boolean) =
                if (nullable) this else NonNullable(this)
        }
    }

    sealed class LiteralType<T : Any>(val value: T) : ExportedType() {
        class StringLiteralType(value: String) : LiteralType<String>(value)
        class NumberLiteralType(value: Number) : LiteralType<Number>(value)
    }

    data class Array(val elementType: ExportedType) : ExportedType() {
        override fun replaceTypes(substitution: Map<ExportedType, ExportedType>): ExportedType =
            substitution[this] ?: Array(elementType.replaceTypes(substitution))
    }

    class Function(
        val parameterTypes: List<ExportedType>,
        val returnType: ExportedType
    ) : ExportedType()

    class ConstructorType(
        val typeParameters: List<TypeParameter>,
        val returnType: ExportedType
    ) : ExportedType()

    data class ClassType(
        val name: String,
        val arguments: List<ExportedType>,
        val isObject: Boolean = false,
        val isExternal: Boolean = false,
        val classId: ClassId? = null,
    ) : ExportedType() {
        override fun equals(other: Any?) = this === other || other is ClassType && classId == other.classId
        override fun hashCode() = classId.hashCode()

        override fun replaceTypes(substitution: Map<ExportedType, ExportedType>) =
            substitution[this] ?: copy(arguments = arguments.map { it.replaceTypes(substitution) })
    }

    data class TypeParameter(val name: String, val constraint: ExportedType? = null) : ExportedType()
    class Nullable(val baseType: ExportedType) : ExportedType()
    class NonNullable(val baseType: ExportedType) : ExportedType()
    class ErrorType(val comment: String) : ExportedType()
    data class TypeOf(val classType: ClassType) : ExportedType()
    class ObjectsParentType(val constructor: ExportedType) : ExportedType()

    class InlineInterfaceType(
        val members: List<ExportedDeclaration>
    ) : ExportedType()

    class UnionType(val lhs: ExportedType, val rhs: ExportedType) : ExportedType()

    class IntersectionType(val lhs: ExportedType, val rhs: ExportedType) : ExportedType()

    class PropertyType(val container: ExportedType, val propertyName: ExportedType) : ExportedType()

    data class ImplicitlyExportedType(val type: ExportedType, val exportedSupertype: ExportedType) : ExportedType() {
        override fun withNullability(nullable: Boolean) =
            ImplicitlyExportedType(type.withNullability(nullable), exportedSupertype.withNullability(nullable))
    }

    open fun withNullability(nullable: Boolean) =
        if (nullable) Nullable(this) else this

    fun withImplicitlyExported(implicitlyExportedType: Boolean, exportedSupertype: ExportedType) =
        if (implicitlyExportedType) ImplicitlyExportedType(this, exportedSupertype) else this
}

enum class ExportedVisibility(val keyword: String) {
    DEFAULT(""),
    PRIVATE("private "),
    PROTECTED("protected ")
}
