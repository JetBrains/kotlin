// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
enum class E {
    I
}

fun box(): String {
    val i = (E::name).get(E.I)
    if (i != "I") return "Fail $i"
    val n = (E::ordinal).get(E.I)
    if (n != 0) return "Fail $n"
    return "OK"
}
