// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: EXCEPTIONS_NOT_IMPLEMENTED
public inline fun fails(block: () -> Unit): Throwable? {
    var thrown: Throwable? = null
    try {
        block()
    } catch (e: Throwable) {
        thrown = e
    }
    if (thrown == null)
        throw Exception("Expected an exception to be thrown")
    return thrown
}

public inline fun throwIt(msg: String) {
    throw Exception(msg)
}

fun box(): String {
    fails {
        throwIt("oops!")
    }

    var x = 0
    try {
        fails {
            x = 1
        }
    }
    catch (e: Exception) {
        x = 2
    }

    if (x != 2) return "Failed: x==$x"

    return "OK"
}