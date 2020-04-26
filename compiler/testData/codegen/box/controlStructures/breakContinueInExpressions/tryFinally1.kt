fun box(): String {
    var x = "OK"
    while (true) {
        try {
            x = x + continue
        }
        finally {
            x = x + break
        }
    }
    return x
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
