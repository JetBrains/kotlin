// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// FULL_JDK

// FILE: lib.kt
package lib

val x: String = computeX()

fun computeX(): String = throw IllegalStateException("1")

// FILE: lib2.kt
package lib2

val y: String = computeY()

class MyError(message: String) : Error(message)

fun computeY(): String = throw MyError("2")


// FILE: main.kt
import lib.*
import lib2.*

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
    } catch(t: MyError) {
        if (t.cause != null) return "FAIL 2.2: cause must be null, got ${t.cause}"
        if (t.message != "2") return "FAIL 2.3: message must be '2', was '${t.message}'"
    }
    return "OK"
}
