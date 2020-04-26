fun box(): String {
    OUTER@while (true) {
        var x = ""
        try {
            do {
                x = x + break@OUTER
            } while (true)
        } finally {
            return "OK"
        }
    }
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
