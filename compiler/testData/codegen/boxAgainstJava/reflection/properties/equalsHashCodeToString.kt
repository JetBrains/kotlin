package test

import kotlin.test.*
import test.equalsHashCodeToString as J

fun box(): String {
    assertEquals("val test.equalsHashCodeToString.b", (J::b).toString())
    assertEquals("var test.equalsHashCodeToString.c", (J::c).toString())

    assertTrue(J::b == J::b)
    assertFalse(J::c == J::b)

    assertTrue(J::b.hashCode() == J::b.hashCode())
    assertFalse(J::b.hashCode() == J::c.hashCode())

    return "OK"
}
