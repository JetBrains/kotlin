// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND_FIR: JVM_IR
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
