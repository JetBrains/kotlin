// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// KJS_WITH_FULL_RUNTIME
operator fun <K, V> MutableMap<K, V>.set(k : K, v : V) = put(k, v)

fun box() : String {
    val map = HashMap<String,String>()
    map["239"] = "932"
    return if(map["239"] == "932") "OK" else "fail"
}
