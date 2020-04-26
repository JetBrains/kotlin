fun foo(): String {
    val s = try {
        "OK"
    } catch (e: Exception) {
        try {
            ""
        } catch (ee: Exception) {
            ""
        }
    }

    return s
}

fun box(): String {
    return foo()
}
// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY
