fun box(): String {
    while (true) {
        try {
            continue;
        }
        finally {
            break;
        }
    }
    return "OK"
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
