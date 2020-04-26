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

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
