// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect fun foo(a: String, b: String = "O"): String

// MODULE: lib()()(common)
// FILE: lib.kt

actual fun foo(a: String, b: String) = a + b

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    return foo("") + foo("K", "")
}