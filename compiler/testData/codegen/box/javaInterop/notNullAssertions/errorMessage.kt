// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FILE: test.kt
fun f(x: String) = "Fail 1"

fun box(): String {
    return try {
        f(J().s())
    } catch (e: NullPointerException) {
        if (e.message == "J().s() must not be null")
            "OK"
        else
            "Fail: ${e.message}"
    }
}

// FILE: J.java
public class J {
    public final String s() { return null; }
}
