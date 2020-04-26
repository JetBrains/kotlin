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

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IR_TRY
