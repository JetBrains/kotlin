// WITH_STDLIB
// FILE: 1.kt
package test

class A
class B

inline fun <reified T> Any?.foo(): T? = this as? T

// FILE: 2.kt

import test.*

fun box(): String {
    if (null.foo<Any>() != null) return "failTypeCast 1"
    if (null.foo<Any?>() != null) return "failTypeCast 2"

    if  (null.foo<A>() != null) return "failTypeCast 3"
    if  (null.foo<A?>() != null) return "failTypeCast 4"

    val a = A()

    if (a.foo<Any>() != a) return "failTypeCast 5"
    if (a.foo<Any?>() != a) return "failTypeCast 6"

    if (a.foo<A>() != a) return "failTypeCast 7"
    if (a.foo<A?>() != a) return "failTypeCast 8"

    val b = B()

    if (b.foo<A>() != null) return "fail 9"
    if (b.foo<A?>() != null) return "fail 10"

    return "OK"
}

inline fun failClassCast(s: () -> Unit) {
    try {
        s()
    }
    catch (e: ClassCastException) {

    }
}
