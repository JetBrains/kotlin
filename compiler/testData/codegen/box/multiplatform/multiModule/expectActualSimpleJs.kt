// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6

// TARGET_BACKEND: JS_IR
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

// MODULE: js()()(common)
// TARGET_PLATFORM: JS
// FILE: main.kt

actual fun func(): String = "O"

actual var prop: String = "!"

fun box() = test()