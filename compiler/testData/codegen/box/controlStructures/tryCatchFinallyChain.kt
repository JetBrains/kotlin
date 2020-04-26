fun box() : String {
    try {
    } finally {
        try {
            try {
            } finally {
                try {
                } finally {
                }
            }
        } catch (e: Exception) {
            try {
            } catch (f: Exception) {
            } finally {
            }
        }
        return "OK"
    }
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
