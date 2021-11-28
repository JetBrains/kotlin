// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@file:JvmPackageName("bar")

fun f() = "OK"

var v: Int = 1

// MODULE: main(lib)
// FILE: B.kt

fun box(): String {
    v = 2
    if (v != 2) return "Fail"
    return f()
}
