/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

enum class PrimitiveType(typeName: String) {
    BOOLEAN("Boolean"),
    CHAR("Char"),
    BYTE("Byte"),
    SHORT("Short"),
    INT("Int"),
    FLOAT("Float"),
    LONG("Long"),
    DOUBLE("Double"),
    ;

    val typeName: Name = Name.identifier(typeName)

    val arrayTypeName: Name = Name.identifier("${typeName}Array")

    val typeFqName: FqName by lazy(LazyThreadSafetyMode.PUBLICATION) { StandardNames.BUILT_INS_PACKAGE_FQ_NAME.child(this.typeName) }

    val arrayTypeFqName: FqName by lazy(LazyThreadSafetyMode.PUBLICATION) { StandardNames.BUILT_INS_PACKAGE_FQ_NAME.child(arrayTypeName) }

    companion object {
        @JvmField
        val NUMBER_TYPES = setOf(CHAR, BYTE, SHORT, INT, FLOAT, LONG, DOUBLE)

        @JvmStatic
        fun getByShortName(name: String): PrimitiveType? = when (name) {
            "Boolean" -> BOOLEAN
            "Char" -> CHAR
            "Byte" -> BYTE
            "Short" -> SHORT
            "Int" -> INT
            "Float" -> FLOAT
            "Long" -> LONG
            "Double" -> DOUBLE
            else -> null
        }

        @JvmStatic
        fun getByShortArrayName(name: String): PrimitiveType? = when (name) {
            "BooleanArray" -> BOOLEAN
            "CharArray" -> CHAR
            "ByteArray" -> BYTE
            "ShortArray" -> SHORT
            "IntArray" -> INT
            "FloatArray" -> FLOAT
            "LongArray" -> LONG
            "DoubleArray" -> DOUBLE
            else -> null
        }
    }
}
