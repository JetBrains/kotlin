// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.test.assertEquals

interface IFoo {
    fun foo(s: String): String
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Long>(val x: T) : IFoo {
    override fun foo(s: String): String = x.toString() + s
}

class Test(x: Long) : IFoo by Z(x)

fun box(): String {
    assertEquals("1OK", Test(1L).foo("OK"))

    return "OK"
}