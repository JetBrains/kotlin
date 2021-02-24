// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
val c = Unit
val d = c

fun box(): String {
    c
    d
    return "OK"
}
