// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
fun println(s: String) {
}

fun box(): String {
    val x = println(":Hi!") as Any
    if (x != Unit) return "Fail: $x"
    return "OK"
}
