// LANGUAGE: +MultiPlatformProjects

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
