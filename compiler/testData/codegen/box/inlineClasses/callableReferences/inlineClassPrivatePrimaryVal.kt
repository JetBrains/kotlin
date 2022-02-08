// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

import kotlin.test.assertEquals

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z(private val x: Int) {
    companion object {
        val xref = Z::x
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class L(private val x: Long) {
    companion object {
        val xref = L::x
    }
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(private val x: String) {
    companion object {
        val xref = S::x
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