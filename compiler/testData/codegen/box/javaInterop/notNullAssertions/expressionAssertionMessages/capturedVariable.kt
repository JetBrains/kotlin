// !LANGUAGE: +NoSourceCodeInNotNullAssertionExceptions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// FILE: test.kt
fun f(x: String) = "Fail 1"

fun box(): String {
    return try {
        val variable = J().s()
        val block = { f(variable) }
        block()
    } catch (e: NullPointerException) {
        if (e.message == null)
            "OK"
        else
            "Fail: ${e.message}"
    }
}

// FILE: J.java
public class J {
    public final String s() { return null; }
}
