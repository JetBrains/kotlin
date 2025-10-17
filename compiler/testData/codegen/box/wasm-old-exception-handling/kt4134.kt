// TARGET_BACKEND: WASM
// USE_OLD_EXCEPTION_HANDLING_PROPOSAL

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