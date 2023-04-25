// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, NATIVE

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
