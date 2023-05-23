// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect class A<B, C> {
    fun o(b: B): C
}

expect val <D> D.k: D

fun k(): String {
    return "K".k
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual class A<C, B> {
    actual fun o(b: C): B = "O" as B
}

actual val <D> D.k: D get() = this as D

fun box() = A<Int, String>().o(42) + k()