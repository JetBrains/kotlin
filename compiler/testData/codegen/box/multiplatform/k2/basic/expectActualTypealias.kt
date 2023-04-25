// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect class S {
    val length: Int
}

expect fun foo(): S

fun test(): S = foo()

// MODULE: platform()()(common)
// FILE: platform.kt

actual typealias S = String

actual fun foo(): S = "OK"

fun box() = test()