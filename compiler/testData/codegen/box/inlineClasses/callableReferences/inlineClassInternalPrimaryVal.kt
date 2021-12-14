// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

import kotlin.test.assertEquals

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(internal val x: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L(internal val x: Long)

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(internal val x: String)

fun box(): String {
    assertEquals(42, Z::x.get(Z(42)))
    assertEquals(1234L, L::x.get(L(1234L)))
    assertEquals("abc", S::x.get(S("abc")))

    assertEquals(42, Z::x.invoke(Z(42)))
    assertEquals(1234L, L::x.invoke(L(1234L)))
    assertEquals("abc", S::x.invoke(S("abc")))

    return "OK"
}