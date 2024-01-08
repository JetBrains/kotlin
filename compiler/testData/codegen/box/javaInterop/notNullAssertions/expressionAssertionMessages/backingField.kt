// !LANGUAGE: +NoSourceCodeInNotNullAssertionExceptions
// TARGET_BACKEND: JVM
// FILE: test.kt

val publicProperty = J().s()
private val privateProperty = J().s()

fun f(x: String) = "Fail 1"

fun box(): String {
    try {
        f(publicProperty)
    } catch (e: NullPointerException) {
        if (e.message != "publicProperty must not be null") return "Fail 2: ${e.message}"
    }

    try {
        f(privateProperty)
    } catch (e: NullPointerException) {
        if (e.message != "privateProperty must not be null") return "Fail 3: ${e.message}"
    }

    return "OK"
}

// FILE: J.java
public class J {
    public final String s() { return null; }
}
