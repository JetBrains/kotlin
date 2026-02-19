// WITH_STDLIB
// API_VERSION: 1.9
// DONT_TARGET_EXACT_BACKEND: WASM_JS, WASM_WASI
// DONT_TARGET_EXACT_BACKEND: JS_IR, JS_IR_ES6

import kotlin.concurrent.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
class IntWrapper(@Volatile var x: Int)

val global = IntWrapper(1)

fun box() : String {
    val local = IntWrapper(2)
    if (global.x + local.x != 3) return "FAIL"
    global.x = 5
    local.x = 6
    return if (global.x + local.x != 11) return "FAIL" else "OK"
}