// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// KJS_WITH_FULL_RUNTIME
operator fun HashMap<String, Int?>.set(index: String, elem: Int?) {
    this.put(index, elem)
}

fun box(): String {
    val s = HashMap<String, Int?>()
    s["239"] = 239
    return if (s["239"] == 239) "OK" else "Fail"
}
