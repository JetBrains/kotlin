// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// FULL_JDK

enum class Color(val s: String) {
    BLACK("black"),
    HATSUNE_MIKU(run { throw IllegalStateException("miku is not a color") });
}

enum class ThrowsError(val s: String) {
    NONTHROWING("throwing"),
    THROWING(run { throw Error("huh") });
}

fun box(): String {
    @Suppress("INVISIBLE_REFERENCE")
    try {
        Color.BLACK
        return "FAIL 1.1: should throw"
    } catch (e: ExceptionInInitializerError) {
        val cause = e.cause
        if (cause !is IllegalStateException) return "FAIL 1.2: cause must be IllegalStateException, was ${cause?.let { it::class }}"
        if (cause.message != "miku is not a color") return "FAIL 1.3: message must be 'miku is not a color', was '${cause.message}'"
        if (e.message != null) return "FAIL 1.4: message must be null, got ${e.message}"
    }

    try {
        Color.BLACK
        return "FAIL 2.1: should throw"
    } catch (e: Error) {
        if (e.cause != null) return "FAIL 2.2: cause must be null, got ${e.cause}"
        val expectedMessage = if (BACKEND_UNDER_TEST == "NATIVE") "There was an error during file or class initialization" else "Could not initialize class Color"
        if (e.message != expectedMessage) return "FAIL 2.3: message must be '$expectedMessage', was '${e.message}'"
    }

    try {
        ThrowsError.NONTHROWING
        return "FAIL 3.1: should throw"
    } catch (e: Error) {
        if (e.cause != null) return "FAIL 3.2: cause must be null, got ${e.cause}"
        if (e.message != "huh") return "FAIL 3.3: message must be 'huh', was '${e.message}'"
    }

    try {
        ThrowsError.NONTHROWING
        return "FAIL 4.1: should throw"
    } catch (e: Error) {
        if (e.cause != null) return "FAIL 4.2: cause must be null, got ${e.cause}"
        val expectedMessage = if (BACKEND_UNDER_TEST == "NATIVE") "There was an error during file or class initialization" else "Could not initialize class ThrowsError"
        if (e.message !=expectedMessage) return "FAIL 4.3: message must be '$expectedMessage', was '${e.message}'"
    }

    return "OK"
}
