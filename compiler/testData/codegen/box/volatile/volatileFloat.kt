// WITH_STDLIB
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, WASM
// !API_VERSION: 1.9

import kotlin.concurrent.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
class FloatWrapper(@Volatile var x: Float)

val global = FloatWrapper(1.5f)

fun box() : String {
    val local = FloatWrapper(2.5f)
    if (global.x + local.x != 4.0f) return "FAIL"
    global.x = 5.5f
    local.x = 6.5f
    return if (global.x + local.x != 12.0f) return "FAIL" else "OK"
}