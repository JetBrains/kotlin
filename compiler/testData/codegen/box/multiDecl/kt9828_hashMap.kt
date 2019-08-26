// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val hashMap = HashMap<String, Int>()
    hashMap.put("one", 1)
    hashMap.put("two", 2)
    for ((key, value) in hashMap) {
    }

    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: UNRESOLVED_REF__ HashMap 
