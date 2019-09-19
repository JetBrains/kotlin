/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import java.lang.StringBuilder

fun List<Proto>.createSimpleDeserializer(): String {

    val sb = StringBuilder()

    sb.appendln("class SimpleIrProtoReader(source: ByteArray) : ProtoReader(source) {")

    val typeMap = this.associate { it.name to it }

    filterIsInstance<Proto.Message>().forEach { sb.addMessage(it, typeMap) }

    sb.appendln("}")

    return sb.toString()
}

private fun String.toReaderInvocation(typeMap: Map<String, Proto>): String {
    return typeMap.get(this)?.let {
        when (it) {
            is Proto.Enum -> "readInt32()"
            is Proto.Message -> "readWithLength { read${it.name}() }"
        }
    } ?: run {
        when (this) {
            "int32" -> "readInt32()"
            "int64", "uint64" -> "readInt64()"
            "string" -> "readString()"
            "bool" -> "readBool()"
            "float" -> "readFloat()"
            "double" -> "readDouble()"
            else -> error("Unknown type: ${this}")
        }
    }
}

private fun StringBuilder.addMessage(m: Proto.Message, typeMap: Map<String, Proto>) {
    val allFields = m.fields.flatMap {
        when (it) {
            is MessageEntry.OneOf -> it.fields
            is MessageEntry.Field -> listOf(it)
        }
    }

    appendln("    fun read${m.name}(): Any {")

    allFields.forEach { f ->
        val (type, initExpression) = if (f.kind == FieldKind.REPEATED) {
            "MutableList<Any>" to "mutableListOf<Any>()"
        } else {
            "Any?" to "null"
        }
        appendln("        var ${f.name}__: $type = $initExpression")
    }

    appendln("        while (hasData) {")
    appendln("            readField { fieldNumber, type -> ")
    appendln("                when (fieldNumber) {")

    val indent = "                    "
    allFields.forEach { f ->
        val readExpression = f.type.toReaderInvocation(typeMap)
        if (f.kind == FieldKind.REPEATED) {
            appendln("${indent}${f.index} -> ${f.name}__.add($readExpression)")
        } else {
            appendln("${indent}${f.index} -> ${f.name}__ = $readExpression")
        }
    }

    appendln("                    else -> skip(type)")
    appendln("                }")
    appendln("            }")
    appendln("        }")

    appendln("        return arrayOf(${allFields.fold("") { acc, c -> (if (acc.isEmpty()) "" else "$acc, ") + c.name + "__" }})")

    appendln("    }")
    appendln()
}