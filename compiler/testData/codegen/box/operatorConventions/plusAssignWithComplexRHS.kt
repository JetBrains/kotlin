// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR

var map: Map<Any, Set<Any>> = emptyMap()

fun box(): String {
    map += "OK" to emptySet()
    return map.keys.first().toString()
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: Wasm stdlib: Maps