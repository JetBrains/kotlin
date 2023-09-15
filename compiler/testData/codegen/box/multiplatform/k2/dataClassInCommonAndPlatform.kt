// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-61972
// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, WASM, NATIVE
// MODULE: common
// FILE: common.kt
data class CommonData(val value: String)

// MODULE: main()()(common)
// FILE: test.kt
data class PlatformData(val commonData: CommonData)

fun box() = "OK"
