// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

class A
class B

inline fun <reified T> Any?.foo() = this is T

inline fun <reified Y> Any?.foo2() = foo<Y?>()

inline fun <reified Z> Any?.foo3() = foo2<Z>()

// FILE: 2.kt

import test.*

fun box(): String {
    if (null.foo3<Any>() != true) return "fail 1"
    if (null.foo3<Any?>() != true) return "fail 2"

    if (null.foo3<A>() != true) return "fail 3"
    if (null.foo3<A?>() != true) return "fail 4"

    val a = A()

    if (a.foo3<Any>() != true) return "fail 5"
    if (a.foo3<Any?>() != true) return "fail 6"

    if (a.foo3<A>() != true) return "fail 7"
    if (a.foo3<A?>() != true) return "fail 8"

    val b = B()

    if (b.foo3<A>() != false) return "fail 9"
    if (b.foo3<A?>() != false) return "fail 10"

    return "OK"
}
