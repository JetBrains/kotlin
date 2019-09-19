/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

sealed class Proto(val name: String, val directives: List<String>) {
    class Message(name: String, val fields: List<MessageEntry>, directives: List<String> = emptyList()) : Proto(name, directives)

    class Enum(name: String, val entries: List<EnumEntry>, directives: List<String> = emptyList()) : Proto(name, directives)
}

class EnumEntry(
    val index: Int,
    val name: String
)

enum class FieldKind {
    REQUIRED,
    OPTIONAL,
    REPEATED,
    ONE_OF
}

sealed class MessageEntry(val name: String) {
    class Field(
        val kind: FieldKind,
        name: String,
        val index: Int,
        val type: String,
        val directives: List<String> = emptyList(),
        val defaultValue: String? = null
    ): MessageEntry(name)

    class OneOf(
        name: String,
        val fields: List<Field>
    ): MessageEntry(name)
}

fun printProto(p: Proto) {
    when (p) {
        is Proto.Message -> printMessage(p)
        is Proto.Enum -> printEnum(p)
    }
    println()
}

fun printMessage(message: Proto.Message) {
    println("message ${message.name} {")
    for (f in message.fields) {
        when (f) {
            is MessageEntry.OneOf -> {
                println("  oneof ${f.name} {")
                for (f2 in f.fields) {
                    println("    ${f2.type} ${f2.name} = ${f2.index};")
                }
                println("  }")
            }
            is MessageEntry.Field -> {
                print("  ${f.kind.toString().toLowerCase()} ${f.type} ${f.name} = ${f.index}")
                if (f.defaultValue != null) {
                    print(" [default = ${f.defaultValue}]")
                }
                if (f.directives.isNotEmpty()) {
                    print(" /*${f.directives.fold("") { acc, c -> acc + " " + c}} */")
                }
                println(";")
            }
        }
    }
    println("}")
}

fun printEnum(enum: Proto.Enum) {
    println("enum ${enum.name} {")
    for (e in enum.entries) {
        println("  ${e.name} = ${e.index};")
    }
    println("}")
}