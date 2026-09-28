// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: test/J.java
package test;

public enum J {
    X, Y, Z;
}

// FILE: box.kt
package test

import kotlin.test.assertEquals

enum class E { X, Y, Z }

fun box(): String {
    assertEquals("fun values(): kotlin.Array<test.E>", E::values.toString())
    assertEquals(listOf(E.X, E.Y, E.Z), E::values.call().toList())
    assertEquals("fun valueOf(kotlin.String): test.E", E::valueOf.toString())
    assertEquals(E.Y, E::valueOf.call("Y"))

    assertEquals("fun values(): kotlin.Array<test.J>", J::values.toString())
    assertEquals(listOf(J.X, J.Y, J.Z), J::values.call().toList())
    assertEquals("fun valueOf(kotlin.String): test.J", J::valueOf.toString())
    assertEquals(J.Y, J::valueOf.call("Y"))

    return "OK"
}
