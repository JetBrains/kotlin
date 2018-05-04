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

package org.jetbrains.kotlin.generators.builtins

import org.jetbrains.kotlin.generators.builtins.ProgressionKind.*
import kotlin.properties.Delegates

enum class PrimitiveType {
    BYTE,
    CHAR,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN;

    val capitalized: String get() = name.toLowerCase().capitalize()
    companion object {
        val exceptBoolean = PrimitiveType.values().filterNot { it == BOOLEAN }
        val onlyNumeric = PrimitiveType.values().filterNot { it == BOOLEAN || it == CHAR }
    }
}

enum class UnsignedType {
    UBYTE,
    USHORT,
    UINT,
    ULONG;

    val capitalized: String get() = name.substring(0, 2) + name.substring(2).toLowerCase()
    val asSigned: PrimitiveType = PrimitiveType.valueOf(name.substring(1))

    val byteSize = (1 shl ordinal)
    val mask = "0x${List(byteSize) { "FF" }.chunked(2).joinToString("_") { it.joinToString("") }}"
}

enum class ProgressionKind {
    CHAR,
    INT,
    LONG;

    val capitalized: String get() = name.toLowerCase().capitalize()
}

fun progressionIncrementType(kind: ProgressionKind) = when (kind) {
    CHAR -> "Int"
    else -> kind.capitalized
}

fun areEqualNumbers(v: String) = "$v == other.$v"

fun hashLong(v: String) = "($v xor ($v ushr 32))"

fun convert(v: String, from: UnsignedType, to: UnsignedType) = if (from == to) v else "$v.to${to.capitalized}()"