// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: commonMain.kt

expect class S

expect fun foo(s: S): S

expect fun foo(i: Int): Int

// Uncomment once KT-62926 is fixed
//expect val Int.k: Int

expect val String.k: String

// Uncomment once KT-62926 is fixed
//expect var Int.l: Int

expect var String.l: String

fun test(s: S) = foo(s)

fun k() = "K".k + "".l

// MODULE: platform()()(common)
// FILE: platform.kt

actual fun foo(i: Int) = i

actual fun foo(s: String) = s

// Uncomment once KT-62926 is fixed
//actual val Int.k: Int get() = 42

actual val String.k: String get() = this

// Uncomment once KT-62926 is fixed
//actual var Int.l: Int
//    get() = 48
//    set(value) {}

actual var String.l: String
    get() = this
    set(value) {}

actual typealias S = String

fun box() = test("O") + k()