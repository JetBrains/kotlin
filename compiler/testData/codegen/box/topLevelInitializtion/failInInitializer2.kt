// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, WASM, NATIVE_WITH_LEGACY_MM
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
        if (cause !is IllegalStateException) return "FAIL 2"
        if (cause.message != "1") return "FAIL 3"
    }
    try {
        y
        return "FAIL 4"
    } catch(t: Error) {
        // On JVM < 20, t.cause is null, but on JVM >= 20, it's ExceptionInInitializerError.
    }
    return "OK"
}
