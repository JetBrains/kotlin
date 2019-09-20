/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

import org.jetbrains.kotlin.backend.common.atMostOne
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
            if (c.isEmpty()) acc else {
                if (acc.isEmpty()) {
                    c[0].toLowerCase() + c.substring(1)
                } else {
                    acc + c[0].toUpperCase() + c.substring(1)
                }
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

                val names = mutableMapOf<MessageEntry, String>()
                names += p.fields.buildNames("")

                fun addMessage(index: Int, suffix: String, fields: List<MessageEntry.Field>) {
                    val actualSuffix = if (index != 0) "" + index + suffix else suffix
                    append("    ${maybeAbstract}fun create${p.name}${actualSuffix}(")
                    fields.forEachIndexed { i, f ->
                        if (i != 0) {
                            append(", ")
                        } else if (index != 0) {
                            append("partial: ${p.name.ktType}, ")
                        }
                        val type = when (f.kind) {
                            FieldKind.REPEATED -> "List<${f.type.ktType}>"
                            FieldKind.REQUIRED, FieldKind.ONE_OF -> f.type.ktType
                            else -> "${f.type.ktType}?"
                        }
                        append("${names[f]!!} : $type")
                    }

                    val impl = if (!isSimple) "" else {
                        val args = fields.fold("") { acc, c ->
                            (if (acc == "") "" else acc + ", ") + names[c]!!
                        }

                        " = arrayOf<Any?>($args)"
                    }

                    appendln("): ${p.name.ktType}$impl")
                }

                val fieldsByOrder = p.fields.inlined().splitByOrder()

                fieldsByOrder.forEachIndexed { i, allFields ->
                    if (allFields.any { it is MessageEntry.OneOf }) {
                        val o =
                            allFields.filterIsInstance<MessageEntry.OneOf>().singleOrNull()
                                ?: error("Too many oneof's in message ${p.name}")

                        val (fp, fs) = allFields.splitBy(o)

                        for (of in o.fields.inlined()) {
                            of as MessageEntry.Field
                            addMessage(i, "_${of.name.escape()}", fp + of + fs)
                        }
                    } else {
                        addMessage(i, "", allFields.map { it as MessageEntry.Field })
                    }
                }
            }
        }
        appendln()
    }

    val MessageEntry.order: Int
        get() = (this as? MessageEntry.Field)?.directives?.atMostOne { it.startsWith("@order_") }?.substring("@order_".length)?.toInt()
            ?: 0

    private fun List<MessageEntry>.splitByOrder(): List<List<MessageEntry>> {
        return this.groupBy { it.order }.entries.sortedBy { (k, _) -> k }.map { (_, v) -> v }
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

    fun List<MessageEntry>.buildNames(prefix: String): MutableMap<MessageEntry.Field, String> {
        val result = mutableMapOf<MessageEntry.Field, String>()

        forEach { f ->
            when (f) {
                is MessageEntry.Field -> {
                    if (f.isInline) {
                        val msg = typeMap[f.type] as Proto.Message
                        result += msg.fields.buildNames((prefix + "_" + f.name).escape())
                    } else {
                        result[f] = (prefix + "_" + f.name).escape()
                    }
                }
                is MessageEntry.OneOf -> {
                    result += f.fields.buildNames(prefix + "OneOf")
                }
            }
        }

        return result
    }

    val MessageEntry.Field.isExposed: Boolean
        get() = "@exposed" in this.directives

    private fun List<MessageEntry>.buildExposedSet(exposeAll: Boolean = false): List<MessageEntry.Field> {
        val result = mutableListOf<MessageEntry.Field>()

        forEach { f ->
            when (f) {
                is MessageEntry.Field -> {
                    if (exposeAll || f.isExposed) {
                        if (f.isInline) {
                            result += (typeMap[f.type] as Proto.Message).fields.buildExposedSet(true)
                        } else {
                            result += f
                        }
                    }
                }
                is MessageEntry.OneOf -> {
                    result += f.fields.buildExposedSet(exposeAll)
                }
            }
        }

        return result
    }

    private fun StringBuilder.addMessage(m: Proto.Message) {
        if (m.isInline) return

        val exposedFields = m.fields.buildExposedSet()

        val allFields = m.fields.inlined().oneOfInlined.inlined().oneOfInlined // TODO

        val names = m.fields.buildNames("")

        fun getName(field: MessageEntry.Field): String {
            return names[field]!!
        }

        allFields.fold(mutableMapOf<String, Int>()) { acc, c ->
            val name = getName(c)
            acc[name] = acc.getOrDefault(name, 0) + 1
            acc
        }.entries.forEach { (name, cnt) ->
            if (cnt > 1) {
                error("Duplication name '$name' in ${m.name}")
            }
        }

        val exposedName = mutableMapOf<MessageEntry.Field, String>()

        if (exposedFields.isNotEmpty()) {
            for (f in exposedFields) {
                val name = "field_${m.name}_${getName(f)}".escape()
                exposedName[f] = name
                f.type.zeroValue?.let {
                    appendln("    protected var $name: ${f.type.ktType} = ${it}")
                } ?: appendln("    protected var $name: ${f.type.ktType}? = null")
            }

            appendln()
        }

        appendln("    open fun read${m.name}(): ${m.name.ktType} {")

        val iterations = allFields.groupBy { if (it in exposedFields) -1 else it.order }.entries.sortedBy { (k, _) -> k }.map { (_, v) -> v }.filter { !it.isEmpty() }
        val fieldToIteration = mutableMapOf<MessageEntry.Field, Int>()
        iterations.forEachIndexed { index, list ->
            list.forEach { fieldToIteration[it] = index }
        }

        val nullableFields = mutableSetOf<MessageEntry.Field>()

        val delayedReads = mutableSetOf<MessageEntry.Field>()

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
            appendln("        var ${getName(f)}: $type = $initExpression")

            if (fieldToIteration[f] != 0 && typeMap[f.type] is Proto.Message) {
                delayedReads += f
                if (f.kind == FieldKind.REPEATED) {
                    appendln("        var ${getName(f)}OffsetList: MutableList<Int> = arrayListOf()")
                } else {
                    appendln("        var ${getName(f)}Offset: Int = -1")
                }
            }
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
                        if (f in delayedReads) {
                            appendln("${indent}${f.index} -> {")
                            appendln("${indent}    ${getName(f)}OffsetList.add(offset)")
                            appendln("${indent}    skip(type)")
                            appendln("${indent}}")
                        } else {
                            appendln("${indent}${f.index} -> ${getName(f)}.add($readExpression)")
                        }
                    } else if (f.kind == FieldKind.ONE_OF) {
                        appendln("${indent}${f.index} -> {")
                        if (f in delayedReads) {
                            appendln("${indent}    ${getName(f)}Offset = offset")
                            appendln("${indent}    skip(type)")
                        } else {
                            appendln("${indent}    ${getName(f)} = $readExpression")
                        }
                        appendln("${indent}    oneOfIndex = ${f.index}")
                        appendln("${indent}}")
                    } else {
                        if (f in delayedReads) {
                            appendln("${indent}${f.index} -> {")
                            appendln("${indent}    ${getName(f)}Offset = offset")
                            appendln("${indent}    skip(type)")
                            appendln("${indent}}")
                        } else {
                            appendln("${indent}${f.index} -> ${getName(f)} = $readExpression")
                        }
                    }
                }
            }

            appendln("${shift}            else -> skip(type)")
            appendln("${shift}        }")
            appendln("${shift}    }")
            appendln("${shift}}")
        }

        readFields("        ", m.fields.oneOfInlined)

        val lastIteration = allFields.splitByOrder().size - 1

        val hasExposed = exposedFields.isNotEmpty()

        fun invokeCreate(index: Int, suffix: String, fields: List<MessageEntry.Field>): String {
            val prefix = if (index != lastIteration || hasExposed) {
                "create${m.name}${suffix}("
            } else {
                "return create${m.name}${suffix}("
            } + if (index == 0) "" else {
                "p${index - 1}, "
            }

            return "${prefix}${fields.fold("") { acc, c ->
                var result = if (acc.isEmpty()) "" else "$acc, "
                result += getName(c)
                if ((c.kind == FieldKind.REQUIRED || c.kind == FieldKind.ONE_OF) && c in nullableFields) {
                    result += "!!"
                }
                result
            }})"
        }

        fun invokeCreateWithOneOf(index: Int, fields: List<MessageEntry.Field>) {

            val hasOneOf = fields.any { it.kind == FieldKind.ONE_OF }

            val suffixPrefix = if (index == 0) "" else "" + index

            if (!hasOneOf) {
                if (index != lastIteration || hasExposed) {
                    appendln("        val p${index} = ${invokeCreate(index, suffixPrefix, fields)}")
                } else {
                    appendln("        ${invokeCreate(index, suffixPrefix, fields)}")
                }
            } else {
                val fp = mutableListOf<MessageEntry.Field>()
                val fs = mutableListOf<MessageEntry.Field>()
                val mf = mutableListOf<MessageEntry.Field>()

                var cf = fp

                fields.forEach {
                    if (it.kind == FieldKind.ONE_OF) {
                        cf = fs
                        mf.add(it)
                    } else {
                        cf.add(it)
                    }
                }

                if (index != lastIteration || hasExposed) {
                    appendln("        val p${index} = when (oneOfIndex) {")
                } else {
                    appendln("        when (oneOfIndex) {")
                }

                for (f in mf) {
                    appendln("            ${f.index} -> ${invokeCreate(index, "${suffixPrefix}_${f.name.escape()}", fp + f + fs)}")
                }
                appendln("            else -> error(\"Incorrect oneOf index: \" + oneOfIndex)")
                appendln("        }")
            }
        }

        if (!hasExposed) {
            invokeCreateWithOneOf(0, iterations[0])
            if (lastIteration != 0) appendln()
        } else {
            for (f in exposedFields) {
                appendln("        val old${exposedName[f]} = ${exposedName[f]}")
                appendln("        ${exposedName[f]} = ${getName(f)}")
            }
        }

        for (i in 1 until iterations.size) {
            val fields = iterations[i].filter { it in delayedReads }
            for (f in fields) {
                if (f.kind == FieldKind.REPEATED) {
                    appendln("        for (o in ${getName(f)}OffsetList) {")
                    appendln("            ${getName(f)}.add(delayed(o) { ${f.type.toReaderInvocation()} })")
                    appendln("        }")
                } else {
                    appendln("        if (${getName(f)}Offset != -1) {")
                    appendln("            ${getName(f)} = delayed(${getName(f)}Offset) { ${f.type.toReaderInvocation()} }")
                    appendln("        }")
                }
            }

            val params = if (i == 1 && hasExposed) allFields.splitByOrder()[0].map { it as MessageEntry.Field } else iterations[i]
            val index = if (hasExposed) i - 1 else i

            invokeCreateWithOneOf(index, params)
            if (lastIteration != index) appendln()
        }

        for (f in exposedFields) {
            appendln("        ${exposedName[f]} = old${exposedName[f]}")
        }

        if (hasExposed) {
            appendln("        return p${lastIteration}")
        }

        appendln("    }")
        appendln()
    }

}