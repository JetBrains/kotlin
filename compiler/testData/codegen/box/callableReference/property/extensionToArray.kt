// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: PROPERTY_REFERENCES
val Array<String>.firstElement: String get() = get(0)

fun box(): String {
    val p = Array<String>::firstElement
    return p.get(arrayOf("OK", "Fail"))
}
