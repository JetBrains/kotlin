/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType

internal fun PrimitiveType.castToIfNecessary(otherType: PrimitiveType): String {
    if (this !in PrimitiveType.onlyNumeric || otherType !in PrimitiveType.onlyNumeric) {
        throw IllegalArgumentException("Cannot cast to non-numeric type")
    }

    if (this == otherType) return ""

    if (this.ordinal < otherType.ordinal) {
        return ".to${otherType.capitalized}()"
    }

    return ""
}

internal fun String.asSign(): String {
    return when (this) {
        "plus" -> "+"
        "minus" -> "-"
        "times" -> "*"
        "div" -> "/"
        "rem" -> "%"
        else -> throw IllegalArgumentException("Unsupported binary operation: ${this}")
    }
}
