fun box(): String =
        "O" + try { throw Exception("oops!") } catch (e: Exception) { "K" }
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
