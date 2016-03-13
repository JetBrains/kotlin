// FILE: 1.kt

package test

class A

fun call(a: String, b: String, c: String, d: String, e: String, f: Any) {

}

inline fun <reified T: Any> Any?.foo(): T {
    call("1", "2", "3", "4", "5", this as T)
    return this as T
}

// FILE: 2.kt

import test.*



fun box(): String {
    val a = A()
    if (a.foo<Any>() != a) return "failTypeCast 5"

    return "OK"
}
