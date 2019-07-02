/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor

sealed class ObjCType {
    final override fun toString(): String = this.render()

    abstract fun render(attrsAndName: String): String

    fun render() = render("")

    protected fun String.withAttrsAndName(attrsAndName: String) =
            if (attrsAndName.isEmpty()) this else "$this ${attrsAndName.trimStart()}"
}

class ObjCRawType(
        val rawText: String
) : ObjCType() {
    override fun render(attrsAndName: String): String = rawText.withAttrsAndName(attrsAndName)
}

sealed class ObjCReferenceType : ObjCType()

sealed class ObjCNonNullReferenceType : ObjCReferenceType()

data class ObjCNullableReferenceType(
        val nonNullType: ObjCNonNullReferenceType
) : ObjCReferenceType() {
    override fun render(attrsAndName: String) = nonNullType.render(" _Nullable".withAttrsAndName(attrsAndName))
}

class ObjCClassType(
        val className: String,
        val typeArguments: List<ObjCNonNullReferenceType> = emptyList()
) : ObjCNonNullReferenceType() {

    override fun render(attrsAndName: String) = buildString {
        append(className)
        if (typeArguments.isNotEmpty()) {
            append("<")
            typeArguments.joinTo(this) { it.render() }
            append(">")
        }
        append(" *")
        append(attrsAndName)
    }
}

class ObjCGenericTypeDeclaration(
        val typeParameterDescriptor: TypeParameterDescriptor,
        val namer: ObjCExportNamer
) : ObjCNonNullReferenceType() {
    override fun render(attrsAndName: String): String {
        return namer.getTypeParameterName(typeParameterDescriptor).withAttrsAndName(attrsAndName)
    }
}

class ObjCProtocolType(
        val protocolName: String
) : ObjCNonNullReferenceType() {
    override fun render(attrsAndName: String) = "id<$protocolName>".withAttrsAndName(attrsAndName)
}

object ObjCIdType : ObjCNonNullReferenceType() {
    override fun render(attrsAndName: String) = "id".withAttrsAndName(attrsAndName)
}

object ObjCInstanceType : ObjCNonNullReferenceType() {
    override fun render(attrsAndName: String): String = "instancetype".withAttrsAndName(attrsAndName)
}

class ObjCBlockPointerType(
        val returnType: ObjCType,
        val parameterTypes: List<ObjCReferenceType>
) : ObjCNonNullReferenceType() {

    override fun render(attrsAndName: String) = returnType.render(buildString {
        append("(^")
        append(attrsAndName)
        append(")(")
        if (parameterTypes.isEmpty()) append("void")
        parameterTypes.joinTo(this) { it.render() }
        append(')')
    })
}

object ObjCMetaClassType : ObjCNonNullReferenceType() {
    override fun render(attrsAndName: String): String = "Class".withAttrsAndName(attrsAndName)
}

class ObjCPrimitiveType(
        val cName: String
) : ObjCType() {
    override fun render(attrsAndName: String) = cName.withAttrsAndName(attrsAndName)
}

class ObjCPointerType(
        val pointee: ObjCType,
        val nullable: Boolean = false
) : ObjCType() {
    override fun render(attrsAndName: String) =
            pointee.render("*${if (nullable) {
                " _Nullable".withAttrsAndName(attrsAndName)
            } else {
                attrsAndName
            }}")
}

object ObjCVoidType : ObjCType() {
    override fun render(attrsAndName: String) = "void".withAttrsAndName(attrsAndName)
}

internal enum class ObjCValueType(val encoding: String) {
    BOOL("c"),
    UNICHAR("S"),
    CHAR("c"),
    SHORT("s"),
    INT("i"),
    LONG_LONG("q"),
    UNSIGNED_CHAR("C"),
    UNSIGNED_SHORT("S"),
    UNSIGNED_INT("I"),
    UNSIGNED_LONG_LONG("Q"),
    FLOAT("f"),
    DOUBLE("d"),
    POINTER("^v")
}

internal fun ObjCType.makeNullableIfReferenceOrPointer(): ObjCType = when (this) {
    is ObjCPointerType -> ObjCPointerType(this.pointee, nullable = true)

    is ObjCNonNullReferenceType -> ObjCNullableReferenceType(this)

    is ObjCNullableReferenceType, is ObjCRawType, is ObjCPrimitiveType, ObjCVoidType -> this
}
