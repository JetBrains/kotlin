// IGNORE_BACKEND: NATIVE
// FILE: A.kt
// LANGUAGE_VERSION: 1.2

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("bar")
package foo

fun f() = "OK"

var v: Int = 1

inline fun i(block: () -> Unit) = block()

// FILE: B.kt
// LANGUAGE_VERSION: 1.2

import foo.*

fun box(): String {
    v = 2
    if (v != 2) return "Fail"
    i { v = 3 }
    if (v != 3) return "Fail"
    return f()
}
