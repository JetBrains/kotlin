/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.ir.declarations.*

sealed class ExportedDeclaration

data class ExportedModule(
    val name: String,
    val declarations: List<ExportedDeclaration>
)

class ExportedNamespace(
    val name: String,
    val declarations: List<ExportedDeclaration>
) : ExportedDeclaration()

class ExportedFunction(
    val name: String,
    val returnType: ExportedType,
    val parameters: List<ExportedParameter>,
    val typeParameters: List<String> = emptyList(),
    val isMember: Boolean = false,
    val isStatic: Boolean = false,
    val isAbstract: Boolean = false,
    val ir: IrSimpleFunction
) : ExportedDeclaration()

class ExportedConstructor(
    val parameters: List<ExportedParameter>
) : ExportedDeclaration()

class ExportedProperty(
    val name: String,
    val type: ExportedType,
    val mutable: Boolean,
    val isMember: Boolean = false,
    val isStatic: Boolean = false,
    val isAbstract: Boolean,
    val ir: IrProperty
) : ExportedDeclaration()


// TODO: Cover all cases with frontend and disable error declarations
class ErrorDeclaration(val message: String) : ExportedDeclaration()

class ExportedClass(
    val name: String,
    val isInterface: Boolean = false,
    val isAbstract: Boolean = false,
    val superClass: ExportedType? = null,
    val superInterfaces: List<ExportedType> = emptyList(),
    val typeParameters: List<String>,
    val members: List<ExportedDeclaration>,
    val statics: List<ExportedDeclaration>,
    val ir: IrClass
) : ExportedDeclaration()

class ExportedParameter(
    val name: String,
    val type: ExportedType
)

sealed class ExportedType {
    sealed class Primitive(val typescript: kotlin.String) : ExportedType() {
        object Boolean : Primitive("boolean")
        object Number : Primitive("number")
        object ByteArray : Primitive("Int8Array")
        object ShortArray : Primitive("Int16Array")
        object IntArray : Primitive("Int32Array")
        object FloatArray : Primitive("Float32Array")
        object DoubleArray : Primitive("Float64Array")
        object String : Primitive("string")
        object Throwable : Primitive("Error")
        object Any : Primitive("any")
        object Unit : Primitive("void")
        object Nothing : Primitive("never")
    }

    class Array(val elementType: ExportedType) : ExportedType()
    class Function(
        val parameterTypes: List<ExportedType>,
        val returnType: ExportedType
    ) : ExportedType()

    class ClassType(val name: String, val arguments: List<ExportedType>) : ExportedType()
    class TypeParameter(val name: String) : ExportedType()
    class Nullable(val baseType: ExportedType) : ExportedType()
    class ErrorType(val comment: String) : ExportedType()

    fun withNullability(nullable: Boolean) =
        if (nullable) Nullable(this) else this
}
