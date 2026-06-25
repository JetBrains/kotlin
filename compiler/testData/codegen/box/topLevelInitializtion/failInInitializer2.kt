// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// FILE: lib.kt
val x: String = computeX()

fun computeX(): String = throw IllegalStateException("1")

val y: String = computeY()

fun computeY(): String = "2"

// FILE: main.kt
fun box() : String {
    try {
        x
        return "FAIL 1"
    } catch(t: Error) {
        val cause = t.cause
        if (cause !is IllegalStateException) return "FAIL 2: cause must be IllegalStateException, was ${cause?.let { it::class }}"
        if (cause.message != "1") return "FAIL 3: message must be '1', was '${cause.message}'"
    }
    try {
        y
        return "FAIL 4"
    } catch(t: Error) {
        if (t.cause != null) return "FAIL 5: cause must be null, got ${t.cause}"
        if (t.message == null) return "FAIL 6: message must not be null"
    }
    return "OK"
}
