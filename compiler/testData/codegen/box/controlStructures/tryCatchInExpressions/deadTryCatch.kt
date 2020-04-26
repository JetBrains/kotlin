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
// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IR_TRY
