// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect fun func(): String

expect var prop: String

fun test(): String {
    prop = "K"
    return func() + prop
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual fun func(): String = "O"

actual var prop: String = "!"

fun box() = test()