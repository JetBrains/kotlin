fun println(s: String) {
}

fun box(): String {
    val x = println(":Hi!") as? Any
    if (x != Unit) return "Fail: $x"
    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNIT
