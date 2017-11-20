// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: test.kt
// LANGUAGE_VERSION: 1.2
import kotlin.test.*

fun String.extension() {}

fun box(): String {
    assertFailsWith<IllegalStateException> { J.s().extension() }
    return "OK"
}

// FILE: J.java
public class J {
    public static String s() { return null; }
}