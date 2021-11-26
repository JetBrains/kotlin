// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: default argument mapping in MPP isn't designed yet
// WITH_STDLIB
// MODULE: lib
// FILE: common.kt

expect fun foo(a: String, b: String = "O"): String

// FILE: platform.kt

actual fun foo(a: String, b: String) = a + b

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    return foo("") + foo("K", "")
}