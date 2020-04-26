fun box(): String {
    Unit

    val a = Unit
    val b = Unit
    if (a != b) return "Fail a != b"

    if (Unit != Unit) return "Fail Unit != Unit"

    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: UNIT