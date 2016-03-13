// FILE: 1.kt

package test

class A(val z: Int) {
    fun calc() = z
}

inline fun call(p: A, s: A.() -> Int): Int {
    return p.s()
}

// FILE: 2.kt

import test.*

fun box() : String {
    val call = call(A(11), A::calc)
    return if (call == 11) "OK" else "fail"
}
