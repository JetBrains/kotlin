fun exit(): Nothing = null!!

fun box(): String {
    val a: String
    try {
        a = "OK"
    }
    catch (e: Exception) {
        exit()
    }
    return a
}
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
