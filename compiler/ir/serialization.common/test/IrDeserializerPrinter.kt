/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import java.lang.StringBuilder

fun List<Proto>.createIrDeserializer(classMap: Map<String, String>, isSimple: Boolean): String {
    return IrDeserializerPrinter(this, classMap, isSimple).buildDeclaration()
}

private class IrDeserializerPrinter(
    val protoList: List<Proto>,
    importMap_: Map<String, String>,
    val isSimple: Boolean
) {

    val importMap = if (!isSimple) importMap_ else importMap_.keys.associate { it to "kotlin.Any" }

    val typeMap: Map<String, Proto> = protoList.associate { it.name to it }

    private fun String.typeParameters(): String {
        val index = indexOf('<')
        return if (index < 0) "" else substring(index)
    }

    private fun String.noTypeParameters(): String {
        val index = indexOf('<')
        return if (index < 0) this else substring(0, index)
    }

    private val String.isBuiltin: Boolean
        get() = this.startsWith("kotlin.")

    val classMap: Map<String, String> = importMap.entries.associate { (k, v) ->
        k to if (v.isBuiltin) {
            v.noTypeParameters().let {
                it.substring(it.lastIndexOf('.') + 1)
            } + v.typeParameters()
        } else {
            k + "MessageType" + v.typeParameters()
        }
    } + mapOf(
        "int32" to "Int",
        "int64" to "Long",
        "uint64" to "Long",
        "string" to "String",
        "bool" to "Boolean",
        "float" to "Float",
        "double" to "Double"
    )

    fun buildDeclaration(): String {

        val sb = StringBuilder()

        importMap.entries.map { (k, v) ->
            if (!v.isBuiltin) {
                "import ${v.noTypeParameters()} as ${k}MessageType"
            } else {
                "import ${v.noTypeParameters()}"
            }
        }.toSet().forEach {
            sb.appendln(it)
        }

        sb.appendln()

        val prefix = if (isSimple) "" else "abstract "
        val name = if (isSimple) "SimpleIr" else "AbstractIr"
        sb.appendln("${prefix}class ${name}ProtoReader(source: ByteArray) : ProtoReader(source) {")

        protoList.forEach { sb.addAbstractFactory(it) }

        protoList.filterIsInstance<Proto.Message>().forEach { sb.addMessage(it) }

        sb.appendln("}")

        return sb.toString()
    }

    private fun String.escape(): String {
        val result = this.split('_').fold("") { acc, c ->
            if (acc.isEmpty()) {
                c
            } else {
                acc + c[0].toUpperCase() + c.substring(1)
            }
        }

        return result + (if (result in setOf(
                "super",
                "this",
                "null",
                "break",
                "continue",
                "while",
                "for",
                "return",
                "throw",
                "try",
                "when"
            )
        ) "_" else "")
    }

    private fun StringBuilder.addAbstractFactory(p: Proto) {
        val maybeAbstract = if (isSimple) "" else "abstract "
        when (p) {
            is Proto.Enum -> {
                val impl = if (isSimple) " = index" else ""
                appendln("    ${maybeAbstract}fun create${p.name}(index: Int): ${p.name.ktType}${impl}")
            }
            is Proto.Message -> {
                append("    ${maybeAbstract}fun create${p.name}(")
                p.allFields.forEachIndexed { i, f ->
                    if (i != 0) append(", ")
                    val type = when (f.kind) {
                        FieldKind.REPEATED -> "List<${f.type.ktType}>"
                        FieldKind.REQUIRED -> f.type.ktType
                        else -> "${f.type.ktType}?"
                    }
                    append("${f.name.escape()} : $type")
                }

                val impl = if (!isSimple) "" else {
                    val args = p.allFields.fold("") { acc, c ->
                        (if (acc == "") "" else acc + ", ") + c.name.escape()
                    }

                    " = arrayOf<Any?>($args)"
                }

                appendln("): ${p.name.ktType}$impl")
            }
        }
        appendln()
    }

    private val String.ktType: String
        get() = classMap[this] ?: error("No known Kotlin type for '$this'")

    private val String.zeroValue: String?
        get() = when (this) {
            "int32" -> "0"
            "int64" -> "0L"
            "uint64" -> "0L"
            "string" -> "\"\""
            "bool" -> "false"
            "float" -> "0.0f"
            "double" -> "0.0"
            else -> null
        }

    private fun String.toReaderInvocation(): String {
        return typeMap.get(this)?.let {
            when (it) {
                is Proto.Enum -> "create${it.name}(readInt32())"
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

    private val Proto.Message.allFields: List<MessageEntry.Field>
        get() = fields.flatMap {
            when (it) {
                is MessageEntry.OneOf -> it.fields
                is MessageEntry.Field -> listOf(it)
            }
        }

    private fun StringBuilder.addMessage(m: Proto.Message) {
        val allFields = m.allFields

        appendln("    open fun read${m.name}(): ${m.name.ktType} {")

        val nullableFields = mutableSetOf<MessageEntry.Field>()

        allFields.forEach { f ->
            val (type, initExpression) = if (f.kind == FieldKind.REPEATED) {
                "MutableList<${f.type.ktType}>" to "mutableListOf()"
            } else {
                val zero = f.type.zeroValue
                if (f.kind == FieldKind.OPTIONAL && f.defaultValue != null) {
                    f.type.ktType to f.defaultValue
                } else if (f.kind == FieldKind.REQUIRED && zero != null) {
                    f.type.ktType to zero
                } else {
                    nullableFields.add(f)
                    "${f.type.ktType}?" to "null"
                }
            }
            appendln("        var ${f.name}__: $type = $initExpression")
        }

        appendln("        while (hasData) {")
        appendln("            readField { fieldNumber, type -> ")
        appendln("                when (fieldNumber) {")

        val indent = "                    "
        allFields.forEach { f ->
            val readExpression = f.type.toReaderInvocation()
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

        appendln("        return create${m.name}(${allFields.fold("") { acc, c ->
            var result = if (acc.isEmpty()) "" else "$acc, "
            result += c.name + "__"
            if (c.kind == FieldKind.REQUIRED && c in nullableFields) {
                result += "!!"
            }
            result
        }})")

        appendln("    }")
        appendln()
    }

}