// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, NATIVE
// KT-57181
// IGNORE_BACKEND_K2: NATIVE
// WASM_MUTE_REASON: EXPECT_DEFAULT_PARAMETERS
// !LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-41901

// MODULE: common
// FILE: common.kt

expect class C {
    val value: String

    fun test(result: String = value): String
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual class C(actual val value: String) {
    actual fun test(result: String): String = result
}

fun box() = C("Fail").test("OK")
