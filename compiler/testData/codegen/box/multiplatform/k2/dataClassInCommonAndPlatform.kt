// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-61972
// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, WASM
// IGNORE_NATIVE_K1: mode=ONE_STAGE_MULTI_MODULE
// MODULE: common
// FILE: common.kt
data class CommonData(val value: String)

// MODULE: main()()(common)
// FILE: test.kt
data class PlatformData(val commonData: CommonData)

fun box() = "OK"
