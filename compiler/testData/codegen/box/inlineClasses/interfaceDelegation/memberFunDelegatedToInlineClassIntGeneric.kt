// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.test.assertEquals

interface IFoo {
    fun foo(s: String): String
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T) : IFoo {
    override fun foo(s: String): String = x.toString() + s
}

class Test(x: Int) : IFoo by Z(x)

fun box(): String {
    assertEquals("1OK", Test(1).foo("OK"))

    return "OK"
}