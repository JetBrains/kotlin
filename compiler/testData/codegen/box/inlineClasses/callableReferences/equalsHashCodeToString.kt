// WITH_REFLECT
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

import kotlin.test.*

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(val s: String)

fun box(): String {
    val a = Z("a")
    val b = Z("b")

    val equals = Z::equals
    assertTrue(equals.invoke(a, a))
    assertFalse(equals.invoke(a, b))

    val hashCode = Z::hashCode
    assertEquals(a.s.hashCode(), hashCode.invoke(a))

    val toString = Z::toString
    assertEquals("Z(s=${a.s})", toString.invoke(a))

    return "OK"
}