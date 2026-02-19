// WITH_STDLIB
// API_VERSION: 1.9
// DONT_TARGET_EXACT_BACKEND: WASM_JS, WASM_WASI
// DONT_TARGET_EXACT_BACKEND: JS_IR, JS_IR_ES6

import kotlin.concurrent.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
class StringWrapper(@Volatile var x: String)

val global = StringWrapper("FA")

fun box() : String {
    val local = StringWrapper("IL")
    if (global.x + local.x != "FAIL") return "FAIL"
    global.x = "O"
    local.x = "K"
    return global.x + local.x
}