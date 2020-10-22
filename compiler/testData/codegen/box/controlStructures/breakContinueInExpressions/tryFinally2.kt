// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
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
