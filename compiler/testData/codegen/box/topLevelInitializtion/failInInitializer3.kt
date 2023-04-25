// TARGET_BACKEND: NATIVE
// IGNORE_BACKEND: NATIVE_WITH_LEGACY_MM
// FILE: lib.kt
import kotlin.native.concurrent.*

@ThreadLocal
val x: String = computeX()

fun computeX(): String = error("1")

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
        if (t.cause != null) return "FAIL 5"
    }
    return "OK"
}
