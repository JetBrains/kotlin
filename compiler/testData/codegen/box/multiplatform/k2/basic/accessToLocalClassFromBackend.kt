// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE, WASM
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