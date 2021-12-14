// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

import kotlin.test.*

OPTIONAL_JVM_INLINE_ANNOTATION
value class S(val string: String)

var prop = S("")

fun box(): String {
    val propRef = ::prop

    assertEquals(S(""), propRef.get())

    propRef.set(S("abc"))
    assertEquals(S("abc"), propRef.get())

    assertEquals("prop", propRef.name)

    return "OK"
}