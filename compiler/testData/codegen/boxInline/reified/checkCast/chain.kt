// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: 1.kt
package test

class A
class B

inline fun <reified T> Any?.foo(): T = this as T

inline fun <reified Y> Any?.foo2(): Y? = foo<Y?>()

inline fun <reified Z> Any?.foo3(): Z? = foo2<Z>()

// FILE: 2.kt

import test.*

fun box(): String {
    if (null.foo3<Any>() != null) return "fail 1"
    if (null.foo3<Any?>() != null) return "fail 2"

    if (null.foo3<A>() != null) return "fail 3"
    if (null.foo3<A?>() != null) return "fail 4"

    val a = A()

    if (a.foo3<Any>() != a) return "fail 5"
    if (a.foo3<Any?>() != a) return "fail 6"

    if (a.foo3<A>() != a) return "fail 7"
    if (a.foo3<A?>() != a) return "fail 8"

    val b = B()

    failClassCast { b.foo3<A>(); return "failTypeCast 9" }
    failClassCast { b.foo3<A?>(); return "failTypeCast 10" }

    return "OK"
}

inline fun failClassCast(s: () -> Unit) {
    try {
        s()
    }
    catch (e: ClassCastException) {

    }
}
