// TARGET_BACKEND: JVM
// MODULE: lib
// WITH_STDLIB

// FILE: A.kt

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("bar")
@file:JvmName("Baz")
package foo

fun f() = "OK"

var v: Int = 1

// MODULE: main(lib)
// FILE: B.kt

import foo.*

fun box(): String {
    v = 2
    if (v != 2) return "Fail"
    return f()
}
