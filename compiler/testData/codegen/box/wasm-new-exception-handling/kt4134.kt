// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// WASM_FAILS_IN_SINGLE_MODULE_MODE
// TODO: remove the test when KT-66906 will be resolved
fun <T, R> io(s: R, a: (R) -> T): T {
    try {
        return a(s)
    } finally {
        try {
            s.toString()
        } catch(e: Exception) {
            //NOP
        }
    }
}

fun box() : String {
    return io(("OK"), {it})
}