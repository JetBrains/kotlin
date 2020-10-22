// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: UNIT_ISSUES
fun box(): String {
    Unit

    val a = Unit
    val b = Unit
    if (a != b) return "Fail a != b"

    if (Unit != Unit) return "Fail Unit != Unit"

    return "OK"
}
