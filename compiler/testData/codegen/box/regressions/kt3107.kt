// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
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