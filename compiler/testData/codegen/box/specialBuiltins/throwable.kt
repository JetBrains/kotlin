fun box(): String {
    try {
        throw Throwable("OK", null)
    } catch (t: Throwable) {
        if (t.cause != null) return "fail 1"
        return t.message!!
    }

    return "fail 2"
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
