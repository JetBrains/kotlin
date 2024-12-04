/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
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
    val ir: IrSimpleFunction
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
    val irGetter: IrFunction? = null,
    val irSetter: IrFunction? = null,
    val isOptional: Boolean = false
) : ExportedDeclaration()

// TODO: Cover all cases with frontend and disable error declarations
class ErrorDeclaration(val message: String) : ExportedDeclaration()


sealed class ExportedClass : ExportedDeclaration() {
    abstract val name: String
    abstract val ir: IrClass
    abstract val members: List<ExportedDeclaration>
    abstract val superClasses: List<ExportedType>
    abstract val superInterfaces: List<ExportedType>
    abstract val nestedClasses: List<ExportedClass>
}

data class ExportedRegularClass(
    override val name: String,
    val isInterface: Boolean = false,
    val isAbstract: Boolean = false,
    override val superClasses: List<ExportedType> = emptyList(),
    override val superInterfaces: List<ExportedType> = emptyList(),
    val typeParameters: List<ExportedType.TypeParameter>,
    override val members: List<ExportedDeclaration>,
    override val nestedClasses: List<ExportedClass>,
    override val ir: IrClass,
) : ExportedClass()

data class ExportedObject(
    override val name: String,
    override val superClasses: List<ExportedType> = emptyList(),
    override val superInterfaces: List<ExportedType> = emptyList(),
    override val members: List<ExportedDeclaration>,
    override val nestedClasses: List<ExportedClass>,
    override val ir: IrClass,
    val irGetter: IrSimpleFunction? = null
) : ExportedClass()

class ExportedParameter(
    val name: String,
    val type: ExportedType,
    val hasDefaultValue: Boolean = false
)

sealed class ExportedType {
    sealed class Primitive(val typescript: kotlin.String) : ExportedType() {
        object Boolean : Primitive("boolean")
        object Number : Primitive("number")
        object BigInt : Primitive("bigint")
        object ByteArray : Primitive("Int8Array")
        object ShortArray : Primitive("Int16Array")
        object IntArray : Primitive("Int32Array")
        object FloatArray : Primitive("Float32Array")
        object DoubleArray : Primitive("Float64Array")
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

    class Array(val elementType: ExportedType) : ExportedType()
    class Function(
        val parameterTypes: List<ExportedType>,
        val returnType: ExportedType
    ) : ExportedType()

    class ClassType(val name: String, val arguments: List<ExportedType>, val ir: IrClass) : ExportedType()
    class TypeParameter(val name: String, val constraint: ExportedType? = null) : ExportedType()
    class Nullable(val baseType: ExportedType) : ExportedType()
    class NonNullable(val baseType: ExportedType) : ExportedType()
    class ErrorType(val comment: String) : ExportedType()
    class TypeOf(val name: String) : ExportedType()

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
