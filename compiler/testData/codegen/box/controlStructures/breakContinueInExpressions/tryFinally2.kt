fun box(): String {
    var r = ""
    for (i in 1..1)  {
        try {
            r += "O"
            break
        } finally {
            r += "K"
            continue
        }
    }
    return r
}



// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
