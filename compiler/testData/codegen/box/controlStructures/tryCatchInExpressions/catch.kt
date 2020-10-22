// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun box(): String =
        "O" + try { throw Exception("oops!") } catch (e: Exception) { "K" }