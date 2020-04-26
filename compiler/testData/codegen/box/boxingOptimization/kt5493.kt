fun box() : String {
    try {
        return "OK"
    }
    finally {
        null?.toString()
    }
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY