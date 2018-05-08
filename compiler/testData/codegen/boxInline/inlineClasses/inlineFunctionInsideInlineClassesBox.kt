// !LANGUAGE: +InlineClasses

// FILE: 1.kt

package test

inline class A(val x: Int) {
    inline fun inc(): A = A(this.x + 1)

    inline fun result(other: A): String = if (other.x == x) "OK" else "fail"
}

// FILE: 2.kt

import test.*

fun box() : String {
    val a = A(0)
    val b = a.inc().inc()
    val result = b.result(A(2))

    return result
}
