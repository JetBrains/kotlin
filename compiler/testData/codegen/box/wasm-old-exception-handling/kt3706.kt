// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL
// WASM_FAILS_IN: Wasmtime

fun f(): Int {
    try {
        return 0
    }
    finally {
        try { // culprit ?? remove this try-catch and it works.
        } catch (ignore: Exception) {
        }
    }
}

fun box(): String {
    if (f() != 0)  return "fail1"

    return "OK"
}