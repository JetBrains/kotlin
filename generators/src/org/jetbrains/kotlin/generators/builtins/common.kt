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
    BYTE
    CHAR
    SHORT
    INT
    LONG
    FLOAT
    DOUBLE
    BOOLEAN

    val capitalized: String get() = name().toLowerCase().capitalize()
    default object {
        val exceptBoolean: Iterable<PrimitiveType> by Delegates.lazy { PrimitiveType.values().filterNot { it == BOOLEAN } }
    }
}

enum class ProgressionKind {
    BYTE
    CHAR
    SHORT
    INT
    LONG
    FLOAT
    DOUBLE

    val capitalized: String get() = name().toLowerCase().capitalize()
}

fun progressionIncrementType(kind: ProgressionKind) = when (kind) {
    BYTE, CHAR, SHORT -> "Int"
    else -> kind.capitalized
}

fun areEqualNumbers(kind: ProgressionKind, v: String) = when (kind) {
    FLOAT, DOUBLE -> "java.lang.${kind.capitalized}.compare($v, other.$v) == 0"
    else -> "$v == other.$v"
}

fun hashLong(v: String) = "($v xor ($v ushr 32))"

fun floatToIntBits(v: String) = "java.lang.Float.floatToIntBits($v)"

fun doubleToLongBits(v: String) = "java.lang.Double.doubleToLongBits($v)"

