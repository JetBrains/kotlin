// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

fun foo(b: Byte, s: String, i: Int, d: Double, li: Long): String = "$b $s $i $d $li"

fun box(): String {
    val test = foo(1, "abc", 1, 1.0, try { 1L } catch (e: Exception) { 10L })
    if (test != "1 abc 1 1.0 1") return "Failed, test==$test"

    return "OK"
}