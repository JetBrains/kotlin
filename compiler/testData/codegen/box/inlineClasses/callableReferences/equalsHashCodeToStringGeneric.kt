// WITH_REFLECT
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.test.*

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: String>(val s: T)

fun box(): String {
    val a = Z("a")
    val b = Z("b")

    val equals = Z<String>::equals
    assertTrue(equals.invoke(a, a))
    assertFalse(equals.invoke(a, b))

    val hashCode = Z<String>::hashCode
    assertEquals(a.s.hashCode(), hashCode.invoke(a))

    val toString = Z<String>::toString
    assertEquals("Z(s=${a.s})", toString.invoke(a))

    return "OK"
}