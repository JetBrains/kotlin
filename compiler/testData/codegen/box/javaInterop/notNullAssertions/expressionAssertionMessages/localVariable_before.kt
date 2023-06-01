// !LANGUAGE: -NoSourceCodeInNotNullAssertionExceptions
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR
// FIR status: don't support legacy feature (KT-57570)
// FILE: test.kt
fun f(x: String) = "Fail 1"

fun box(): String {
    return try {
        val variable = J().s()
        f(variable)
    } catch (e: NullPointerException) {
        if (e.message == "variable must not be null")
            "OK"
        else
            "Fail: ${e.message}"
    }
}

// FILE: J.java
public class J {
    public final String s() { return null; }
}
