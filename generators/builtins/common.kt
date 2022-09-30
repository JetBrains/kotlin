/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins

import org.jetbrains.kotlin.generators.builtins.ProgressionKind.CHAR
import java.io.PrintWriter

enum class PrimitiveType(val byteSize: Int) {
    BYTE(1),
    CHAR(2),
    SHORT(2),
    INT(4),
    LONG(8),
    FLOAT(4),
    DOUBLE(8),
    BOOLEAN(1);

    val capitalized: String get() = name.lowercase().replaceFirstChar(Char::uppercase)
    val bitSize = byteSize * 8

    val isFloatingPoint: Boolean get() = this in floatingPoint
    val isIntegral: Boolean get() = this in integral

    companion object {
        val exceptBoolean = PrimitiveType.values().filterNot { it == BOOLEAN }
        val onlyNumeric = PrimitiveType.values().filterNot { it == BOOLEAN || it == CHAR }
        val floatingPoint = listOf(FLOAT, DOUBLE)
        val integral = exceptBoolean - floatingPoint
    }
}

enum class UnsignedType {
    UBYTE,
    USHORT,
    UINT,
    ULONG;

    val capitalized: String get() = name.substring(0, 2) + name.substring(2).lowercase()
    val asSigned: PrimitiveType = PrimitiveType.valueOf(name.substring(1))

    val byteSize = (1 shl ordinal)
    val bitSize = byteSize * 8
    val mask = "0x${List(byteSize) { "FF" }.chunked(2).joinToString("_") { it.joinToString("") }}"
}

enum class ProgressionKind {
    CHAR,
    INT,
    LONG;

    val capitalized: String get() = name.lowercase().replaceFirstChar(Char::uppercase)
}

fun progressionIncrementType(kind: ProgressionKind) = when (kind) {
    CHAR -> "Int"
    else -> kind.capitalized
}

fun areEqualNumbers(v: String) = "$v == other.$v"

fun hashLong(v: String) = "($v xor ($v ushr 32))"

fun convert(v: String, from: UnsignedType, to: UnsignedType) = if (from == to) v else "$v.to${to.capitalized}()"

fun convert(v: String, from: PrimitiveType, to: PrimitiveType) = if (from == to) v else "$v.to${to.capitalized}()"


fun PrintWriter.printDoc(documentation: String, indent: String) {
    val docLines = documentation.lines()
    if (docLines.size == 1) {
        this.println("$indent/** $documentation */")
    } else {
        this.println("$indent/**")
        docLines.forEach { this.println("$indent * $it".trimEnd()) }
        this.println("$indent */")
    }
}
