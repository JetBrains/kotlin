// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect fun <T> Array<T>.getChecked(index: Int): T

expect fun BooleanArray.getChecked(index: Int): Boolean

fun ok() = if (!BooleanArray(1).getChecked(0)) "OK" else "FAIL"

// MODULE: platform()()(common)
// FILE: platform.kt

actual fun <T> Array<T>.getChecked(index: Int) = get(index)

actual fun BooleanArray.getChecked(index: Int) = get(index)

fun box() = ok()