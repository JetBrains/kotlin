// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
fun box(): String =
        "" +
        try { "O" } catch (e: Exception) { "1" } +
        try { throw Exception("oops!") } catch (e: Exception) { "K" }