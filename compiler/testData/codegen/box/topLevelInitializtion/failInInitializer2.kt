// DISABLE_IR_VISIBILITY_CHECKS: ANY
// FULL_JDK

// FILE: lib.kt
val x: String = computeX()

fun computeX(): String = throw IllegalStateException("1")

val y: String = computeY()

fun computeY(): String = "2"

// FILE: main.kt
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

    @Suppress("INVISIBLE_REFERENCE")
    try {
        y
        return "FAIL 2.1"
    } catch(t: NoClassDefFoundError) {
        if (t.cause != null) return "FAIL 2.2: cause must be null, got ${t.cause}"
        val expectedMessage = when (BACKEND_UNDER_TEST) {
            "JVM_IR" -> "Could not initialize class LibKt"
            else -> "Could not initialize file"
        }
        if (t.message != expectedMessage) return "FAIL 2.3: message must be '$expectedMessage', was '${t.message}'"
    }
    return "OK"
}
