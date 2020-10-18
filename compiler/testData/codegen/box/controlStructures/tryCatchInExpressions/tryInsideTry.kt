// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
class MyException(message: String): Exception(message)

fun box(): String =
        "O" +
        try {
            try { throw Exception("oops!") } catch (mye: MyException) { "1" }
        }
        catch (e: Exception) {
            "K"
        }