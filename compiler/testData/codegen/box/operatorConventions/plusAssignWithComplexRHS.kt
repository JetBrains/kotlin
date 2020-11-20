// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: STDLIB_COLLECTIONS
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

var map: Map<Any, Set<Any>> = emptyMap()

fun box(): String {
    map += "OK" to emptySet()
    return map.keys.first().toString()
}
