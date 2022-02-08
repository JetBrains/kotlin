// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: default argument mapping in MPP isn't designed yet
// !LANGUAGE: +MultiPlatformProjects
// FILE: common.kt

class Receiver(val value: String)

expect fun Receiver.test(result: String = value): String

// FILE: platform.kt

actual fun Receiver.test(result: String): String {
    return result
}

fun box() = Receiver("Fail").test("OK")
