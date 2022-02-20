// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.test.assertEquals

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(internal val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(internal val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(internal val x: T)

fun box(): String {
    assertEquals(42, Z<Int>::x.get(Z(42)))
    assertEquals(1234L, L<Long>::x.get(L(1234L)))
    assertEquals("abc", S<String>::x.get(S("abc")))

    assertEquals(42, Z<Int>::x.invoke(Z(42)))
    assertEquals(1234L, L<Long>::x.invoke(L(1234L)))
    assertEquals("abc", S<String>::x.invoke(S("abc")))

    return "OK"
}