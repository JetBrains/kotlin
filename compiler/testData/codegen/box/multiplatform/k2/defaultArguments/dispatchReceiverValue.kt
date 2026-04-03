// IGNORE_BACKEND_K1: ANY
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: JS:1.9
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-41901, KT-57181

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
