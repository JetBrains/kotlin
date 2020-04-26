fun box(): String {
    val f = "kotlin"::length
    val result = f.get()
    return if (result == 6) "OK" else "Fail: $result"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: PROPERTY_REFERENCES
