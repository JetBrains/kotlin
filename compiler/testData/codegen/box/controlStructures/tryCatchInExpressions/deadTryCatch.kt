// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
inline fun catchAll(x: String, block: () -> Unit): String {
    try {
        block()
    } catch (e: Throwable) {
    }
    return x
}

inline fun tryTwice(block: () -> Unit) {
    try {
        block()
        try {
            block()
        } catch (e: Exception) {
        }
    } catch (e: Exception) {
    }
}

fun box(): String {
    return catchAll("OK") {
        tryTwice {
            throw Exception()
        }
    }
}