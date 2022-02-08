// WITH_STDLIB
// TODO: Reified generics required some design to unify behavior across all backends
// IGNORE_BACKEND: JS, JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// FILE: 1.kt
package test

class A
class B

inline fun <reified T> Any?.foo(): T = this as T

// FILE: 2.kt

import test.*

fun box(): String {
    failNPE { null.foo<Any>(); return "Fail 1" }
    if (null.foo<Any?>() != null) return "Fail 2"

    failNPE { null.foo<A>(); return "Fail 3" }
    if  (null.foo<A?>() != null) return "Fail 4"

    val a = A()

    if (a.foo<Any>() != a) return "Fail 5"
    if (a.foo<Any?>() != a) return "Fail 6"

    if (a.foo<A>() != a) return "Fail 7"
    if (a.foo<A?>() != a) return "Fail 8"

    val b = B()

    failClassCast { b.foo<A>(); return "Fail 9" }
    failClassCast { b.foo<A?>(); return "Fail 10" }

    return "OK"
}

inline fun failNPE(s: () -> Unit) {
    try {
        s()
    }
    catch (e: NullPointerException) {
        // OK
    }
}

inline fun failClassCast(s: () -> Unit) {
    try {
        s()
    }
    catch (e: ClassCastException) {
        // OK
    }
}
