// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: ANY:2.4
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
    m["a"] += 10
    return if (m.m[0] == 10) "OK" else "Fail: ${m.m[0]}"
}
