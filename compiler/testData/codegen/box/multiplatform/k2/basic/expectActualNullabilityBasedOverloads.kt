// IGNORE_BACKEND: JVM, JVM_IR
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect fun o(x: String?): String?
expect fun k(x: String?): String?

fun box() = o(null) + k(null)

// MODULE: platform()()(common)
// FILE: platform.kt

fun o(x: String) = ""
actual fun o(x: String?): String? = "O"

actual fun k(x: String?): String? = "K"
fun k(x: String) = ""
