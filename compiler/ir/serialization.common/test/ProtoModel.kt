/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.nextgen

sealed class Proto(val name: String) {
    class Message(name: String, val fields: List<MessageEntry>) : Proto(name)

    class Enum(name: String, val entries: List<EnumEntry>) : Proto(name)
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
        val type: String
    ): MessageEntry(name)

    class OneOf(
        name: String,
        val fields: List<Field>
    ): MessageEntry(name)
}