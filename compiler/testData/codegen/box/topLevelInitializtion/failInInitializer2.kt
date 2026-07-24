// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// FULL_JDK

// FILE: lib.kt
package lib

val x: String = computeX()

fun computeX(): String = throw IllegalStateException("1")

val y: String = computeY()

fun computeY(): String = "2"

// FILE: main.kt
import lib.*

fun box() : String {
    @Suppress("INVISIBLE_REFERENCE")
    try {
        x
        return "FAIL 1.1"
    } catch(t: ExceptionInInitializerError) {
        val cause = t.cause
        if (cause !is IllegalStateException) return "FAIL 1.2: cause must be IllegalStateException, was ${cause?.let { it::class }}"
        if (cause.message != "1") return "FAIL 1.3: message must be '1', was '${cause.message}'"
        if (t.message != null) return "FAIL 1.4: message must be null, got ${t.message}"
    }
    try {
        y
        return "FAIL 2.1"
    } catch(t: Error /* NoClassDefFoundError */) {
        if (BACKEND_UNDER_TEST != "ANDROID") {
            val expectedMessage = when (BACKEND_UNDER_TEST) {
                "JVM_IR" -> "Could not initialize class lib.LibKt"
                "NATIVE" -> "There was an error during file or class initialization"
                else -> "Could not initialize file"
            }
            if (t.message != expectedMessage) return "FAIL 2.2: message must be '$expectedMessage', was '${t.message}'"
        }
    }
    return "OK"
}
