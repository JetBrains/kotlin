// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE, WASM

// MODULE: common
// FILE: common.kt

class Receiver(val value: String)

expect fun Receiver.test(result: String = value): String

// MODULE: platform()()(common)
// FILE: platform.kt

actual fun Receiver.test(result: String): String {
    return result
}

fun box() = Receiver("Fail").test("OK")
