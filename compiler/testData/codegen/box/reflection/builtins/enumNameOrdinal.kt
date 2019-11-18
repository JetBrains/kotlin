// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR, JS, NATIVE
// WITH_REFLECT

import kotlin.test.assertEquals

enum class E { X, Y, Z }

fun box(): String {
    assertEquals(11, E::class.members.size)
    assertEquals("Y", E::name.call(E.Y))
    assertEquals(2, E::ordinal.call(E.Z))
    return "OK"
}
