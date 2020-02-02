// IGNORE_BACKEND: WASM
fun box(): String =
        "O" + try { throw Exception("oops!") } catch (e: Exception) { "K" }