// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.test.assertEquals

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(private val x: T) {
    companion object {
        val xref = Z<Int>::x
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(private val x: T) {
    companion object {
        val xref = L<Long>::x
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class S<T: String>(private val x: T) {
    companion object {
        val xref = S<String>::x
    }
}

fun box(): String {
    assertEquals(42, Z.xref.get(Z(42)))
    assertEquals(1234L, L.xref.get(L(1234L)))
    assertEquals("abc", S.xref.get(S("abc")))

    assertEquals(42, Z.xref.invoke(Z(42)))
    assertEquals(1234L, L.xref.invoke(L(1234L)))
    assertEquals("abc", S.xref.invoke(S("abc")))

    return "OK"
}