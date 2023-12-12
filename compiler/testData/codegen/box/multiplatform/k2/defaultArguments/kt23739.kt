// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect inline fun <T> get(p: String = "OK"): String

// MODULE: platform()()(common)
// FILE: platform.kt

actual inline fun <T> get(p: String): String {
    return p
}

fun box(): String {
    return get<String>()
}
