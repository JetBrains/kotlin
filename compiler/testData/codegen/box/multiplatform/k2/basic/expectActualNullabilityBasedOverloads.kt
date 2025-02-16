// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JVM_IR

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
