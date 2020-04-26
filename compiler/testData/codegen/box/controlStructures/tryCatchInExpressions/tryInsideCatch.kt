fun box(): String =
        "O" +
        try {
            throw Exception("oops!")
        }
        catch (e: Exception) {
            try { "K" } catch (e: Exception) { "2" }
        }
// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY
