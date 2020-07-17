/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    val typeFqName: FqName by lazy(LazyThreadSafetyMode.PUBLICATION) { KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.child(this.typeName) }

    val arrayTypeFqName: FqName by lazy(LazyThreadSafetyMode.PUBLICATION) { KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.child(arrayTypeName) }

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
