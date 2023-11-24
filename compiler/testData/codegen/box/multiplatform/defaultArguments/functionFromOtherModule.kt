// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// FIR status: expect/actual in the same module
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