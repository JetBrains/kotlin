// FILE: 1.kt
// WITH_RUNTIME
package test

class A
class B

inline fun <reified T> Any?.foo(): T = this as T

// FILE: 2.kt

import test.*

fun box(): String {
    failTypeCast { null.foo<Any>(); return "failTypeCast 1" }
    if (null.foo<Any?>() != null) return "failTypeCast 2"

    failTypeCast { null.foo<A>(); return "failTypeCast 3" }
    if  (null.foo<A?>() != null) return "failTypeCast 4"

    val a = A()

    if (a.foo<Any>() != a) return "failTypeCast 5"
    if (a.foo<Any?>() != a) return "failTypeCast 6"

    if (a.foo<A>() != a) return "failTypeCast 7"
    if (a.foo<A?>() != a) return "failTypeCast 8"

    val b = B()

    failClassCast { b.foo<A>(); return "failTypeCast 9" }
    failClassCast { b.foo<A?>(); return "failTypeCast 10" }

    return "OK"
}

inline fun failTypeCast(s: () -> Unit) {
    try {
        s()
    }
    catch (e: TypeCastException) {

    }
}

inline fun failClassCast(s: () -> Unit) {
    try {
        s()
    }
    catch (e: TypeCastException) {
        throw e
    }
    catch (e: ClassCastException) {

    }
}
