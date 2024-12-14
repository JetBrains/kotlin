// WITH_STDLIB
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// API_VERSION: 1.9
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR
// ^^^ Source code is not compiled in JS.

import kotlin.concurrent.*

@OptIn(kotlin.ExperimentalStdlibApi::class)
class GenericWrapper<T>(@Volatile var x: T)

val global = GenericWrapper("FA")
val globalLong = GenericWrapper(1L)

fun box() : String {
    val local = GenericWrapper("IL")
    val localLong = GenericWrapper(2L)
    if (global.x + local.x != "FAIL") return "FAIL 1"
    if (globalLong.x + localLong.x != 3L) return "FAIL 2"
    global.x = "O"
    local.x = "K"
    globalLong.x = 5L
    localLong.x = 6L
    if (globalLong.x + localLong.x != 11L) return "FAIL 3"
    return global.x + local.x
}