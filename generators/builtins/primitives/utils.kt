/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.numbers.primitives

import org.jetbrains.kotlin.generators.builtins.PrimitiveType

internal const val END_LINE = "\n"

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

internal fun operatorSign(methodName: String): String {
    return when (methodName) {
        "plus" -> "+"
        "minus" -> "-"
        "times" -> "*"
        "div" -> "/"
        "rem" -> "%"
        else -> throw IllegalArgumentException("Unsupported binary operation: ${methodName}")
    }
}

internal fun String.toPrimitiveType(): PrimitiveType {
    return PrimitiveType.valueOf(this.uppercase())
}
