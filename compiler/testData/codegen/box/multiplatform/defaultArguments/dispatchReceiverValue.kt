// IGNORE_BACKEND: WASM, JS_IR, JS_IR_ES6
// WASM_MUTE_REASON: EXPECT_DEFAULT_PARAMETERS
// IGNORE_BACKEND_K2: JVM, JVM_IR, NATIVE
// FIR status: outdated code (expect and actual in the same module)
// !LANGUAGE: +MultiPlatformProjects

// KT-41901

// FILE: common.kt

expect class C {
    val value: String

    fun test(result: String = value): String
}

// FILE: platform.kt

actual class C(actual val value: String) {
    actual fun test(result: String): String = result
}

fun box() = C("Fail").test("OK")
