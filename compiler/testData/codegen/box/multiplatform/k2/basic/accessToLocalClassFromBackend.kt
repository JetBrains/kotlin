// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, WASM
// IGNORE_NATIVE_K1: mode=ONE_STAGE_MULTI_MODULE
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

val LocalClass = object {
    override fun toString() = "OK"
}

fun ok() = LocalClass.toString()

// MODULE: platform()()(common)
// FILE: platform.kt

fun box() = ok()