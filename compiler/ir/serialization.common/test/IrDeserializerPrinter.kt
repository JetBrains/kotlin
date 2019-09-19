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
        val name = if (isSimple) "SimpleIr" else "AbstractIrSmart"
        sb.appendln("${prefix}class ${name}ProtoReader(source: ByteArray) : ProtoReader(source) {")

        protoList.forEach { sb.addAbstractFactory(it) }

        protoList.filterIsInstance<Proto.Message>().forEach { sb.addMessage(it) }

        sb.appendln("}")

        return sb.toString()
    }

    private fun String.escape(): String {
        val result = this.split('_').fold("") { acc, c ->
            if (acc.isEmpty()) {
                c[0].toLowerCase() + c.substring(1)
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
                "when",
                "hasData",
                "fieldNumber",
                "type"
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

                if (p.isInline) return

                fun addMessage(suffix: String, fields: List<MessageEntry.Field>) {
                    append("    ${maybeAbstract}fun create${p.name}${suffix}(")
                    fields.forEachIndexed { i, f ->
                        if (i != 0) append(", ")
                        val type = when (f.kind) {
                            FieldKind.REPEATED -> "List<${f.type.ktType}>"
                            FieldKind.REQUIRED, FieldKind.ONE_OF -> f.type.ktType
                            else -> "${f.type.ktType}?"
                        }
                        append("${f.name.escape()} : $type")
                    }

                    val impl = if (!isSimple) "" else {
                        val args = fields.fold("") { acc, c ->
                            (if (acc == "") "" else acc + ", ") + c.name.escape()
                        }

                        " = arrayOf<Any?>($args)"
                    }

                    appendln("): ${p.name.ktType}$impl")
                }

                val allFields = p.fields.inlined()

                if (allFields.any { it is MessageEntry.OneOf }) {
                    val o =
                        allFields.filterIsInstance<MessageEntry.OneOf>().singleOrNull() ?: error("Too many oneof's in message ${p.name}")

                    val (fp, fs) = allFields.splitBy(o)

                    for (of in o.fields) {
                        addMessage("_${of.name.escape()}", fp + of + fs)
                    }
                } else {
                    addMessage("", allFields.map { it as MessageEntry.Field })
                }
            }
        }
        appendln()
    }

    val Proto.Message.isInline: Boolean
        get() = "@inline" in this.directives

    val MessageEntry.Field.isInline: Boolean
        get() = type in typeMap && (typeMap[type]!! as? Proto.Message)?.isInline == true

    fun List<MessageEntry>.inlined(): List<MessageEntry> {
        return this.flatMap {
            if (it is MessageEntry.Field && it.isInline) {
                (typeMap[it.type] as Proto.Message).fields.inlined()
            } else listOf(it)
        }
    }

    private fun List<MessageEntry>.splitBy(of: MessageEntry.OneOf): Pair<List<MessageEntry.Field>, List<MessageEntry.Field>> {
        val fp = mutableListOf<MessageEntry.Field>()
        val fs = mutableListOf<MessageEntry.Field>()

        var cf = fp
        this.forEach {
            if (it === of) {
                cf = fs
            } else {
                cf.add(it as MessageEntry.Field)
            }
        }

        return fp to fs
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

    private val List<MessageEntry>.oneOfInlined: List<MessageEntry.Field>
        get() = flatMap {
            when (it) {
                is MessageEntry.OneOf -> it.fields
                is MessageEntry.Field -> listOf(it)
            }
        }

    private fun StringBuilder.addMessage(m: Proto.Message) {
        if (m.isInline) return

        val allFields = m.fields.inlined().oneOfInlined

        allFields.fold(mutableMapOf<String, Int>()) { acc, c ->
            val name = c.name.escape()
            acc[name] = acc.getOrDefault(name, 0) + 1
            acc
        }.entries.forEach { (name, cnt) ->
            if (cnt > 1) {
                error("Duplication name '$name' in ${m.name}")
            }
        }

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
            appendln("        var ${f.name.escape()}: $type = $initExpression")
        }


        val of = m.fields.inlined().filterIsInstance<MessageEntry.OneOf>().singleOrNull()?.let { of ->
            appendln("        var oneOfIndex: Int = -1")
            of
        }


        fun readFields(shift: String, fields: List<MessageEntry.Field>) {
            appendln("${shift}while (hasData) {")
            appendln("${shift}    readField { fieldNumber, type -> ")
            appendln("${shift}        when (fieldNumber) {")

            val indent = "${shift}            "
            fields.forEach { f ->
                if (f.isInline) {
                    appendln("${indent}${f.index} -> readWithLength {")
                    readFields("${indent}    ", (typeMap[f.type] as Proto.Message).fields.oneOfInlined)
                    appendln("${indent}}")
                } else {
                    val readExpression = f.type.toReaderInvocation()
                    if (f.kind == FieldKind.REPEATED) {
                        appendln("${indent}${f.index} -> ${f.name.escape()}.add($readExpression)")
                    } else if (f.kind == FieldKind.ONE_OF) {
                        appendln("${indent}${f.index} -> {")
                        appendln("${indent}    ${f.name.escape()} = $readExpression")
                        appendln("${indent}    oneOfIndex = ${f.index}")
                        appendln("${indent}}")
                    } else {
                        appendln("${indent}${f.index} -> ${f.name.escape()} = $readExpression")
                    }
                }
            }

            appendln("${shift}            else -> skip(type)")
            appendln("${shift}        }")
            appendln("${shift}    }")
            appendln("${shift}}")
        }

        readFields("        ", m.fields.oneOfInlined)

        fun invokeCreate(suffix: String, fields: List<MessageEntry.Field>): String {
            return "return create${m.name}${suffix}(${fields.fold("") { acc, c ->
                var result = if (acc.isEmpty()) "" else "$acc, "
                result += c.name.escape()
                if ((c.kind == FieldKind.REQUIRED || c.kind == FieldKind.ONE_OF) && c in nullableFields) {
                    result += "!!"
                }
                result
            }})"
        }

        if (of == null) {
            appendln("        ${invokeCreate("", allFields)}")
        } else {
            val (fp, fs) = m.fields.splitBy(of)

            appendln("        when (oneOfIndex) {")
            for (f in of.fields) {
                appendln("            ${f.index} -> ${invokeCreate("_${f.name.escape()}", fp + f + fs)}")
            }
            appendln("            else -> error(\"Incorrect oneOf index: \" + oneOfIndex)")
            appendln("        }")
        }

        appendln("    }")
        appendln()
    }

}