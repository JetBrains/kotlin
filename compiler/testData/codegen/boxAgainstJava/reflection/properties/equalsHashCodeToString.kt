// IGNORE_BACKEND: JVM_IR
// WITH_REFLECT
// FILE: test/J.java

package test;

public class J {
    public final boolean b;
    public char c;

    public J() {
        this.b = false;
        this.c = '0';
    }
}

// FILE: 1.kt

package test

import kotlin.test.*

fun box(): String {
    assertEquals("val test.J.b: kotlin.Boolean", (J::b).toString())
    assertEquals("var test.J.c: kotlin.Char", (J::c).toString())

    assertTrue(J::b == J::b)
    assertFalse(J::c == J::b)

    assertTrue(J::b.hashCode() == J::b.hashCode())
    assertFalse(J::b.hashCode() == J::c.hashCode())

    return "OK"
}
