// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_OLD_AGAINST_IR
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt

package test

class M(size: Int) {
    val m = IntArray(size) { 0 }
}

inline operator fun M.get(a: Any, b: Any, ifn: () -> Int) =
    m[ifn()]

inline operator fun <reified T> M.set(a: T, b: Any, ifn: () -> Int, v: Int) {
    if (b !is T) throw AssertionError()
    m[ifn()] = v
}

// FILE: 2.kt

import test.*

fun box(): String {
    val m = M(4)
    m["a", "b", { 1 }] += 10
    return if (m.m[1] == 10) "OK" else "Fail"
}
