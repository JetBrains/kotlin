fun <T> castToString(t: T) {
    t as String
}


fun box(): String {
    try {
        castToString<Any?>(null)
    } catch (e: Exception) {
        return "OK"
    }
    return "Fail"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
