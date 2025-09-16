// WITH_STDLIB
// API_VERSION: 1.9
// DONT_TARGET_EXACT_BACKEND: WASM_JS, WASM_WASI
// DONT_TARGET_EXACT_BACKEND: JS_IR, JS_IR_ES6

import kotlin.concurrent.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
class BoolWrapper(@Volatile var x: Boolean)

val global = BoolWrapper(false)

fun box() : String {
    val local = BoolWrapper(false)
    if (global.x || local.x) return "FAIL"
    global.x = true
    local.x = true
    return if (global.x && local.x) "OK" else "FAIL"
}