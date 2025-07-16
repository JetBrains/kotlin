// TARGET_BACKEND: WASM
// RUN_PLAIN_BOX_FUNCTION
// IGNORE_BACKEND_K1: WASM
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, NATIVE
// FILE: main.kt
@file:OptIn(kotlin.js.ExperimentalJsExport::class)

import kotlin.js.*

external class C : JsAny          // <- excluded
fun jsVal(): JsAny = js("123")

fun box(): String {
    jsVal().unsafeCast<C>()       // must not throw
    return "OK"
}