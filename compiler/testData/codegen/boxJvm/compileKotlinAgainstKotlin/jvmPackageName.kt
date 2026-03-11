// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("bar")
package foo

fun f() = "OK"

var v: Int = 1

inline fun i(block: () -> Unit) = block()

// MODULE: main(lib)
// FILE: B.kt

import foo.*

fun box(): String {
    v = 2
    if (v != 2) return "Fail"
    i { v = 3 }
    if (v != 3) return "Fail"
    return f()
}
