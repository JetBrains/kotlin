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

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.ValueType

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
        val returnType: ObjCReferenceType,
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

internal enum class ObjCValueType(
        val kotlinValueType: ValueType, // It is here for simplicity.
        val encoding: String
) {

    BOOL(ValueType.BOOLEAN, "c"),
    CHAR(ValueType.BYTE, "c"),
    UNSIGNED_SHORT(ValueType.CHAR, "S"),
    SHORT(ValueType.SHORT, "s"),
    INT(ValueType.INT, "i"),
    LONG_LONG(ValueType.LONG, "q"),
    FLOAT(ValueType.FLOAT, "f"),
    DOUBLE(ValueType.DOUBLE, "d")

    ;

    // UNSIGNED_SHORT -> unsignedShort
    val nsNumberName = this.name.split('_').mapIndexed { index, s ->
        val lower = s.toLowerCase()
        if (index > 0) lower.capitalize() else lower
    }.joinToString("")

    val nsNumberValueSelector get() = "${nsNumberName}Value"
    val nsNumberFactorySelector get() = "numberWith${nsNumberName.capitalize()}:"
}
