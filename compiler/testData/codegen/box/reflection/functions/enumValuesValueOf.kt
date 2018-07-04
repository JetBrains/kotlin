// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR, JS, NATIVE
// WITH_REFLECT

import kotlin.test.assertEquals

enum class E { X, Y, Z }

fun box(): String {
    assertEquals("fun values(): kotlin.Array<E>", E::values.toString())
    assertEquals(listOf(E.X, E.Y, E.Z), E::values.call().toList())
    assertEquals("fun valueOf(kotlin.String): E", E::valueOf.toString())
    assertEquals(E.Y, E::valueOf.call("Y"))

    return "OK"
}
