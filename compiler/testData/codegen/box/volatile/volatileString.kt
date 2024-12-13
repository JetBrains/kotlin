// WITH_STDLIB
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// API_VERSION: 1.9
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

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