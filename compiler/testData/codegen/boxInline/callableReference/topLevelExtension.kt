// FILE: 1.kt

package test

inline fun call(p: Int, s: Int.(Int) -> Int): Int {
    return p.s(p)
}

// FILE: 2.kt

import test.*

fun box() : String {
    return if (call(10, Int::calc) == 100) "OK" else "fail"
}

fun Int.calc(p: Int) : Int {
    return p * this
}
