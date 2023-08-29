// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// !LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58896

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect fun f(param: Int): String

fun f(param: Any) = "$param: Any"

fun commonFun() = "${f(1)}; ${f("s")}"

// MODULE: platform()()(common)
// FILE: platform.kt

actual fun f(param: Int) = "$param: Int"

fun platformFun() = "${f(1)}; ${f("s")}"

fun box(): String {
    if (commonFun() != "1: Int; s: Any") return "FAIL 1"
    if (platformFun() != "1: Int; s: Any") return "FAIL 2"
    return "OK"
}
