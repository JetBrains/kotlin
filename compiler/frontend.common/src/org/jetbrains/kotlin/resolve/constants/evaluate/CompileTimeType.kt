/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.constants.evaluate

enum class CompileTimeType {
    BYTE,
    SHORT,
    INT,
    LONG,
    DOUBLE,
    FLOAT,
    CHAR,
    BOOLEAN,
    STRING,
    ANY,
}

fun CompileTimeType.toKotlinTypeName(): String = when (this) {
    CompileTimeType.BYTE -> "kotlin.Byte"
    CompileTimeType.SHORT -> "kotlin.Short"
    CompileTimeType.INT -> "kotlin.Int"
    CompileTimeType.LONG -> "kotlin.Long"
    CompileTimeType.DOUBLE -> "kotlin.Double"
    CompileTimeType.FLOAT -> "kotlin.Float"
    CompileTimeType.CHAR -> "kotlin.Char"
    CompileTimeType.BOOLEAN -> "kotlin.Boolean"
    CompileTimeType.STRING -> "kotlin.String"
    CompileTimeType.ANY -> "kotlin.Any"
}