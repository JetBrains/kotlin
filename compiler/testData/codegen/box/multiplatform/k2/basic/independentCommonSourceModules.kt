// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common1
// TARGET_PLATFORM: Common
// FILE: common1.kt

fun o() = "O"

// MODULE: common2
// TARGET_PLATFORM: Common
// FILE: common2.kt

fun k() = "K"

// MODULE: platform()()(common1, common2)
// FILE: platform.kt

fun box() = o() + k()