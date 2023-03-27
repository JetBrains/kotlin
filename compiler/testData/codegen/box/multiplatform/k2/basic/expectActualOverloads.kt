// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: commonMain.kt

expect class S

expect fun foo(s: S): S

expect fun foo(i: Int): Int

fun test(s: S) = foo(s)

// MODULE: platform()()(common)
// FILE: platform.kt

actual fun foo(i: Int) = i

actual fun foo(s: String) = s

actual typealias S = String

fun box() = test("OK")