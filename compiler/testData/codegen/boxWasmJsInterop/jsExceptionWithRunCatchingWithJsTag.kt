// USE_JS_TAG
// TARGET_BACKEND: WASM

fun throwSomeJsException(): Int = js("{ throw new TypeError('Test'); }")
fun throwSomeJsPrimitive(): Int = js("{ throw 'Test'; }")

// Finally only
inline fun jsPrimitiveWithRunCatching(): Result<Int> = runCatching {
    throwSomeJsPrimitive()
}

inline fun jsExceptionWithRunCatching(): Result<Int> = runCatching {
    throwSomeJsException()
}

fun box(): String {
    var fatalException: Throwable? = null
    try {
        throwSomeJsPrimitive()
    } catch (e: Throwable) {
       fatalException = e
    } finally {
        if (!jsPrimitiveWithRunCatching().isFailure) return "Problem with primitive run catching"
        if (!jsExceptionWithRunCatching().isFailure) return "Problem with JS Error run catching"
    }
    return "OK"
}
