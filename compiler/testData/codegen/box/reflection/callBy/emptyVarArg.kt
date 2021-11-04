// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: JS, NATIVE, WASM

// WITH_REFLECT

import kotlin.test.assertEquals

fun join(vararg strings: String) = strings.toList().joinToString("")

fun sum(vararg bytes: Byte) = bytes.toList().fold(0) { acc, el -> acc + el }

fun box(): String {
    val j = ::join
    val s = ::sum
    assertEquals("", j.callBy(emptyMap()))
    assertEquals(0, s.callBy(emptyMap()))
    return "OK"
}
