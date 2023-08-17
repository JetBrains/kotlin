// !LANGUAGE: -EnumEntries
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM
// WITH_STDLIB

import kotlin.enums.*

enum class E { A, B, C }

@OptIn(kotlin.ExperimentalStdlibApi::class)
fun box() : String {
    if (enumEntries<E>() != listOf(E.A, E.B, E.C)) return "FAIL"
    return "OK"
}
