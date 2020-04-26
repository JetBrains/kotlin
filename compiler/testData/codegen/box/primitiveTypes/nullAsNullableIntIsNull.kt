fun box(): String {
    try {
        if ((null as Int?)!! == 10) return "Fail #1"
        return "Fail #2"
    }
    catch (e: Exception) {
        return "OK"
    }
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY
