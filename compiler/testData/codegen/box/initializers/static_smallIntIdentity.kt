// KT-66088
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: WASM-JS:2.4
// ^^^ KT-66089 is fixed in 2.5.0-Beta1

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
