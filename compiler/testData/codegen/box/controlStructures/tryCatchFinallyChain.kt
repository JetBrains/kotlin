// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
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
