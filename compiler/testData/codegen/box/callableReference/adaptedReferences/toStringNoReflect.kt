// TARGET_BACKEND: JVM
// WITH_RUNTIME
package test

import kotlin.test.assertEquals

class A {
    fun foo(s: String = "", vararg xs: Long): CharSequence = "foo"
}

fun check(expected: String, x: Any) {
    assertEquals(expected, x.toString())
}

fun coercionToUnit(f: (A, String, LongArray) -> Unit): Any = f
fun varargToElement(f: (A, String, Long, Long) -> CharSequence): Any = f
fun defaultAndVararg(f: (A) -> CharSequence): Any = f
fun allOfTheAbove(f: (A) -> Unit): Any = f

fun box(): String {
    check("Function3<test.A, java.lang.String, long[], kotlin.Unit>", coercionToUnit(A::foo))
    check("Function4<test.A, java.lang.String, java.lang.Long, java.lang.Long, java.lang.CharSequence>", varargToElement(A::foo))
    check("Function1<test.A, java.lang.CharSequence>", defaultAndVararg(A::foo))
    check("Function1<test.A, kotlin.Unit>", allOfTheAbove(A::foo))

    return "OK"
}
