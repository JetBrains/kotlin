// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE, WASM
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// FIR status: expect/actual in the same module (ACTUAL_WITHOUT_EXPECT)

// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ IR serialization/deserialization is not tested with K1.

// WITH_STDLIB
// LANGUAGE: +InstantiationOfAnnotationClasses +MultiPlatformProjects

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
