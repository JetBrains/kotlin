// Multi-platform not supported with FIR yet.
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: WASM

// (supported: JVM_IR, JS_IR(_E6))

// WITH_STDLIB
// !LANGUAGE: +InstantiationOfAnnotationClasses +MultiPlatformProjects

// MODULE: lib
// FILE: common.kt

expect annotation class A(val value: String)

fun createCommon(): A = A("OK")

// FILE: platform.kt

actual annotation class A(actual val value: String)

fun createPlatform(): A = A("OK")

// MODULE: main(lib)
// FILE: main.kt

fun createApp(): A = A("OK")

fun box(): String {
    if (createApp().value != "OK") return "FAIL app"
    if (createCommon().value != "OK") return "FAIL common"
    if (createPlatform().value != "OK") return "FAIL platform"
    return "OK"
}
