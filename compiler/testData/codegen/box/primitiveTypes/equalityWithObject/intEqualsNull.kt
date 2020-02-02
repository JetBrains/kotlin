// IGNORE_BACKEND: WASM
fun box(): String =
    if (1.equals(null)) "FAIL" else "OK"