/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// KT-66088
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// KT-66089
// IGNORE_BACKEND: WASM
// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    val xBool = true
    val xBoolStatic : Any = false
    val xBoolDyanmic : Any = !xBool
    assertSame(xBoolStatic, xBoolDyanmic, "xBoolStatic($xBoolStatic) != xBoolDyanmic($xBoolDyanmic)")

    val xByte = 1.toByte()
    val xByteStatic : Any = 2.toByte()
    val xByteDyanmic : Any = (xByte + xByte).toByte()
    assertSame(xByteStatic, xByteDyanmic, "xByteStatic($xByteStatic) != xByteDyanmic($xByteDyanmic)")

    val xShort = 1.toShort()
    val xShortStatic : Any = 2.toShort()
    val xShortDyanmic : Any = (xShort + xShort).toShort()
    assertSame(xShortStatic, xShortDyanmic, "xShortStatic($xShortStatic) != xShortDyanmic($xShortDyanmic)")

    val xInt = 1.toInt()
    val xIntStatic : Any = 2.toInt()
    val xIntDyanmic : Any = xInt + xInt
    assertSame(xIntStatic, xIntDyanmic, "xIntStatic($xIntStatic) != xIntDyanmic($xIntDyanmic)")

    val xChar = 1.toChar()
    val xCharStatic : Any = 2.toChar()
    val xCharDyanmic : Any = (xChar.code + xChar.code).toChar()
    assertSame(xCharStatic, xCharDyanmic, "xCharStatic(${(xCharStatic as Char).code}) != xCharDyanmic(${(xCharDyanmic as Char).code})")

    val xLong = 1.toLong()
    val xLongStatic = 2.toLong()
    val xLongDyanmic = xLong + xLong
    assertSame(xLongStatic, xLongDyanmic, "xLongStatic($xLongStatic) != xLongDyanmic($xLongDyanmic)")

    return "OK"
}