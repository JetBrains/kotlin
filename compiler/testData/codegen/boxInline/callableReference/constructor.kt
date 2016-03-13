// FILE: 1.kt

package test

class A(val z: Int) {
    fun calc() = z
}

inline fun call(p: Int, s: (Int) -> A): Int {
    return s(p).z
}

// FILE: 2.kt

import test.*

fun box() : String {
    val call = call(11, ::A)
    return if (call == 11) "OK" else "fail"
}
