// TARGET_BACKEND: JVM
// WITH_STDLIB
// LAMBDAS: CLASS

import kotlin.test.assertEquals
import kotlin.jvm.internal.FunctionBase

fun test(f: Function<*>, arity: Int) {
    assertEquals(arity, (f as FunctionBase).arity)
}

fun foo(s: String, i: Int) {}
class A {
    fun bar(s: String, i: Int) {}
}
fun Double.baz(s: String, i: Int) {}

fun box(): String {
    test(::foo, 2)
    test(A::bar, 3)
    test(Double::baz, 3)

    test(::box, 0)

    fun local(x: Int) {}
    test(::local, 1)

    test(fun(s: String) = s, 1)
    test(fun(){}, 0)
    test({}, 0)
    test({x: Int -> x}, 1)

    return "OK"
}
