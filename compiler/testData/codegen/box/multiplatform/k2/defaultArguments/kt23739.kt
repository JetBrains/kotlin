// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K1: JVM, JVM_IR, JS, JS_IR, JS_IR_ES6, WASM
// IGNORE_NATIVE_K1: mode=ONE_STAGE_MULTI_MODULE

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
