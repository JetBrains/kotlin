// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_FIR: JVM_IR

// FILE: common.kt

class B(val value: Int)

expect fun test(a: Int = 2, b: Int = B(a * 2).value, c: String = "${b}$a"): String

// FILE: platform.kt

actual fun test(a: Int, b: Int, c: String): String = c

fun box(): String {
    val result = test()
    return if (result == "42") "OK" else "Fail: $result"
}
