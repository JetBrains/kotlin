// IGNORE_BACKEND: JS_IR, JS, NATIVE, WASM
// IGNORE_BACKEND: JS_IR_ES6
// WITH_REFLECT

package test

import kotlin.test.assertEquals

enum class E { X, Y, Z }

fun box(): String {
    assertEquals("fun values(): kotlin.Array<test.E>", E::values.toString())
    assertEquals(listOf(E.X, E.Y, E.Z), E::values.call().toList())
    assertEquals("fun valueOf(kotlin.String): test.E", E::valueOf.toString())
    assertEquals(E.Y, E::valueOf.call("Y"))

    return "OK"
}
