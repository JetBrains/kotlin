// FILE: 1.kt

package test

class M {
    val m = IntArray(1)
}

inline operator fun <reified T> M.get(a: T): Int {
    if ("" !is T) throw AssertionError()
    return m[0]
}

operator fun M.set(a: Any, v: Int) {
    m[0] = v
}

// FILE: 2.kt

import test.*

fun box(): String {
    val m = M()
    m["b"]++
    return if (m.m[0] == 1) "OK" else "Fail: ${m.m[0]}"
}
