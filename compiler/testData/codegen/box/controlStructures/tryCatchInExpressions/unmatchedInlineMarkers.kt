// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
inline fun catchAll(x: String, block: () -> Unit): String {
    try {
        block()
    } catch (e: Throwable) {
    }
    return x
}

inline fun throwIt(msg: String) {
    throw Exception(msg)
}

inline fun bar(x: String): String =
        x + catchAll("") { throwIt("oops!") }

fun box(): String =
        bar("OK")
