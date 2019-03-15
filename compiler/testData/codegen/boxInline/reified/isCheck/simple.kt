// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

class A
class B

inline fun <reified T> Any?.foo() = this is T

// FILE: 2.kt

import test.*

fun box(): String {
    if (null.foo<Any>() != false) return "fail 1"
    if (null.foo<Any?>() != true) return "fail 2"

    if (null.foo<A>() != false) return "fail 3"
    if (null.foo<A?>() != true) return "fail 4"

    val a = A()

    if (a.foo<Any>() != true) return "fail 5"
    if (a.foo<Any?>() != true) return "fail 6"

    if (a.foo<A>() != true) return "fail 7"
    if (a.foo<A?>() != true) return "fail 8"

    val b = B()

    if (b.foo<A>() != false) return "fail 9"
    if (b.foo<A?>() != false) return "fail 10"

    return "OK"
}
