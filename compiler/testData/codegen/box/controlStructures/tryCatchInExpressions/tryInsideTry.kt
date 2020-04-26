class MyException(message: String): Exception(message)

fun box(): String =
        "O" +
        try {
            try { throw Exception("oops!") } catch (mye: MyException) { "1" }
        }
        catch (e: Exception) {
            "K"
        }
// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY
