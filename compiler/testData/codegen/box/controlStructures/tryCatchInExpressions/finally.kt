// IGNORE_BACKEND: WASM
fun box(): String =
        "O" + try { "K" } finally { "hmmm" }