// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXPECT_DEFAULT_PARAMETERS
// !LANGUAGE: +MultiPlatformProjects

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
