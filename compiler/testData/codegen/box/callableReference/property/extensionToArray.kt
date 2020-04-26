val Array<String>.firstElement: String get() = get(0)

fun box(): String {
    val p = Array<String>::firstElement
    return p.get(arrayOf("OK", "Fail"))
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
