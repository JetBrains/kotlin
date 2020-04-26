fun box(): String =
        "O" + try { "K" } catch (e: Exception) { "oops!" }
// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY
