// IGNORE_BACKEND: JVM_IR
// FILE: A.kt

package a

@Suppress("UNSUPPORTED_FEATURE")
inline class Foo(val x: IntArray) {
    val size: Int get() = x.size
}

// FILE: B.kt

import a.*

fun box(): String {
    val a = Foo(intArrayOf(3, 4))
    if (a.size != 2) return "Fail"
    return "OK"
}
