// IGNORE_BACKEND: WASM
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// WASM_MUTE_REASON: EXPECT_DEFAULT_PARAMETERS
// IGNORE_BACKEND_K2: JVM_IR, NATIVE
// FIR status: default argument mapping in MPP isn't designed yet
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
