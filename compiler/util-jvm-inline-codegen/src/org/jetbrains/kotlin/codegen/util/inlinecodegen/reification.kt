/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.util.inlinecodegen

enum class ReifiedOperationKind {
    NEW_ARRAY, AS, SAFE_AS, IS, JAVA_CLASS, ENUM_REIFIED, TYPE_OF, CATCH;

    val id: Int get() = ordinal
}

data class ReificationArgument(
    val parameterName: String, val nullable: Boolean, val arrayDepth: Int
) {
    fun asString(): String =
        "[".repeat(arrayDepth) + parameterName + (if (nullable) "?" else "")

    fun combine(replacement: ReificationArgument): ReificationArgument =
        ReificationArgument(
            replacement.parameterName,
            this.nullable || (replacement.nullable && this.arrayDepth == 0),
            this.arrayDepth + replacement.arrayDepth
        )
}
