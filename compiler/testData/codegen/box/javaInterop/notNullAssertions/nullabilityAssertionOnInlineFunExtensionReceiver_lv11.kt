// !LANGUAGE: -NullabilityAssertionOnExtensionReceiver
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: test.kt
import kotlin.test.*

inline fun String.extension() {}

fun box(): String {
    J.s().extension() // NB no exception thrown
    return "OK"
}

// FILE: J.java
public class J {
    public static String s() { return null; }
}