// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.test.assertEquals

OPTIONAL_JVM_INLINE_ANNOTATION
value class Z<T: Int>(val x: T)

class Outer(val z1: Z<Int>) {
    inner class Inner(val z2: Z<Int>)
}

fun box(): String {
    assertEquals(Z(1), ::Outer.invoke(Z(1)).z1)
    assertEquals(Z(2), Outer::Inner.invoke(Outer(Z(1)), Z(2)).z2)

    return "OK"
}