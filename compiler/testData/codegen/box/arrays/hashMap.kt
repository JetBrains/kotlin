// KJS_WITH_FULL_RUNTIME
operator fun HashMap<String, Int?>.set(index: String, elem: Int?) {
    this.put(index, elem)
}

fun box(): String {
    val s = HashMap<String, Int?>()
    s["239"] = 239
    return if (s["239"] == 239) "OK" else "Fail"
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_HASH_MAP
