// KJS_WITH_FULL_RUNTIME

fun <K: Any, V: Any> foo(k: K, v: V) {
    val map = HashMap<K, V>()
    val old = map.put(k, v)
}

fun box(): String {
    foo("", "")
    return "OK"
}


// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: STDLIB_HASH_MAP
